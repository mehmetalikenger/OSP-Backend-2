package org.offitec.osp.application.service;

import org.offitec.osp.infrastructure.coolprop.CoolPropNative;
import org.springframework.stereotype.Service;

/**
 * Thermodynamic refrigerant properties, the Java replacement for FSS3's ASEREP32.dll wrapper
 * (LibreriaRefrigerante), backed by CoolProp via {@link CoolPropNative}.
 *
 * <p>All arguments and results are SI: temperature in K, pressure in Pa, enthalpy in J/kg,
 * entropy in J/(kg·K), density in kg/m³, quality 0..1, molar mass in kg/mol. The calculation
 * engine converts from the catalogue's °C / bar at its boundary, exactly as FSS3 did.</p>
 *
 * <p>Method names echo the FSS3 routines (B = bubble/saturated-liquid, D = dew/saturated-vapor)
 * so the cycle port maps across one-to-one.</p>
 */
@Service
public class RefrigerantProperties {

    private final CoolPropNative coolprop;

    public RefrigerantProperties(CoolPropNative coolprop) {
        this.coolprop = coolprop;
    }

    public boolean isAvailable() {
        return coolprop.isAvailable();
    }

    // ---- saturation (FSS3 T_D / T_B) ----

    /** Dew-line (saturated vapour) temperature [K] at pressure p [Pa]. FSS3 T_D. */
    public double tDew(String fluid, double pPa) {
        return coolprop.propsSI("T", "P", pPa, "Q", 1.0, fluid);
    }

    /** Bubble-line (saturated liquid) temperature [K] at pressure p [Pa]. FSS3 T_B. */
    public double tBubble(String fluid, double pPa) {
        return coolprop.propsSI("T", "P", pPa, "Q", 0.0, fluid);
    }

    /** Saturation (dew) pressure [Pa] at temperature T [K]. */
    public double pDew(String fluid, double tK) {
        return coolprop.propsSI("P", "T", tK, "Q", 1.0, fluid);
    }

    // ---- single-phase state (FSS3 h_Tp / s_Tp / v_Tp) ----

    /** Enthalpy [J/kg] at (T [K], p [Pa]). FSS3 h_Tp. */
    public double enthalpyTp(String fluid, double tK, double pPa) {
        return coolprop.propsSI("H", "T", tK, "P", pPa, fluid);
    }

    /** Entropy [J/(kg·K)] at (T [K], p [Pa]). FSS3 s_Tp. */
    public double entropyTp(String fluid, double tK, double pPa) {
        return coolprop.propsSI("S", "T", tK, "P", pPa, fluid);
    }

    /** Density [kg/m³] at (T [K], p [Pa]). */
    public double densityTp(String fluid, double tK, double pPa) {
        return coolprop.propsSI("D", "T", tK, "P", pPa, fluid);
    }

    // ---- saturated-phase enthalpies (FSS3 h_B / h_D) ----

    /** Saturated-liquid enthalpy [J/kg] at T [K]. FSS3 h_B. */
    public double enthalpyBubble(String fluid, double tK) {
        return coolprop.propsSI("H", "T", tK, "Q", 0.0, fluid);
    }

    /** Saturated-vapour enthalpy [J/kg] at T [K]. FSS3 h_D. */
    public double enthalpyDew(String fluid, double tK) {
        return coolprop.propsSI("H", "T", tK, "Q", 1.0, fluid);
    }

    // ---- inverse lookups used to find discharge / economiser states (FSS3 T_ph / T_ps / x_ps) ----

    /** Temperature [K] from (p [Pa], h [J/kg]). FSS3 T_ph. */
    public double temperaturePh(String fluid, double pPa, double hJkg) {
        return coolprop.propsSI("T", "P", pPa, "H", hJkg, fluid);
    }

    /** Temperature [K] from (p [Pa], s [J/(kg·K)]). FSS3 T_ps. */
    public double temperaturePs(String fluid, double pPa, double sJkgK) {
        return coolprop.propsSI("T", "P", pPa, "S", sJkgK, fluid);
    }

    /** Vapour quality [0..1] from (p [Pa], s [J/(kg·K)]). FSS3 x_ps. */
    public double qualityPs(String fluid, double pPa, double sJkgK) {
        return coolprop.propsSI("Q", "P", pPa, "S", sJkgK, fluid);
    }

    // ---- fluid constants ----

    /** Molar mass [kg/mol]. FSS3 m_mass. */
    public double molarMass(String fluid) {
        return coolprop.propsSI("molar_mass", "T", 300.0, "P", 101325.0, fluid);
    }

    /** Critical temperature [K]. */
    public double criticalTemperature(String fluid) {
        return coolprop.propsSI("Tcrit", "T", 300.0, "P", 101325.0, fluid);
    }

    /** Critical pressure [Pa]. */
    public double criticalPressure(String fluid) {
        return coolprop.propsSI("Pcrit", "T", 300.0, "P", 101325.0, fluid);
    }
}
