package org.offitec.osp.infrastructure.coolprop;

import org.offitec.osp.application.service.CompressorPerformanceEngine;
import org.offitec.osp.application.service.CompressorPerformanceEngine.Input;
import org.offitec.osp.application.service.CompressorPerformanceEngine.Result;
import org.offitec.osp.application.service.CompressorPerformanceEngine.Suction;
import org.offitec.osp.domain.entity.CompressorRating;
import org.offitec.osp.infrastructure.repository.CompressorRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ad-hoc single-point evaluator for validating against FSS3. Active when {@code engine.calc.model}
 * is set; reads the operating point from properties and logs the full result. Example:
 * {@code -Dengine.calc.model=V25-71.1AXH -Dengine.calc.refrigerant=R290 -Dengine.calc.te=2
 *  -Dengine.calc.tc=50 -Dengine.calc.sh=10 -Dengine.calc.sc=5 -Dengine.calc.hz=60}.
 */
@Component
@Order(115)
public class EngineCalcRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EngineCalcRunner.class);

    private final CompressorRatingRepository ratingRepo;
    private final CompressorPerformanceEngine engine;

    @Value("${engine.calc.model:}")        private String model;
    @Value("${engine.calc.refrigerant:}")  private String refrigerant;
    @Value("${engine.calc.te:}")           private Double te;
    @Value("${engine.calc.tc:}")           private Double tc;
    @Value("${engine.calc.sh:}")           private Double sh;   // superheat K (omit if using suction gas temp)
    @Value("${engine.calc.tsuc:}")         private Double tsuc; // absolute suction gas temp °C (overrides sh)
    @Value("${engine.calc.sc:0}")          private double sc;   // subcooling K
    @Value("${engine.calc.hz:50}")         private double hz;

    public EngineCalcRunner(CompressorRatingRepository ratingRepo, CompressorPerformanceEngine engine) {
        this.ratingRepo = ratingRepo;
        this.engine = engine;
    }

    @Override
    public void run(String... args) {
        if (model == null || model.isBlank()) return;

        List<CompressorRating> ratings = ratingRepo.findByModelAndRefrigerant(model.trim(), refrigerant.trim());
        if (ratings.isEmpty()) { log.warn("engine.calc: no rating for {} / {}", model, refrigerant); return; }
        CompressorRating r = ratings.get(0);
        String fluid = r.getRefrigerant().getCoolpropName();
        boolean tk = "TK".equalsIgnoreCase(r.getCompressor().getFrascoldType());

        Suction suction = (tsuc != null)
                ? new CompressorPerformanceEngine.SuctionGasTemp(tsuc)
                : new CompressorPerformanceEngine.Superheat(sh != null ? sh : 10.0);

        Result res = engine.compute(new Input(r, fluid, tk, te, tc, suction, sc, hz, null, null));

        log.info("================ ENGINE CALC ================");
        log.info("compressor : {}  ({}, fluid={}, ohRef={}, scRef={}, maxFreq={})",
                model, r.getCompressor().getFrascoldType(), fluid, r.getOhRef(), r.getScRef(), r.getMaxFrequency());
        log.info("point      : Te={} °C  Tc={} °C  SH={} K  SC={} K  f={} Hz  (per single compressor)",
                te, tc, (tsuc != null ? "(Tsuc=" + tsuc + "°C)" : sh), sc, hz);
        log.info("valid={}  inEnvelope={}", res.valid(), res.withinEnvelope());
        log.info("Cooling Capacity : {} kW   ({} W)", round(res.coolingCapacityW() / 1000), round(res.coolingCapacityW()));
        log.info("Power Input      : {} kW   ({} W)", round(res.powerInputW() / 1000), round(res.powerInputW()));
        log.info("COP / EER        : {}", round(res.cop()));
        log.info("Mass flow (suct) : {} kg/h", round(res.massFlowKgH()));
        log.info("Condenser duty   : {} kW", round(res.condenserDutyW() / 1000));
        log.info("Discharge temp   : {} °C", round(res.dischargeTempC()));
        log.info("Suction gas temp : {} °C   Liquid temp : {} °C", round(res.suctionGasTempC()), round(res.liquidTempC()));
        log.info("=============================================");
    }

    private static double round(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
