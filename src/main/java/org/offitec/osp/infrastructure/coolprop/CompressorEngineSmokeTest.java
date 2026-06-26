package org.offitec.osp.infrastructure.coolprop;

import org.offitec.osp.application.service.CompressorPerformanceEngine;
import org.offitec.osp.application.service.CompressorPerformanceEngine.Input;
import org.offitec.osp.application.service.CompressorPerformanceEngine.Result;
import org.offitec.osp.application.service.CompressorPerformanceEngine.Superheat;
import org.offitec.osp.domain.entity.CompressorRating;
import org.offitec.osp.infrastructure.repository.CompressorRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * End-to-end check of the performance engine against real catalogue data. Runs only when
 * {@code engine.smoketest=true}.
 *
 * <p>The decisive test is self-consistency: evaluated at the rating's <em>reference</em> conditions
 * (reference superheat, zero subcooling, 50 Hz) the rebuilt cycle must reproduce the EN12900
 * capacity polynomial. Then we show that subcooling raises capacity and that 60 Hz scales it.</p>
 */
@Component
@Order(110) // after the frdata import (@Order 100) so the catalogue is present
public class CompressorEngineSmokeTest implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CompressorEngineSmokeTest.class);

    private final CompressorRatingRepository ratingRepo;
    private final CompressorPerformanceEngine engine;
    private final boolean enabled;

    public CompressorEngineSmokeTest(CompressorRatingRepository ratingRepo,
                                     CompressorPerformanceEngine engine,
                                     @org.springframework.beans.factory.annotation.Value("${engine.smoketest:false}") boolean enabled) {
        this.ratingRepo = ratingRepo;
        this.engine = engine;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;

        List<CompressorRating> ratings = ratingRepo.findByModelAndRefrigerant("S10-52Y", "R404A");
        if (ratings.isEmpty()) { log.warn("engine smoke test: S10-52Y/R404A not found"); return; }
        CompressorRating r = ratings.get(0);
        String fluid = r.getRefrigerant().getCoolpropName();
        boolean tk = "TK".equalsIgnoreCase(r.getCompressor().getFrascoldType());

        log.info("=== engine smoke test: {} / {} (fluid={}, ohRef={}, scRef={}) ===",
                r.getCompressor().getModel(), r.getRefrigerant().getCode(), fluid, r.getOhRef(), r.getScRef());

        // 1) Self-consistency at reference conditions.
        double teRef = -10, tcRef = 45;
        double refSuperheat = (r.getOhRef() == 999.0) ? (r.getTaspRef() - teRef) : r.getOhRef();
        Result atRef = engine.compute(new Input(r, fluid, tk, teRef, tcRef,
                new Superheat(refSuperheat), r.getScRef(), 50, null));
        double polyQ = CompressorPerformanceEngine.en12900(r.getCapCoeffs(), teRef, tcRef);
        double polyP = CompressorPerformanceEngine.en12900(r.getPowerCoeffs(), teRef, tcRef);
        log.info("REFERENCE  Te={} Tc={}: cycle Q={} W vs poly Q={} W  (Δ={}%)",
                teRef, tcRef, round(atRef.coolingCapacityW()), round(polyQ),
                round(100 * (atRef.coolingCapacityW() - polyQ) / polyQ));
        log.info("           cycle P={} W vs poly P={} W ; COP={}",
                round(atRef.powerInputW()), round(polyP), round3(atRef.cop()));

        // 2) Realistic operating point at 50 Hz, 10 K superheat, 0 K subcooling.
        Result base = engine.compute(new Input(r, fluid, tk, -10, 45, new Superheat(10), 0, 50, null));
        log.info("50Hz SH10 SC0  : Q={} kW  P={} kW  EER={}  mdot={} kg/h  Tdis={} C  inEnv={}",
                kw(base.coolingCapacityW()), kw(base.powerInputW()), round3(base.cop()),
                round(base.massFlowKgH()), round(base.dischargeTempC()), base.withinEnvelope());

        // 3) Same point with 5 K subcooling → capacity should rise.
        Result sub = engine.compute(new Input(r, fluid, tk, -10, 45, new Superheat(10), 5, 50, null));
        log.info("50Hz SH10 SC5  : Q={} kW  (Δ vs SC0 = {}%)  P={} kW",
                kw(sub.coolingCapacityW()), round(100 * (sub.coolingCapacityW() - base.coolingCapacityW()) / base.coolingCapacityW()),
                kw(sub.powerInputW()));

        // 4) Same point at 60 Hz → capacity & power scale by the frequency factor.
        Result hz60 = engine.compute(new Input(r, fluid, tk, -10, 45, new Superheat(10), 0, 60, null));
        log.info("60Hz SH10 SC0  : Q={} kW  (Δ vs 50Hz = {}%)  P={} kW  EER={}",
                kw(hz60.coolingCapacityW()), round(100 * (hz60.coolingCapacityW() - base.coolingCapacityW()) / base.coolingCapacityW()),
                kw(hz60.powerInputW()), round3(hz60.cop()));
        log.info("=== engine smoke test done ===");
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private static double kw(double w) { return Math.round(w / 10.0) / 100.0; }
}
