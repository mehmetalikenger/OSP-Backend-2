package org.offitec.osp.application.report;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Flat, presentation-ready view model for the unit selection PDF report.
 *
 * Most fields are pre-formatted display strings so the Thymeleaf template stays
 * dumb. Raw numeric values needed to draw the SVG charts are kept as doubles and
 * live in {@link #workingLimit} and {@link #pressureCurve}.
 */
@Getter
@Builder
public class UnitReportModel {

    // --- Localized labels (key -> translated text) used by the template ('t' for brevity). ---
    private final Map<String, String> t;

    // --- Unit imagery embedded in the report ---
    private final String primaryImageUrl;       // shown next to the Configuration section ("" when none)
    private final List<String> drawingUrls;     // technical drawings, rendered at the end (empty when none)

    // --- Project information block ---
    private final String projectName;
    private final String responsiblePerson;   // user's name
    private final String email;
    private final String phone;
    private final String country;
    private final String city;
    private final String address;
    private final String printedDate;         // e.g. "Saturday, 21 June 2026"

    // --- Configuration ---
    private final String model;
    private final String category;             // e.g. "Air Cooled Chiller"

    // --- Inputs (entered by the user) ---
    private final String ambient;              // "35"
    private final String waterInlet;           // "12"
    private final String waterOutlet;          // "7"

    // --- Unit specifications ---
    private final String coolingCapacityKcalh; // "91,295"
    private final String coolingCapacityKw;    // "106.2"
    private final String inputPowerKw;         // "38.0"
    private final String eerCopLabel;          // "EER" or "COP"
    private final String eerCopValue;          // "2.80"

    // --- Full load cooling table (ambient -10..40) ---
    private final List<FullLoadRow> fullLoad;

    // --- Technical specifications ---
    private final String refrigerantCode;
    private final String compressorModel;
    private final String compressorBrand;
    private final String compressorQty;        // "n" unit
    private final String circuitQty;
    private final String condenserType;

    // --- Hydraulic kit ---
    private final String evaporatorType;
    private final String flowRate;             // "18.26" m³/h
    private final String waterPressure;        // "1.5" bar
    private final String pressureDrop;         // "50" kPa

    // --- Fans ---
    private final String fanType;
    private final String fanQty;
    private final String airFlowRate;          // m³/h

    // --- Pipe connection ---
    private final String waterInletConnection;
    private final String waterOutletConnection;

    // --- Electrical data ---
    private final String moc;                  // compressor MOC * compressor qty, in A
    private final String lra;                  // compressor LRA, in A

    // --- Unit dimensions ---
    private final String length;               // m
    private final String width;
    private final String height;

    // --- Raw chart data (consumed by the SVG builders, not the table cells) ---
    private final WorkingLimit workingLimit;
    private final PressureCurve pressureCurve;

    @Getter
    @Builder
    public static class FullLoadRow {
        private final String ambient;   // "-10.0"
        private final String capacity;  // "142"
        private final String power;     // "19.1"
        private final String eerCop;    // "7.40"
    }

    /** Safe-area rectangle bounds + the plotted operating point for the working-limit graph. */
    @Getter
    @Builder
    public static class WorkingLimit {
        private final double minWaterOutlet;
        private final double maxWaterOutlet;
        private final double minAmbient;
        private final double maxAmbient;
        private final double pointWaterOutlet;  // operating point x = user water outlet
        private final double pointAmbient;       // operating point y = user ambient
    }

    /** Design flow rate + design pressure drop; the curve is synthesized as PD = PDd*(Q/Qd)^2. */
    @Getter
    @Builder
    public static class PressureCurve {
        private final double designFlowRate;     // Qd, m³/h
        private final double designPressureDrop;  // PDd, kPa
    }
}
