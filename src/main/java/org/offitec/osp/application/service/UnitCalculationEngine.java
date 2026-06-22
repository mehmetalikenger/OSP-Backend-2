package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.CompressorSpecs;
import org.springframework.stereotype.Component;

/**
 * Pure calculation logic shared by the live /units/calculate endpoint and the
 * PDF report assembler. Evaluates the compressor's bivariate cubic performance
 * surface for a given operating point and scales it by the compressor count.
 *
 * The independent variables fed to the polynomial are derived from the operating
 * conditions exactly as the original calculate() did:
 *   S = evapOut - 5   (evaporating-side variable, from the water outlet temp)
 *   D = ambient + 15  (condensing-side variable, from the ambient temp)
 */
@Component
public class UnitCalculationEngine {

    /** Result of one operating point: total capacity (kW), total power (kW) and EER/COP. */
    public record Result(double capacityKw, double powerKw, double copEer) {}

    public Result compute(CompressorSpecs specs, int compressorQty, double ambient, double evapOut) {
        double s = evapOut - 5;
        double d = ambient + 15;

        double q = evalPolynomial(s, d,
                specs.getQC1(), specs.getQC2(), specs.getQC3(), specs.getQC4(), specs.getQC5(),
                specs.getQC6(), specs.getQC7(), specs.getQC8(), specs.getQC9(), specs.getQC10());

        double p = evalPolynomial(s, d,
                specs.getPC1(), specs.getPC2(), specs.getPC3(), specs.getPC4(), specs.getPC5(),
                specs.getPC6(), specs.getPC7(), specs.getPC8(), specs.getPC9(), specs.getPC10());

        int qty = Math.max(compressorQty, 1);
        // The polynomials return power in WATTS; convert the totals to kW. COP/EER is a
        // ratio of the two, so it's unit-independent and computed before the conversion.
        double totalQ = q * qty;
        double totalP = p * qty;
        double copEer = totalP > 0 ? totalQ / totalP : 0;

        return new Result(totalQ / 1000.0, totalP / 1000.0, copEer);
    }

    public double evalPolynomial(double s, double d,
            double c1, double c2, double c3, double c4, double c5,
            double c6, double c7, double c8, double c9, double c10) {
        return c1
                + c2 * s
                + c3 * d
                + c4 * s * s
                + c5 * s * d
                + c6 * d * d
                + c7 * s * s * s
                + c8 * d * s * s
                + c9 * s * d * d
                + c10 * d * d * d;
    }
}
