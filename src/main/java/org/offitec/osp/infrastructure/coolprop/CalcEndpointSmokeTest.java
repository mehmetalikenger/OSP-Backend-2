package org.offitec.osp.infrastructure.coolprop;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.offitec.osp.application.service.PublicUnitAppService;
import org.offitec.osp.domain.entity.CompressorRating;
import org.offitec.osp.domain.entity.TechSpecs;
import org.offitec.osp.domain.entity.Unit;
import org.offitec.osp.domain.entity.UnitDetails;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.infrastructure.repository.CompressorRatingRepository;
import org.offitec.osp.presentation.dto.CalculationRequestDTO;
import org.offitec.osp.presentation.dto.CalculationResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * End-to-end check of POST /units/calculate's service path. Runs only when
 * {@code calc.endpoint.smoketest=true}. Seeds a throwaway air-cooled chiller linked to the
 * S10-52Y/R404A rating, calls the real {@link PublicUnitAppService#calculate}, logs the result,
 * then rolls everything back (setRollbackOnly) so the database is untouched.
 */
@Component
@Order(120)
public class CalcEndpointSmokeTest implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CalcEndpointSmokeTest.class);

    private final CompressorRatingRepository ratingRepo;
    private final PublicUnitAppService publicUnitAppService;
    private final org.offitec.osp.application.report.ReportDataAssembler reportAssembler;
    private final org.offitec.osp.application.report.PdfReportService pdfReportService;
    private final TransactionTemplate txTemplate;
    private final boolean enabled;

    @PersistenceContext
    private EntityManager em;

    public CalcEndpointSmokeTest(CompressorRatingRepository ratingRepo,
                                 PublicUnitAppService publicUnitAppService,
                                 org.offitec.osp.application.report.ReportDataAssembler reportAssembler,
                                 org.offitec.osp.application.report.PdfReportService pdfReportService,
                                 PlatformTransactionManager txManager,
                                 @org.springframework.beans.factory.annotation.Value("${calc.endpoint.smoketest:false}") boolean enabled) {
        this.ratingRepo = ratingRepo;
        this.publicUnitAppService = publicUnitAppService;
        this.reportAssembler = reportAssembler;
        this.pdfReportService = pdfReportService;
        this.txTemplate = new TransactionTemplate(txManager);
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;

        txTemplate.executeWithoutResult(status -> {
            List<CompressorRating> ratings = ratingRepo.findByModelAndRefrigerant("S10-52Y", "R404A");
            if (ratings.isEmpty()) { log.warn("calc endpoint smoke test: rating not found"); return; }
            CompressorRating rating = ratings.get(0);

            Unit unit = new Unit();
            unit.setModel("SMOKE-TEST-CHILLER");
            unit.setCategory(UnitCategory.CHILLER);
            unit.setUnitType(UnitTypeEnum.AW);
            unit.setCompressorQty(1);
            unit.setDeleted(false);
            em.persist(unit);

            TechSpecs ts = new TechSpecs();
            ts.setCompressorRating(rating);
            em.persist(ts);

            UnitDetails details = new UnitDetails();
            details.setUnit(unit);
            details.setMod(Mod.COOLING);
            details.setTechSpecs(ts);
            em.persist(details);
            em.flush();

            CalculationRequestDTO dto = new CalculationRequestDTO();
            dto.setUnitId(unit.getId());
            dto.setMod("COOLING");
            dto.setAmbient(35);     // air on the condenser → Tc = 35 + 15 = 50 °C
            dto.setEvapOut(7);      // chilled-water out → Te = 7 − 5 = 2 °C
            dto.setEvapIn(12);
            dto.setSuperheat(10.0);
            dto.setSubcooling(5.0);   // the calc form's default — the report must use the SAME
            dto.setFrequencyHz(50.0);

            CalculationResultDTO r = publicUnitAppService.calculate(dto);

            log.info("=== calc endpoint smoke test (AW chiller, amb=35, evapOut=7) ===");
            log.info("faithfulEngine = {}", r.isFaithfulEngine());
            log.info("Capacity   = {} kW", round(r.getRefrigeratingCapacity()));
            log.info("PowerInput = {} kW", round(r.getPowerInput()));
            log.info("EER        = {}", round(r.getCopEer()));
            log.info("MassFlow   = {} kg/h", round(r.getMassFlow()));
            log.info("CondDuty   = {} kW", round(r.getCondenserDuty()));
            log.info("Tdischarge = {} °C", round(r.getDischargeTemp()));
            log.info("inEnvelope = {}", r.isWithinEnvelope());

            // Repeat at 60 Hz to confirm the frequency input flows through the endpoint.
            dto.setFrequencyHz(60.0);
            CalculationResultDTO r60 = publicUnitAppService.calculate(dto);
            log.info("60Hz Capacity = {} kW  Power = {} kW", round(r60.getRefrigeratingCapacity()), round(r60.getPowerInput()));

            // Report model build (this is the path that previously threw for cascade units with no compressorSpecs).
            // Reload the unit so its unitDetails graph is hydrated exactly like the real /report flow (findById).
            em.flush();
            em.clear();
            Unit freshUnit = em.find(Unit.class, unit.getId());
            try {
                // Same operating inputs the calc used (SC=5, SH=10, 50 Hz) so the PDF matches the UI.
                var coolOp = org.offitec.osp.application.report.ReportDataAssembler.OpInputs.of(50.0, 5.0, 10.0, null);
                var reportModel = reportAssembler.assemble(freshUnit, Mod.COOLING, 35, 12, 7,
                        null, null, null, null, java.util.Locale.ENGLISH, false, 0, 0, 0, coolOp, coolOp);
                log.info("MATCH? calc capacity={} vs report capKw={}", round(r.getRefrigeratingCapacity()), reportModel.getCoolingCapacityKw());
                log.info("REPORT model: capKw={} powKw={} eer={} ambient={} waterOut={} comp={} refrig={} fullLoadRows={}",
                        reportModel.getCoolingCapacityKw(), reportModel.getInputPowerKw(), reportModel.getEerCopValue(),
                        reportModel.getAmbient(), reportModel.getWaterOutlet(), reportModel.getCompressorModel(),
                        reportModel.getRefrigerantCode(), reportModel.getFullLoad() == null ? -1 : reportModel.getFullLoad().size());
                byte[] pdf = pdfReportService.render(reportModel);
                java.nio.file.Files.write(java.nio.file.Path.of("C:/Users/mak/Desktop/fulls/test-report.pdf"), pdf);
                log.info("REPORT pdf bytes = {}  -> wrote test-report.pdf", pdf.length);
            } catch (Exception e) {
                log.error("REPORT assemble FAILED", e);
            }
            log.info("=== calc endpoint smoke test done ===");

            status.setRollbackOnly(); // undo the seeded unit and calculate()'s persistence
        });
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
