package org.offitec.osp.application.report;

import org.offitec.osp.application.service.CompressorPerformanceEngine;
import org.offitec.osp.application.service.GlycolCorrection;
import org.offitec.osp.application.service.UnitCalculationEngine;
import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.AssetType;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 *   - MOC                       = compressor MOC * compressor qty
 *   - LRA                       = compressor LRA
 *   - Fan type / pipe connections = null for now ("-")
 */
@Component
public class ReportDataAssembler {

    // Standard ambient temperatures for the Full Load Cooling table.
    private static final double[] FULL_LOAD_AMBIENTS = {-10, 0, 10, 20, 30, 40};
    // Heating full-load sweep: -15 °C to 35 °C in 10 °C steps.
    private static final double[] HEATING_FULL_LOAD_AMBIENTS = {-15, -5, 5, 15, 25, 35};

    // Hardcoded constants (no DB field yet).
    private static final double WATER_PRESSURE_BAR = 1.5;
    private static final double PRESSURE_DROP_KPA = 50.0;

    // Fallback working-envelope bounds when a unit has none configured yet.
    private static final double DEFAULT_MIN_OUTLET = -5, DEFAULT_MAX_OUTLET = 25;
    private static final double DEFAULT_MIN_AMBIENT = -10, DEFAULT_MAX_AMBIENT = 48;

    // Air-to-water heat pump working envelopes (x = water outlet, y = ambient), per mode.
    // The unit stores only one envelope, so dual-mode (heat pump) reports use these fixed
    // ranges for the cooling and heating working-limit graphs.
    private static final double HP_COOL_MIN_OUTLET = -25, HP_COOL_MAX_OUTLET = 20;
    private static final double HP_COOL_MIN_AMBIENT = -5, HP_COOL_MAX_AMBIENT = 50;
    private static final double HP_HEAT_MIN_OUTLET = 10, HP_HEAT_MAX_OUTLET = 75;
    private static final double HP_HEAT_MIN_AMBIENT = -20, HP_HEAT_MAX_AMBIENT = 40;

    private final UnitCalculationEngine engine;
    private final CompressorPerformanceEngine performanceEngine;
    private final ReportMessages messages;

    public ReportDataAssembler(UnitCalculationEngine engine,
                               CompressorPerformanceEngine performanceEngine,
                               ReportMessages messages) {
        this.engine = engine;
        this.performanceEngine = performanceEngine;
        this.messages = messages;
    }

    /** One operating point's results, in the report's units (kW). */
    private record Perf(double capacityKw, double powerKw, double copEer) {}

    /** The faithful-engine operating inputs the user entered (so the PDF matches the calc page). */
    public record OpInputs(double frequencyHz, double subcooling, Double superheat, Double suctionGasTemp) {
        public static OpInputs of(Double frequencyHz, Double subcooling, Double superheat, Double suctionGasTemp) {
            return new OpInputs(frequencyHz != null ? frequencyHz : 50.0,
                    subcooling != null ? subcooling : 0.0, superheat, suctionGasTemp);
        }
    }

    private boolean useFaithful(CompressorRating rating) {
        return rating != null && performanceEngine.isAvailable() && rating.isCalculable()
                && rating.getRefrigerant() != null && rating.getRefrigerant().getCoolpropName() != null;
    }

