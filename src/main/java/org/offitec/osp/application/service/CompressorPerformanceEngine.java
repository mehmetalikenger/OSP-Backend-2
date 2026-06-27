package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.CompressorRating;
import org.springframework.stereotype.Component;

/**
 * Faithful Java port of the FSS3 compressor performance cycle (Compressor.cs + Performance.cs
 * DefineWorkingPoints), backed by {@link RefrigerantProperties} (CoolProp).
 *
 * <p>The EN12900 polynomial gives the <em>reference</em> capacity/power at the rating's reference
 * superheat and subcooling. The actual capacity for the user's conditions is recovered by rebuilding
 * the refrigeration cycle: reference mass flow = Q_ref / (h_suction_ref − h_liquid_sat), corrected
 * for the actual suction density, then Q = ṁ·(h_suction − h_liquid) with the user's superheat and
 * subcooling. This reduces exactly to the polynomial at the reference conditions.</p>
 *
 * <p>Subcritical (SK) refrigerants are fully supported. Transcritical CO₂ (TK) needs the gas-cooler
 * branch and is rejected for now ({@link #isSupported}).</p>
 */
@Component
public class CompressorPerformanceEngine {

    private final RefrigerantProperties props;

    public CompressorPerformanceEngine(RefrigerantProperties props) {
        this.props = props;
    }

    /** How the user pinned the suction state: a superheat (K above dew) or an absolute gas temp (°C). */
    public sealed interface Suction permits Superheat, SuctionGasTemp {}
    public record Superheat(double kelvin) implements Suction {}
    public record SuctionGasTemp(double celsius) implements Suction {}

    public record Input(
            CompressorRating rating,
            String coolpropFluid,
            boolean transcritical,
            double evapTempC,        // Te — saturated suction (dew) temperature
            double condTempC,        // Tc — saturated condensing (dew) temperature
            Suction suction,
            double subcoolingK,      // liquid subcooling below the condenser bubble point
            double frequencyHz,      // 50, 60, or an inverter frequency
            Double evapSuperheatK,   // optional: evaporator-outlet superheat for evaporator duty
            Double rpm               // optional: compressor speed (Copeland trivariate variable R)
    ) {}

    public record Result(
            double coolingCapacityW,
            double powerInputW,
            double cop,
            double massFlowKgH,
            double condenserDutyW,
            double evaporatorDutyW,
            double dischargeTempC,
            double suctionGasTempC,
            double liquidTempC,
            boolean withinEnvelope,
            boolean valid
    ) {}

    /** Whether the underlying property engine (CoolProp) is loaded and usable. */
    public boolean isAvailable() {
        return props.isAvailable();
    }

    public boolean isSupported(Input in) {
        if (in.rating() == null) return false;
        // Copeland ratings use the direct polynomial path: supported as long as they carry
        // coefficients. CoolProp is only needed for the optional subcooling bump.
        if (isCopeland(in.rating())) {
            return in.rating().getCapCoeffs() != null && in.rating().getPowerCoeffs() != null;
        }
        return props.isAvailable() && !in.transcritical() && in.coolpropFluid() != null;
    }

    // Brand-driven engine branch: Copeland (admin-entered direct polynomials, incl. the typo'd
    // "Copelant") vs Frascold (imported cycle reconstruction). Matches the legacy UnitCalculationEngine.
    private static boolean isCopeland(CompressorRating r) {
        if (r == null || r.getCompressor() == null) return false;
        String b = r.getCompressor().getBrand();
        return b != null && b.trim().toLowerCase().startsWith("copel");
    }

