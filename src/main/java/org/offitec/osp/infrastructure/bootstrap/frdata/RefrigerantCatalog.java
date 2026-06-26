package org.offitec.osp.infrastructure.bootstrap.frdata;

import java.util.Map;

/**
 * Maps Frascold/ASEREP refrigerant names to the names understood by the CoolProp property engine.
 *
 * <p>An entry's presence means "CoolProp can model this fluid" → the rating is calculable. Names not
 * present here are imported for completeness but flagged non-calculable and reported, per the
 * "skip + report" decision. This list is the single source of truth and will be reconciled against
 * the actual CoolProp build when the property layer is wired (it is intentionally conservative now).</p>
 */
public final class RefrigerantCatalog {

    private RefrigerantCatalog() {}

    // Frascold name -> CoolProp fluid name (identity unless CoolProp uses a different spelling).
    private static final Map<String, String> COOLPROP = Map.ofEntries(
            // pure fluids
            Map.entry("R134a", "R134a"),
            Map.entry("R22", "R22"),
            Map.entry("R23", "R23"),
            Map.entry("R290", "R290"),
            Map.entry("R170", "R170"),
            Map.entry("R744", "R744"),
            Map.entry("R717", "R717"),
            Map.entry("R1270", "R1270"),
            Map.entry("R600", "R600"),
            Map.entry("R600a", "R600a"),
            Map.entry("R601a", "R601a"),
            Map.entry("R1234yf", "R1234yf"),
            Map.entry("R1234ze", "R1234ze(E)"),
            Map.entry("R1233zd", "R1233zd(E)"),
            // predefined mixtures
            Map.entry("R404A", "R404A"),
            Map.entry("R407A", "R407A"),
            Map.entry("R407C", "R407C"),
            Map.entry("R407F", "R407F"),
            Map.entry("R410A", "R410A"),
            Map.entry("R448A", "R448A"),
            Map.entry("R449A", "R449A"),
            Map.entry("R450A", "R450A"),
            Map.entry("R452A", "R452A"),
            Map.entry("R454C", "R454C"),
            Map.entry("R455A", "R455A"),
            Map.entry("R507", "R507A"),
            Map.entry("R513A", "R513A")
            // Not yet mapped (reported on import): R407H, R471A, R515B
    );

    /** CoolProp name for a Frascold refrigerant, or null if unsupported. */
    public static String coolpropName(String frascoldName) {
        return frascoldName == null ? null : COOLPROP.get(frascoldName.trim());
    }

    public static boolean isSupported(String frascoldName) {
        return coolpropName(frascoldName) != null;
    }
}