    // Computes capacity/power/COP for one point. Uses the faithful engine when the unit's TechSpecs
    // carries an imported rating; otherwise falls back to the legacy CompressorSpecs polynomial.
    // The report is ambient-based, so the AW approach bridge is used (Cooling: Te=water-5, Tc=amb+15;
    // Heating: Te=amb-5, Tc=water+5) with the user's frequency / subcooling / superheat.
    private Perf computePerf(CompressorSpecs cspecs, CompressorRating rating, Unit unit, Mod mod,
                             double ambient, double waterTemp, OpInputs op) {
        if (useFaithful(rating)) {
            double te = mod == Mod.HEATING ? ambient - 5.0 : waterTemp - 5.0;
            double tc = mod == Mod.HEATING ? waterTemp + 5.0 : ambient + 15.0;
            CompressorPerformanceEngine.Suction suction = op.suctionGasTemp() != null
                    ? new CompressorPerformanceEngine.SuctionGasTemp(op.suctionGasTemp())
                    : new CompressorPerformanceEngine.Superheat(op.superheat() != null ? op.superheat() : 10.0);
            CompressorPerformanceEngine.Result res = performanceEngine.compute(new CompressorPerformanceEngine.Input(
                    rating, rating.getRefrigerant().getCoolpropName(),
                    "TK".equalsIgnoreCase(rating.getCompressor().getFrascoldType()),
                    te, tc, suction, op.subcooling(), op.frequencyHz(), null));
            if (res.valid()) {
                int qty = Math.max(unit.getCompressorQty(), 1);
                double capKw = res.coolingCapacityW() * qty / 1000.0;
                double powKw = res.powerInputW() * qty / 1000.0;
                return new Perf(capKw, powKw, powKw > 0 ? capKw / powKw : 0);
            }
        }
        if (cspecs == null) return new Perf(0, 0, 0);
        UnitCalculationEngine.Result r = engine.compute(cspecs, unit.getCompressorQty(), mod, ambient, waterTemp);
        return new Perf(r.capacityKw(), r.powerKw(), r.copEer());
    }

    public UnitReportModel assemble(Unit unit, Mod mod,
                                    double ambient, double evapIn, double evapOut,
                                    Project project, User user,
                                    String glycolType, Integer glycolPercentage,
                                    Locale locale) {
        return assemble(unit, mod, ambient, evapIn, evapOut, project, user,
                glycolType, glycolPercentage, locale, false, 0, 0, 0,
                OpInputs.of(null, null, null, null), OpInputs.of(null, null, null, null));
    }

    /**
     * Dual-mode overload: when {@code dualMode} is true (heat pumps), the {@code ambient/
     * evapIn/evapOut} arguments are the COOLING point and the {@code heating*} arguments
     * are the HEATING point, both rendered into one PDF.
     */
    public UnitReportModel assemble(Unit unit, Mod mod,
                                    double ambient, double evapIn, double evapOut,
                                    Project project, User user,
                                    String glycolType, Integer glycolPercentage,
                                    Locale locale,
                                    boolean dualMode, double heatingAmbient,
                                    double heatingWaterInlet, double heatingWaterOutlet,
                                    OpInputs coolingOp, OpInputs heatingOp) {

        Map<String, String> t = messages.labels(locale);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale);

        UnitDetails details = findDetails(unit, mod);
        TechSpecs ts = details != null ? details.getTechSpecs() : null;
        CompressorSpecs cspecs = ts != null ? ts.getCompressorSpecs() : null;
        CompressorRating rating = ts != null ? ts.getCompressorRating() : null;
        if (cspecs == null && rating == null) {
            throw new IllegalStateException("No compressor configured for this unit/mode; cannot build report.");
        }
        // Compressor + refrigerant metadata come from whichever selection the unit uses.
        Compressor compressor = cspecs != null ? cspecs.getCompressor()
                : (rating != null ? rating.getCompressor() : null);
        Refrigerant refrigerant = cspecs != null
                ? (compressor != null ? compressor.getRefrigerant() : null)
                : (rating != null ? rating.getRefrigerant() : null);

        Perf design = computePerf(cspecs, rating, unit, mod, ambient, evapOut, coolingOp);

        // A selected glycol mixture scales capacity, power and pressure drop.
        GlycolCorrection.Factors gf = GlycolCorrection.lookup(glycolType, glycolPercentage);

        // The "Output" block shows the values computed from THIS calculation (the entered
        // operating conditions), not the unit's stored/default specs.
        double outCapacityKw = design.capacityKw() * gf.capacity();
        double outPowerKw = design.powerKw() * gf.power();
        double outCop = outPowerKw > 0 ? outCapacityKw / outPowerKw : design.copEer();

        // Flow rate is derived from the calculated cooling capacity (the value shown in the
        // Output table): capacity(kW) * 860 / 5000, so the two stay consistent.
        double flowRate = outCapacityKw * 860.0 / 5000.0;
        boolean cooling = mod == Mod.COOLING;