    public Result compute(Input in) {
        if (!isSupported(in)) {
            return invalid();
        }
        if (isCopeland(in.rating())) {
            return computeCopeland(in);
        }
        final String f = in.coolpropFluid();
        final CompressorRating r = in.rating();
        final double teC = in.evapTempC();
        final double tcC = in.condTempC();

        // Saturation pressures (Pa) from the dew-point temperatures.
        final double pEvap = props.pDew(f, teC + 273.15);
        final double pCond = props.pDew(f, tcC + 273.15);
        final double tBubCondK = props.tBubble(f, pCond);
        if (!finite(pEvap, pCond, tBubCondK)) return invalid();

        // Reference suction state (FSS3 point "0"): fixed suction temp when ohRef==999, else Te+ohRef.
        final double tSucRefK = (r.getOhRef() == 999.0)
                ? r.getTaspRef() + 273.15
                : teC + r.getOhRef() + 273.15;
        final double h0 = props.enthalpyTp(f, tSucRefK, pEvap);
        final double d0 = props.densityTp(f, tSucRefK, pEvap);

        // Actual suction state (point "1").
        final double tSucK = switch (in.suction()) {
            case Superheat sh -> teC + sh.kelvin() + 273.15;
            case SuctionGasTemp t -> t.celsius() + 273.15;
        };
        final double h1 = props.enthalpyTp(f, tSucK, pEvap);
        final double d1 = props.densityTp(f, tSucK, pEvap);

        // Reference mass-flow basis: saturated liquid at the condenser bubble point (FSS3 MassFlow).
        final double hLiqSat = props.enthalpyBubble(f, tBubCondK);
        if (!finite(h0, d0, h1, d1, hLiqSat) || h0 - hLiqSat <= 0) return invalid();

        // Reference cooling capacity / power from the EN12900 polynomials (Te, Tc in °C).
        final double qRef = en12900(r.getCapCoeffs(), teC, tcC);
        final double pRef = en12900(r.getPowerCoeffs(), teC, tcC);

        // Reference mass flow [kg/s], then corrected for the actual suction density.
        double massFlow = qRef / (h0 - hLiqSat) * (d1 / d0);

        // Frequency factor (50/60 Hz and inverter). 1.0 when no frequency coeffs or f<=0.
        final double fq = freqFactor(r.getFreqCapCoeffs(), in.frequencyHz(), teC, tcC);
        final double fp = freqFactor(r.getFreqPowerCoeffs(), in.frequencyHz(), teC, tcC);
        massFlow *= fq;

        // Actual liquid state (point "3"): subcooled below the bubble point. With (near-)zero
        // subcooling the liquid sits exactly on the saturation line, where PropsSI(H | T,P) is
        // undefined (two-phase) and returns NaN — so use the saturated-liquid enthalpy directly there
        // and only do the single-phase (T,p) lookup once genuinely subcooled.
        final double tLiqK = tBubCondK - in.subcoolingK();
        final double h3 = in.subcoolingK() <= 1e-4
                ? props.enthalpyBubble(f, tBubCondK)
                : props.enthalpyTp(f, tLiqK, pCond);
        if (!finite(h3) || massFlow <= 0) return invalid();

        final double coolingCapacity = massFlow * (h1 - h3);          // W
        final double power = Math.max(0.0001, pRef) * fp;             // W
        final double cop = power > 0 ? coolingCapacity / power : 0;

        // Discharge state (point "2") and condenser duty.
        final double h2 = h1 + power / massFlow;
        final double tDisK = props.temperaturePh(f, pCond, h2);
        final double condenserDuty = massFlow * (h2 - h3);            // W

        // Evaporator duty: uses the evaporator-outlet superheat when supplied, else equals capacity.
        final double evaporatorDuty;
        if (in.evapSuperheatK() != null) {
            double h1ev = props.enthalpyTp(f, teC + in.evapSuperheatK() + 273.15, pEvap);
            evaporatorDuty = finite(h1ev) ? massFlow * (h1ev - h3) : coolingCapacity;
        } else {
            evaporatorDuty = coolingCapacity;
        }

        final boolean within = pointInPolygon(teC, tcC, r.getEnvelope());
        // The primary outputs are capacity and power; a non-finite discharge temperature or condenser
        // duty (CoolProp inverse lookups that can fail near range limits) must NOT void them — otherwise
        // a perfectly good capacity/power point would collapse to zero. Degrade those secondaries instead.
        final boolean valid = finite(coolingCapacity, power);
        final double dischargeTempC = finite(tDisK) ? tDisK - 273.15 : 0.0;
        final double condDutyW = finite(condenserDuty) ? condenserDuty : coolingCapacity + power;

        return new Result(coolingCapacity, power, cop, massFlow * 3600.0,
                condDutyW, evaporatorDuty, dischargeTempC,
                tSucK - 273.15, tLiqK - 273.15, within, valid);
    }

