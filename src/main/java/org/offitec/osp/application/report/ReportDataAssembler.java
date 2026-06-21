package org.offitec.osp.application.report;

import org.offitec.osp.application.service.UnitCalculationEngine;
import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the {@link UnitReportModel} for a unit + user-entered operating conditions.
 *
 * Several report values are not stored on the unit and are derived or hardcoded
 * per the product spec:
 *   - Cooling capacity (kcal/h) = capacity(kW) * 860
 *   - Flow rate (m³/h)          = capacity(kW) * 860 / 5000
 *   - Circuit qty               = compressor qty
 *   - Water pressure            = 1.5 bar     (constant)
 *   - Pressure drop             = 50 kPa      (constant)
 *   - MRA                       = 480 A       (constant)
 *   - LRA                       = 135.3 A     (constant)
 *   - Fan type / pipe connections = null for now ("-")
 */
@Component
public class ReportDataAssembler {

    // Standard ambient temperatures for the Full Load Cooling table.
    private static final double[] FULL_LOAD_AMBIENTS = {-10, 0, 10, 20, 30, 40};

    // Hardcoded constants (no DB field yet).
    private static final double WATER_PRESSURE_BAR = 1.5;
    private static final double PRESSURE_DROP_KPA = 50.0;
    private static final double MRA_A = 480.0;
    private static final double LRA_A = 135.3;

    // Fallback working-envelope bounds when a unit has none configured yet.
    private static final double DEFAULT_MIN_OUTLET = -5, DEFAULT_MAX_OUTLET = 25;
    private static final double DEFAULT_MIN_AMBIENT = -10, DEFAULT_MAX_AMBIENT = 48;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private final UnitCalculationEngine engine;

    public ReportDataAssembler(UnitCalculationEngine engine) {
        this.engine = engine;
    }

    public UnitReportModel assemble(Unit unit, Mod mod,
                                    double ambient, double evapIn, double evapOut,
                                    Project project, User user) {

        UnitDetails details = findDetails(unit, mod);
        TechSpecs ts = details != null ? details.getTechSpecs() : null;
        CompressorSpecs cspecs = ts != null ? ts.getCompressorSpecs() : null;
        if (cspecs == null) {
            throw new IllegalStateException("Compressor specs not configured for this unit/mode; cannot build report.");
        }

        UnitCalculationEngine.Result design = engine.compute(cspecs, unit.getCompressorQty(), ambient, evapOut);

        double capacityKw = design.capacityKw();
        double flowRate = capacityKw * 860.0 / 5000.0;
        boolean cooling = mod == Mod.COOLING;

        // --- Full load cooling table across the standard ambient range ---
        List<UnitReportModel.FullLoadRow> fullLoad = new ArrayList<>();
        for (double a : FULL_LOAD_AMBIENTS) {
            UnitCalculationEngine.Result r = engine.compute(cspecs, unit.getCompressorQty(), a, evapOut);
            fullLoad.add(UnitReportModel.FullLoadRow.builder()
                    .ambient(fmt1(a))
                    .capacity(fmt0(r.capacityKw()))
                    .power(fmt1(r.powerKw()))
                    .eerCop(fmt2(r.copEer()))
                    .build());
        }

        // --- Working envelope bounds (fall back to sensible defaults if unset) ---
        double minOut = unit.getMinWaterOutlet();
        double maxOut = unit.getMaxWaterOutlet();
        if (maxOut <= minOut) { minOut = DEFAULT_MIN_OUTLET; maxOut = DEFAULT_MAX_OUTLET; }
        double minAmb = unit.getMinAmbient();
        double maxAmb = unit.getMaxAmbient();
        if (maxAmb <= minAmb) { minAmb = DEFAULT_MIN_AMBIENT; maxAmb = DEFAULT_MAX_AMBIENT; }

        return UnitReportModel.builder()
                // Project information (project values win; otherwise fall back to the user's account)
                .projectName(project != null ? project.getName() : "")
                .responsiblePerson(user != null ? nz(user.getUsername()) : "")
                .email(user != null ? nz(user.getEmail()) : "")
                .phone(formatPhone(pick(project != null ? project.getPhone() : null, user != null ? user.getPhone() : null)))
                .country(pick(project != null ? project.getCountry() : null, user != null ? user.getCountry() : null))
                .city(pick(project != null ? project.getCity() : null, user != null ? user.getCity() : null))
                .address(pick(project != null ? project.getAddress() : null, user != null ? user.getAddress() : null))
                .printedDate(LocalDate.now().format(DATE_FMT))
                // Configuration
                .model(nz(unit.getModel()))
                .category(categoryLabel(unit.getCategory(), unit.getUnitType()))
                // Inputs
                .ambient(fmt1(ambient))
                .waterInlet(fmt1(evapIn))
                .waterOutlet(fmt1(evapOut))
                // Unit specifications
                .coolingCapacityKcalh(fmtThousands(capacityKw * 860.0))
                .coolingCapacityKw(fmt1(capacityKw))
                .inputPowerKw(fmt1(design.powerKw()))
                .eerCopLabel(cooling ? "EER" : "COP")
                .eerCopValue(fmt2(design.copEer()))
                .fullLoad(fullLoad)
                // Technical specifications
                .refrigerantCode(unit.getRefrigerant() != null ? nz(unit.getRefrigerant().getCode()) : "-")
                .compressorModel(compressorField(cspecs, true))
                .compressorBrand(compressorField(cspecs, false))
                .compressorQty(String.valueOf(unit.getCompressorQty()))
                .circuitQty(String.valueOf(unit.getCompressorQty())) // circuit qty := compressor qty
                .condenserType(condenserType(ts))
                // Hydraulic kit
                .evaporatorType(evaporatorType(ts))
                .flowRate(fmt2(flowRate))
                .waterPressure(fmt1(WATER_PRESSURE_BAR))
                .pressureDrop(fmt0(PRESSURE_DROP_KPA))
                // Fans
                .fanType("-")
                .fanQty(String.valueOf(unit.getNumberOfFans()))
                .airFlowRate(fmtThousands(unit.getAirflowRate()))
                // Pipe connection (null for now)
                .waterInletConnection("-")
                .waterOutletConnection("-")
                // Electrical
                .mra(fmt0(MRA_A))
                .lra(fmt1(LRA_A))
                // Dimensions are stored in metres and shown with 2 decimals (e.g. 3.00 m).
                .length(fmt2(unit.getLength()))
                .width(fmt2(unit.getWidth()))
                .height(fmt2(unit.getHeight()))
                // Chart raw data
                .workingLimit(UnitReportModel.WorkingLimit.builder()
                        .minWaterOutlet(minOut).maxWaterOutlet(maxOut)
                        .minAmbient(minAmb).maxAmbient(maxAmb)
                        .pointWaterOutlet(evapOut).pointAmbient(ambient)
                        .build())
                .pressureCurve(UnitReportModel.PressureCurve.builder()
                        .designFlowRate(flowRate)
                        .designPressureDrop(PRESSURE_DROP_KPA)
                        .build())
                .build();
    }

