package org.offitec.osp.infrastructure.coolprop;

import org.offitec.osp.application.service.RefrigerantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sanity check for the CoolProp binding. Runs only when {@code coolprop.smoketest=true}. Prints a
 * few properties with known reference values so we can confirm the native library is wired and
 * returning correct numbers (not NaN).
 */
@Component
@Order(50)
public class CoolPropSmokeTest implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CoolPropSmokeTest.class);

    private final RefrigerantProperties props;
    private final boolean enabled;

    public CoolPropSmokeTest(RefrigerantProperties props,
                             @org.springframework.beans.factory.annotation.Value("${coolprop.smoketest:false}") boolean enabled) {
        this.props = props;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        log.info("=== CoolProp smoke test (available={}) ===", props.isAvailable());
        if (!props.isAvailable()) return;

        // Reference: R134a normal boiling point ~ -26.3 C (246.8 K) at 1 atm.
        double r134aTsat = props.tBubble("R134a", 101325.0);
        // Reference: CO2 critical point ~ 304.13 K, 7.377 MPa.
        double co2Tcrit = props.criticalTemperature("R744");
        double co2Pcrit = props.criticalPressure("R744");
        // Reference: R134a molar mass ~ 0.10203 kg/mol.
        double r134aM = props.molarMass("R134a");
        // R410A saturated temp at 1 atm (~ -51.4 C / 221.7 K, glide-averaged).
        double r410aTsat = props.tBubble("R410A", 101325.0);
        // Enthalpy of superheated R134a vapour at 300 K, 1 atm (sensible, non-NaN).
        double r134aH = props.enthalpyTp("R134a", 300.0, 101325.0);

        log.info("R134a Tsat@1atm   = {} K   (expect ~246.8)", round(r134aTsat));
        log.info("R744  Tcrit       = {} K   (expect ~304.13)", round(co2Tcrit));
        log.info("R744  Pcrit       = {} Pa  (expect ~7.377e6)", round(co2Pcrit));
        log.info("R134a molar mass  = {} kg/mol (expect ~0.10203)", r134aM);
        log.info("R410A Tsat@1atm   = {} K   (expect ~221.7)", round(r410aTsat));
        log.info("R134a h(300K,1atm)= {} J/kg (sensible, non-NaN)", round(r134aH));
        log.info("=== CoolProp smoke test done ===");
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