    /**
     * Copeland direct-polynomial path. The capacity/power coefficient sets give kW directly as a
     * polynomial in (S = evaporating temp, D = condensing temp, R = speed) — bivariate (10 coeffs,
     * fixed-speed) or trivariate (20 coeffs, variable-speed). Mass flow is NOT a separate polynomial:
     * it is derived from the capacity and the refrigerating effect (h_suction − h_liquid_sat) via
     * CoolProp, exactly like the Frascold path. The same enthalpies give the subcooling correction.
     */
    private Result computeCopeland(Input in) {
        final CompressorRating r = in.rating();
        final double S = in.evapTempC();
        final double D = in.condTempC();
        // Variable-speed Copeland: the trivariate's speed variable R (rpm) is driven by the drive
        // frequency — R = Hz × 60 (2-pole synchronous speed: 50 Hz → 3000 rpm, 60 Hz → 3600 rpm).
        // An explicit rpm still overrides; if neither is usable, fall back to the rating's max speed.
        final double R = in.rpm() != null ? in.rpm()
                : in.frequencyHz() > 0 ? in.frequencyHz() * 60.0
                : (r.getMaxSpeed() != null ? r.getMaxSpeed() : 3000.0);

        final double capKw = poly(r.getCapCoeffs(), S, D, R);     // kW (at the polynomial's reference subcooling)
        final double powerKw = poly(r.getPowerCoeffs(), S, D, R); // kW

        double coolingCapacityW = capKw * 1000.0;
        final double powerInputW = powerKw * 1000.0;

        // Mass flow + subcooling correction come from the refrigerant cycle (CoolProp): the reference
        // capacity divided by the refrigerating effect (h_suction − h_satLiquid) is the mass flow, and
        // subcooling below the bubble point adds ṁ·Δh. Skipped (mass flow 0, no SC bump) when CoolProp
        // is unavailable or the lookups don't converge.
        double massKgS = 0.0;
        double dischargeTempC = 0.0;
        if (props.isAvailable() && in.coolpropFluid() != null) {
            try {
                final String f = in.coolpropFluid();
                final double pEvap = props.pDew(f, S + 273.15);
                final double tSucK = switch (in.suction()) {
                    case Superheat sh -> S + sh.kelvin() + 273.15;
                    case SuctionGasTemp t -> t.celsius() + 273.15;
                };
                final double h1 = props.enthalpyTp(f, tSucK, pEvap);
                final double pCond = props.pDew(f, D + 273.15);
                final double tBubCondK = props.tBubble(f, pCond);
                final double hSatLiq = props.enthalpyBubble(f, tBubCondK);
                if (finite(pEvap, h1, pCond, tBubCondK, hSatLiq) && h1 - hSatLiq > 0) {
                    massKgS = coolingCapacityW / (h1 - hSatLiq); // derive mass flow from the capacity
                    if (in.subcoolingK() > 1e-4) {
                        final double hSub = props.enthalpyTp(f, (D - in.subcoolingK()) + 273.15, pCond);
                        final double dh = hSatLiq - hSub; // J/kg
                        if (finite(hSub, dh)) coolingCapacityW += massKgS * dh; // W
                    }
                    // Discharge state (point 2): h2 = h_suction + power/ṁ, then the discharge gas
                    // temperature from a (p,h) inverse lookup at condensing pressure — same construction
                    // as the Frascold path. Left at 0 if the lookup doesn't converge.
                    if (massKgS > 0) {
                        final double h2 = h1 + powerInputW / massKgS;
                        final double tDisK = props.temperaturePh(f, pCond, h2);
                        if (finite(h2, tDisK)) dischargeTempC = tDisK - 273.15;
                    }
                }
            } catch (RuntimeException ignored) {
                // non-finite / out-of-range property lookup → mass flow 0, capacity stays at the polynomial value
            }
        }

        final double condenserDutyW = coolingCapacityW + powerInputW;
        final double cop = powerInputW > 0 ? coolingCapacityW / powerInputW : 0;
        final boolean within = pointInPolygon(S, D, r.getEnvelope());
        final boolean valid = finite(capKw, powerKw);

        return new Result(coolingCapacityW, powerInputW, cop, massKgS * 3600.0,
                condenserDutyW, coolingCapacityW, dischargeTempC, 0, 0, within, valid);
    }

