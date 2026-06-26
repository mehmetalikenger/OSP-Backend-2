package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.Compressor;
import org.offitec.osp.domain.entity.CompressorSpecs;
import org.offitec.osp.domain.enums.CompressorKind;
import org.offitec.osp.domain.enums.Mod;
import org.springframework.stereotype.Component;

/**
 * Pure calculation logic shared by the live /units/calculate endpoint and the
 * PDF report assembler. Evaluates the compressor's performance surface for a given
 * operating point and scales it by the compressor count.
 *
 * The independent variables depend on the mode:
 *   COOLING: S = leavingWaterTemp - 5  (evaporating side, the chilled-water outlet)
 *            D = ambient + 15          (condensing side, from the ambient air)
 *   HEATING: S = ambient - 5           (evaporating side, the air source)
 *            D = leavingWaterTemp + 5  (condensing side, the hot-water outlet)
 *
 * Standard compressors use a 10-coefficient bivariate cubic in (S, D). ISCR
 * (variable-speed) Copeland compressors instead use a 20-coefficient trivariate
 * cubic in (S, D, R), where R is the compressor's base RPM.
 */
@Component
public class UnitCalculationEngine {

    /** Result of one operating point: total capacity (kW), total power (kW) and EER/COP. */
    public record Result(double capacityKw, double powerKw, double copEer) {}

    /** Cooling-mode convenience overload (kept for callers that only rate the cooling point). */
    public Result compute(CompressorSpecs specs, int compressorQty, double ambient, double leavingWaterTemp) {
        return compute(specs, compressorQty, Mod.COOLING, ambient, leavingWaterTemp);
    }

    public Result compute(CompressorSpecs specs, int compressorQty, Mod mod, double ambient, double leavingWaterTemp) {
        double s, d;
        if (mod == Mod.HEATING) {
            s = ambient - 5;
            d = leavingWaterTemp + 5;
        } else {
            s = leavingWaterTemp - 5;
            d = ambient + 15;
        }

        double q, p;
        if (isIscrCopeland(specs)) {
            double r = specs.getRpmBase();
            q = evalTrivariate(s, d, r,
                    specs.getQC1(), specs.getQC2(), specs.getQC3(), specs.getQC4(), specs.getQC5(),
                    specs.getQC6(), specs.getQC7(), specs.getQC8(), specs.getQC9(), specs.getQC10(),
                    nz(specs.getQC11()), nz(specs.getQC12()), nz(specs.getQC13()), nz(specs.getQC14()), nz(specs.getQC15()),
                    nz(specs.getQC16()), nz(specs.getQC17()), nz(specs.getQC18()), nz(specs.getQC19()), nz(specs.getQC20()));
            p = evalTrivariate(s, d, r,
                    specs.getPC1(), specs.getPC2(), specs.getPC3(), specs.getPC4(), specs.getPC5(),
                    specs.getPC6(), specs.getPC7(), specs.getPC8(), specs.getPC9(), specs.getPC10(),
                    nz(specs.getPC11()), nz(specs.getPC12()), nz(specs.getPC13()), nz(specs.getPC14()), nz(specs.getPC15()),
                    nz(specs.getPC16()), nz(specs.getPC17()), nz(specs.getPC18()), nz(specs.getPC19()), nz(specs.getPC20()));
        } else {
            q = evalPolynomial(s, d,
                    specs.getQC1(), specs.getQC2(), specs.getQC3(), specs.getQC4(), specs.getQC5(),
                    specs.getQC6(), specs.getQC7(), specs.getQC8(), specs.getQC9(), specs.getQC10());
            p = evalPolynomial(s, d,
                    specs.getPC1(), specs.getPC2(), specs.getPC3(), specs.getPC4(), specs.getPC5(),
                    specs.getPC6(), specs.getPC7(), specs.getPC8(), specs.getPC9(), specs.getPC10());
        }

        int qty = Math.max(compressorQty, 1);
        // The polynomials return power in WATTS; convert the totals to kW. COP/EER is a
        // ratio of the two, so it's unit-independent and computed before the conversion.
        double totalQ = q * qty;
        double totalP = p * qty;
        double copEer = totalP > 0 ? totalQ / totalP : 0;

        return new Result(totalQ / 1000.0, totalP / 1000.0, copEer);
    }

    // ISCR (variable-speed) Copeland compressors with a base RPM and the second
    // coefficient set (qC11..qC20) filled are rated with the trivariate formula.
    private boolean isIscrCopeland(CompressorSpecs specs) {
        Compressor c = specs.getCompressor();
        return c != null
                && c.getType() == CompressorKind.ISCR
                && isCopeland(c.getBrand())
                && specs.getRpmBase() != null
                && specs.getQC11() != null;
    }

    // The admin brand option is spelled "Copelant" (a typo for Copeland); accept both so the
    // trivariate formula is selected regardless of which spelling is stored.
    private static boolean isCopeland(String brand) {
        return brand != null && brand.trim().toLowerCase().startsWith("copel");
    }

    private static double nz(Double v) {
        return v != null ? v : 0.0;
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

    // Trivariate cubic in (S, D, R). Coefficients are the spec's qC1..qC20 / pC1..pC20,
    // mapped to C0..C19 of the formula (C0 = qC1, ..., C19 = qC20):
    //   X = C0 + C1*S + C2*D + C3*R + C4*S*D + C5*S*R + C6*D*R + C7*S^2 + C8*D^2 + C9*R^2
    //       + C10*S*D*R + C11*S^2*D + C12*S^2*R + C13*S^3 + C14*S*D^2 + C15*D^2*R
    //       + C16*D^3 + C17*S*R^2 + C18*D*R^2 + C19*R^3
    public double evalTrivariate(double s, double d, double r,
            double c0, double c1, double c2, double c3, double c4,
            double c5, double c6, double c7, double c8, double c9,
            double c10, double c11, double c12, double c13, double c14,
            double c15, double c16, double c17, double c18, double c19) {
        return c0
                + c1 * s
                + c2 * d
                + c3 * r
                + c4 * s * d
                + c5 * s * r
                + c6 * d * r
                + c7 * s * s
                + c8 * d * d
                + c9 * r * r
                + c10 * s * d * r
                + c11 * s * s * d
                + c12 * s * s * r
                + c13 * s * s * s
                + c14 * s * d * d
                + c15 * d * d * r
                + c16 * d * d * d
                + c17 * s * r * r
                + c18 * d * r * r
                + c19 * r * r * r;
    }
}