        // --- Full load cooling table across the standard ambient range ---
        List<UnitReportModel.FullLoadRow> fullLoad = new ArrayList<>();
        for (double a : FULL_LOAD_AMBIENTS) {
            Perf r = computePerf(cspecs, rating, unit, mod, a, evapOut, coolingOp);
            double cap = r.capacityKw() * gf.capacity();
            double pow = r.powerKw() * gf.power();
            fullLoad.add(UnitReportModel.FullLoadRow.builder()
                    .ambient(fmt1(a))
                    .capacity(fmt4(cap))
                    .power(fmt4(pow))
                    .eerCop(fmt4(pow > 0 ? cap / pow : r.copEer()))
                    .build());
        }

        // Pressure drop (base 50 kPa) scaled by the glycol factor.
        double pressureDropKpa = PRESSURE_DROP_KPA * gf.pressureDrop();

        // --- Working envelope bounds (fall back to sensible defaults if unset) ---
        double minOut = unit.getMinWaterOutlet();
        double maxOut = unit.getMaxWaterOutlet();
        if (maxOut <= minOut) { minOut = DEFAULT_MIN_OUTLET; maxOut = DEFAULT_MAX_OUTLET; }
        double minAmb = unit.getMinAmbient();
        double maxAmb = unit.getMaxAmbient();
        if (maxAmb <= minAmb) { minAmb = DEFAULT_MIN_AMBIENT; maxAmb = DEFAULT_MAX_AMBIENT; }

        // Heat-pump reports use the fixed cooling envelope for the (cooling) working-limit graph.
        if (dualMode) {
            minOut = HP_COOL_MIN_OUTLET; maxOut = HP_COOL_MAX_OUTLET;
            minAmb = HP_COOL_MIN_AMBIENT; maxAmb = HP_COOL_MAX_AMBIENT;
        }

        // Electrical data from the compressor: MOC is per-compressor * compressor qty; LRA as-is.
        double mocPer = compressor != null && compressor.getMoc() != null ? compressor.getMoc() : 0.0;
        double lraVal = compressor != null && compressor.getLra() != null ? compressor.getLra() : 0.0;
        double totalMoc = mocPer * Math.max(unit.getCompressorQty(), 1);

        // Unit imagery: the primary image is shown next to the Configuration block and the
        // technical drawings are rendered at the end of the report (PdfReportService fetches
        // each URL and embeds it as a data URI).
        String primaryImageUrl = "";
        List<String> drawingUrls = new ArrayList<>();
        if (unit.getAssets() != null) {
            String firstImage = null;
            for (UnitAsset a : unit.getAssets()) {
                if (a.getAssetType() == AssetType.IMAGE) {
                    if (firstImage == null) firstImage = a.getUrl();
                    if (a.isPrimary()) primaryImageUrl = a.getUrl();
                } else if (a.getAssetType() == AssetType.DRAWING && a.getUrl() != null) {
                    drawingUrls.add(a.getUrl());
                }
            }
            // Fall back to any image when none is explicitly flagged primary.
            if (primaryImageUrl.isEmpty() && firstImage != null) primaryImageUrl = firstImage;
        }

        // Glycol mixture display for the Inputs table (shown on every report).
        String glycolMixture;
        if (glycolType == null || glycolType.isBlank() || glycolPercentage == null) {
            glycolMixture = t.getOrDefault("glycolNone", "None");
        } else {
            glycolMixture = glycolType + " " + glycolPercentage + "%";
        }