    private UnitDetails findDetails(Unit unit, Mod mod) {
        if (unit.getUnitDetails() == null) return null;
        return unit.getUnitDetails().stream()
                .filter(d -> d.getMod() == mod)
                .findFirst()
                .orElse(null);
    }

    private String compressorField(CompressorSpecs cspecs, boolean model) {
        Compressor c = cspecs.getCompressor();
        if (c == null) return "-";
        String v = model ? c.getModel() : c.getBrand();
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private String condenserType(TechSpecs ts) {
        if (ts.getCondenserSpecs() != null && ts.getCondenserSpecs().getCondenser() != null
                && ts.getCondenserSpecs().getCondenser().getType() != null) {
            return prettyEnum(ts.getCondenserSpecs().getCondenser().getType().name());
        }
        return "-";
    }

    private String evaporatorType(TechSpecs ts) {
        if (ts.getEvaporatorSpecs() != null && ts.getEvaporatorSpecs().getEvaporator() != null
                && ts.getEvaporatorSpecs().getEvaporator().getType() != null) {
            return prettyEnum(ts.getEvaporatorSpecs().getEvaporator().getType().name());
        }
        return "-";
    }

    private String categoryLabel(UnitCategory category, UnitTypeEnum type) {
        boolean air = type == UnitTypeEnum.AW;
        if (category == UnitCategory.CHILLER) {
            return air ? "Air Cooled Chiller" : "Water Cooled Chiller";
        }
        return air ? "Air to Water Heat Pump" : "Water to Water Heat Pump";
    }

    // --- formatting helpers ---

    private String prettyEnum(String raw) {
        // SHELL_AND_TUBE -> "Shell And Tube", MICROCHANNEL -> "Microchannel"
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private String fmt0(double v) { return String.format(Locale.US, "%.0f", v); }
    private String fmt1(double v) { return String.format(Locale.US, "%.1f", v); }
    private String fmt2(double v) { return String.format(Locale.US, "%.2f", v); }
    private String fmtThousands(double v) { return String.format(Locale.US, "%,.0f", v); }

    // The phone is stored as the country code + number without a leading "+"
    // (react-phone-input-2's format). Prefix "+" so the dialing code is shown on the report.
    private String formatPhone(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        return trimmed.startsWith("+") ? trimmed : "+" + trimmed;
    }

    private String nz(String s) { return s == null ? "" : s; }
    private String pick(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }
}