    // Copeland polynomial: trivariate (>=20 coeffs) in (S,D,R) or bivariate (10 coeffs) in (S,D).
    static double poly(double[] c, double S, double D, double R) {
        if (c == null) return 0.0;
        return c.length >= 20 ? trivariate(c, S, D, R) : bivariate(c, S, D);
    }

    static double bivariate(double[] c, double S, double D) {
        return c[0] + c[1] * S + c[2] * D
                + c[3] * S * S + c[4] * S * D + c[5] * D * D
                + c[6] * S * S * S + c[7] * D * S * S + c[8] * S * D * D + c[9] * D * D * D;
    }

    static double trivariate(double[] c, double S, double D, double R) {
        return c[0] + c[1] * S + c[2] * D + c[3] * R
                + c[4] * S * D + c[5] * S * R + c[6] * D * R
                + c[7] * S * S + c[8] * D * D + c[9] * R * R
                + c[10] * S * D * R + c[11] * S * S * D + c[12] * S * S * R + c[13] * S * S * S
                + c[14] * S * D * D + c[15] * D * D * R + c[16] * D * D * D
                + c[17] * S * R * R + c[18] * D * R * R + c[19] * R * R * R;
    }

    // EN12900 10-coefficient bivariate cubic in (t0, tc) [°C]. Matches Compressor.CoolingCapacityRef.
    public static double en12900(double[] c, double t0, double tc) {
        double v = c[0] + c[1] * t0 + c[2] * tc
                + c[3] * t0 * t0 + c[4] * t0 * tc + c[5] * tc * tc
                + c[6] * t0 * t0 * t0 + c[7] * tc * t0 * t0 + c[8] * t0 * tc * tc + c[9] * tc * tc * tc;
        return Math.max(0.0001, v);
    }

    // SK frequency-correction factor. Matches Compressor.FrequencyFactorQ/P (SK branch):
    //   d = f/50 - 1
    //   poly = c0 + c1*d + c2*Te + c3*Tc + c4*d*Te + c5*Te*Tc + c6*d*Tc + c7*d^2 + c8*Te^2 + c9*Tc^2
    //   factor = 1 + d*poly   (1.0 when no coeffs or f<=0)
    static double freqFactor(double[] c, double f, double te, double tc) {
        if (c == null || f <= 0) return 1.0;
        double d = f / 50.0 - 1.0;
        double poly = c[0] + c[1] * d + c[2] * te + c[3] * tc
                + c[4] * d * te + c[5] * te * tc + c[6] * d * tc
                + c[7] * d * d + c[8] * te * te + c[9] * tc * tc;
        return 1.0 + d * poly;
    }

    // Operating-envelope test: is (te, tc) inside the polygon of [Te, Tc] vertices? Ray casting.
    static boolean pointInPolygon(double te, double tc, double[][] poly) {
        if (poly == null || poly.length < 3) return true; // no envelope on record → don't block
        boolean inside = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            double xi = poly[i][0], yi = poly[i][1];
            double xj = poly[j][0], yj = poly[j][1];
            boolean intersect = ((yi > tc) != (yj > tc))
                    && (te < (xj - xi) * (tc - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private static boolean finite(double... xs) {
        for (double x : xs) if (Double.isNaN(x) || Double.isInfinite(x)) return false;
        return true;
    }

    private static Result invalid() {
        return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
    }
}