        // Heat-pump dual-mode: compute the HEATING operating point from its own mode's specs.
        String hAmbient = "", hWaterIn = "", hWaterOut = "";
        String hCapKcalh = "", hCapKw = "", hPowKw = "", hCop = "";
        UnitReportModel.WorkingLimit heatingWL = null;
        UnitReportModel.PressureCurve heatingPC = null;
        List<UnitReportModel.FullLoadRow> heatingFullLoad = new ArrayList<>();
        if (dualMode) {
            UnitDetails hDetails = findDetails(unit, Mod.HEATING);
            TechSpecs hts = hDetails != null ? hDetails.getTechSpecs() : null;
            CompressorSpecs hcs = hts != null ? hts.getCompressorSpecs() : null;
            CompressorRating hrating = hts != null ? hts.getCompressorRating() : null;
            if (hcs != null || hrating != null) {
                Perf hr = computePerf(hcs, hrating, unit, Mod.HEATING, heatingAmbient, heatingWaterOutlet, heatingOp);
                // Heating heats the water, so no glycol correction is applied to heating values.
                double hCap = hr.capacityKw();
                double hPow = hr.powerKw();
                double hCopVal = hPow > 0 ? hCap / hPow : hr.copEer();

                // Heating full-load table across the heating ambient range (held at the entered
                // heating water outlet), same shape as the cooling table.
                for (double a : HEATING_FULL_LOAD_AMBIENTS) {
                    Perf fr = computePerf(hcs, hrating, unit, Mod.HEATING, a, heatingWaterOutlet, heatingOp);
                    double cap = fr.capacityKw();
                    double pow = fr.powerKw();
                    heatingFullLoad.add(UnitReportModel.FullLoadRow.builder()
                            .ambient(fmt1(a))
                            .capacity(fmt4(cap))
                            .power(fmt4(pow))
                            .eerCop(fmt4(pow > 0 ? cap / pow : fr.copEer()))
                            .build());
                }
                hAmbient = fmt1(heatingAmbient);
                hWaterIn = fmt1(heatingWaterInlet);
                hWaterOut = fmt1(heatingWaterOutlet);
                hCapKcalh = fmtThousands(hCap * 860.0);
                hCapKw = fmt4(hCap);
                hPowKw = fmt4(hPow);
                hCop = fmt4(hCopVal);

                // Heating charts: reuse the unit's working envelope, plot the heating point;
                // heating flow rate is derived from the heating capacity like the cooling one.
                double hFlow = hCap * 860.0 / 5000.0;
                heatingWL = UnitReportModel.WorkingLimit.builder()
                        .minWaterOutlet(HP_HEAT_MIN_OUTLET).maxWaterOutlet(HP_HEAT_MAX_OUTLET)
                        .minAmbient(HP_HEAT_MIN_AMBIENT).maxAmbient(HP_HEAT_MAX_AMBIENT)
                        .pointWaterOutlet(heatingWaterOutlet).pointAmbient(heatingAmbient)
                        .build();
                heatingPC = UnitReportModel.PressureCurve.builder()
                        .designFlowRate(hFlow).designPressureDrop(PRESSURE_DROP_KPA)
                        .build();
            }
        }

