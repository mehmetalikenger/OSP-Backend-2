package org.offitec.osp.application.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Glycol mixture correction factors (from the product's values table). When a glycol
 * mixture and percentage are selected, the calculated cooling capacity, compressor power
 * input and pressure drop are multiplied by the matching factors.
 *
 * Percentages are 5..50 in steps of 5. Unknown/blank selections return {@link Factors#NONE}
 * (all 1.0), i.e. no correction.
 */
public final class GlycolCorrection {

    private GlycolCorrection() {}

    public record Factors(double capacity, double power, double pressureDrop) {
        public static final Factors NONE = new Factors(1.0, 1.0, 1.0);
    }

    // type (normalized) -> percentage -> factors
    private static final Map<String, Map<Integer, Factors>> TABLE = new HashMap<>();

    private static void put(String type, int pct, double cap, double pow, double pd) {
        TABLE.computeIfAbsent(type, k -> new HashMap<>()).put(pct, new Factors(cap, pow, pd));
    }

    static {
        // Ethylene Glycol
        put("ethyleneglycol", 5,  0.997, 0.999, 1.02);
        put("ethyleneglycol", 10, 0.994, 0.999, 1.04);
        put("ethyleneglycol", 15, 0.990, 0.998, 1.06);
        put("ethyleneglycol", 20, 0.986, 0.997, 1.08);
        put("ethyleneglycol", 25, 0.981, 0.996, 1.11);
        put("ethyleneglycol", 30, 0.976, 0.996, 1.14);
        put("ethyleneglycol", 35, 0.970, 0.995, 1.17);
        put("ethyleneglycol", 40, 0.964, 0.994, 1.12);
        put("ethyleneglycol", 45, 0.957, 0.993, 1.24);
        put("ethyleneglycol", 50, 0.950, 0.993, 1.28);
        // Propylene Glycol
        put("propyleneglycol", 5,  0.995, 0.999, 1.03);
        put("propyleneglycol", 10, 0.990, 0.997, 1.06);
        put("propyleneglycol", 15, 0.983, 0.995, 1.09);
        put("propyleneglycol", 20, 0.976, 0.993, 1.13);
        put("propyleneglycol", 25, 0.968, 0.991, 1.18);
        put("propyleneglycol", 30, 0.960, 0.988, 1.22);
        put("propyleneglycol", 35, 0.950, 0.986, 1.28);
        put("propyleneglycol", 40, 0.939, 0.983, 1.33);
        put("propyleneglycol", 45, 0.928, 0.980, 1.39);
        put("propyleneglycol", 50, 0.916, 0.977, 1.46);
    }

    /** Factors for the given mixture type + percentage, or {@link Factors#NONE} if not set/found. */
    public static Factors lookup(String type, Integer percentage) {
        if (type == null || percentage == null) return Factors.NONE;
        String key = type.toLowerCase().replaceAll("[^a-z]", ""); // "Ethylene Glycol" -> "ethyleneglycol"
        Map<Integer, Factors> byPct = TABLE.get(key);
        if (byPct == null) return Factors.NONE;
        return byPct.getOrDefault(percentage, Factors.NONE);
    }
}