        return UnitReportModel.builder()
                .t(t)
                .glycolMixture(glycolMixture)
                .dualMode(dualMode)
                .heatingAmbient(hAmbient)
                .heatingWaterInlet(hWaterIn)
                .heatingWaterOutlet(hWaterOut)
                .heatingCapacityKcalh(hCapKcalh)
                .heatingCapacityKw(hCapKw)
                .heatingInputPowerKw(hPowKw)
                .heatingCopValue(hCop)
                .heatingWorkingLimit(heatingWL)
                .heatingPressureCurve(heatingPC)
                .heatingFullLoad(heatingFullLoad)
                .primaryImageUrl(primaryImageUrl)
                .drawingUrls(drawingUrls)
                // Project information (project values win; otherwise fall back to the user's account)
                .projectName(project != null ? project.getName() : "")
                .responsiblePerson(user != null ? nz(user.getUsername()) : "")
                .email(user != null ? nz(user.getEmail()) : "")
                .phone(formatPhone(pick(project != null ? project.getPhone() : null, user != null ? user.getPhone() : null)))
                .country(pick(project != null ? project.getCountry() : null, user != null ? user.getCountry() : null))
                .city(pick(project != null ? project.getCity() : null, user != null ? user.getCity() : null))
                .address(pick(project != null ? project.getAddress() : null, user != null ? user.getAddress() : null))
                .printedDate(LocalDate.now().format(dateFmt))
                // Configuration
                .model(nz(unit.getModel()))
                .category(categoryLabel(unit.getCategory(), unit.getUnitType(), t))
                // Inputs
                .ambient(fmt1(ambient))
                .waterInlet(fmt1(evapIn))
                .waterOutlet(fmt1(evapOut))
                // Outputs (values computed from the entered operating conditions)
                .coolingCapacityKcalh(fmtThousands(outCapacityKw * 860.0))
                .coolingCapacityKw(fmt4(outCapacityKw))
                .inputPowerKw(fmt4(outPowerKw))
                .eerCopLabel(cooling ? "EER" : "COP")
                .eerCopValue(fmt4(outCop))
                .fullLoad(fullLoad)
                // Technical specifications
                // Refrigerant is a property of the compressor now (derived from cspecs).
                .refrigerantCode(refrigerant != null ? nz(refrigerant.getCode()) : "-")
                .compressorModel(dash(compressor != null ? compressor.getModel() : null))
                .compressorBrand(dash(compressor != null ? compressor.getBrand() : null))
                .compressorQty(String.valueOf(unit.getCompressorQty()))
                .circuitQty(String.valueOf(unit.getCompressorQty())) // circuit qty := compressor qty
                .condenserType(condenserType(ts))
                // Hydraulic kit
                .evaporatorType(evaporatorType(ts))
                .flowRate(fmt4(flowRate))
                .waterPressure(fmt1(WATER_PRESSURE_BAR))
                .pressureDrop(fmt4(pressureDropKpa))
                // Fans
                .fanType(dash(unit.getFanType()))
                .fanQty(String.valueOf(unit.getNumberOfFans()))
                // Total air flow = fan quantity * per-fan air flow rate.
                .airFlowRate(fmtThousands(Math.max(unit.getNumberOfFans(), 1) * unit.getAirflowRate()))
                // Pipe connections (stored on the unit)
                .waterInletConnection(dash(unit.getWaterInletConnection()))
                .waterOutletConnection(dash(unit.getWaterOutletConnection()))
                // Electrical (from the compressor)
                .moc(fmt4(totalMoc))
                .lra(fmt4(lraVal))
                // Dimensions are stored in millimetres.
                .length(fmt0(unit.getLength()))
                .width(fmt0(unit.getWidth()))
                .height(fmt0(unit.getHeight()))
                // Chart raw data
                .workingLimit(UnitReportModel.WorkingLimit.builder()
                        .minWaterOutlet(minOut).maxWaterOutlet(maxOut)
                        .minAmbient(minAmb).maxAmbient(maxAmb)
                        .pointWaterOutlet(evapOut).pointAmbient(ambient)
                        .build())
                .pressureCurve(UnitReportModel.PressureCurve.builder()
                        .designFlowRate(flowRate)
                        .designPressureDrop(pressureDropKpa)
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

    private String categoryLabel(UnitCategory category, UnitTypeEnum type, Map<String, String> t) {
        boolean air = type == UnitTypeEnum.AW;
        if (category == UnitCategory.CHILLER) {
            return air ? t.get("catAirCooledChiller") : t.get("catWaterCooledChiller");
        }
        return air ? t.get("catAirToWaterHeatPump") : t.get("catWaterToWaterHeatPump");
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

    // Up to 4 decimals, trailing zeros trimmed: 38.5 -> "38.5", 3.14159 -> "3.1416", 40 -> "40".
    private String fmt4(double v) {
        double r = Math.round(v * 10000.0) / 10000.0;
        if (r == Math.rint(r) && !Double.isInfinite(r)) return String.format(Locale.US, "%.0f", r);
        String s = String.format(Locale.US, "%.4f", r);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private String fmtThousands(double v) { return String.format(Locale.US, "%,.0f", v); }

    // The phone is stored as the country code + number without a leading "+"
    // (react-phone-input-2's format). Prefix "+" so the dialing code is shown on the report.
    private String formatPhone(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        return trimmed.startsWith("+") ? trimmed : "+" + trimmed;
    }

    private String nz(String s) { return s == null ? "" : s; }
    // Like nz but shows "-" for missing values (for report cells that always render a dash).
    private String dash(String s) { return (s == null || s.isBlank()) ? "-" : s; }
    private String pick(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }
}
