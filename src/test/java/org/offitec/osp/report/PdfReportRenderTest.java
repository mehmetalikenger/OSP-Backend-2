package org.offitec.osp.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.offitec.osp.application.report.PdfReportService;
import org.offitec.osp.application.report.UnitReportModel;
import org.offitec.osp.application.report.chart.PressureDropSvgBuilder;
import org.offitec.osp.application.report.chart.WorkingLimitSvgBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders a sample report straight to target/sample-report.pdf with no Spring
 * context or database, to verify the full HTML+SVG -> PDF pipeline works.
 */
class PdfReportRenderTest {

    @Test
    void rendersSamplePdf() throws Exception {
        PdfReportService service = new PdfReportService(
                new WorkingLimitSvgBuilder(), new PressureDropSvgBuilder());

        UnitReportModel model = baseModel()
                .projectName("Istanbul Data Center")
                .responsiblePerson("Mehmet Yilmaz")
                .country("Turkey").city("Istanbul").address("Mall of Istanbul, Basaksehir")
                .build();

        byte[] pdf = service.render(model);

        Path out = Path.of("target/sample-report.pdf");
        Files.write(out, pdf);

        assertTrue(pdf.length > 1000, "PDF should be non-trivial in size");
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F',
                "Output should start with the %PDF magic header");
        System.out.println("Wrote sample PDF: " + out.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }

    @Test
    void rendersTurkishCharacters() throws Exception {
        PdfReportService service = new PdfReportService(
                new WorkingLimitSvgBuilder(), new PressureDropSvgBuilder());

        UnitReportModel model = baseModel()
                .projectName("İzmir Soğutma Projesi")
                .responsiblePerson("Şükrü Çağlayan")
                .city("İzmir").country("Türkiye")
                .address("Çiğli, İzmir")
                .build();

        byte[] pdf = service.render(model);
        Files.write(Path.of("target/turkish-report.pdf"), pdf);

        try (PDDocument doc = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            System.out.println("EXTRACTED >>>\n" + text + "\n<<< END");
            assertTrue(text.contains("İzmir"), "Turkish 'İzmir' should appear; got: " + text);
            assertTrue(text.contains("Türkiye"), "Turkish 'Türkiye' should appear; got: " + text);
        }
    }

    @Test
    void rendersDualModeHeatPumpPdf() throws Exception {
        PdfReportService service = new PdfReportService(
                new WorkingLimitSvgBuilder(), new PressureDropSvgBuilder());

        // Dual-mode (heat pump) report: cooling block from baseModel + a heating block.
        UnitReportModel model = baseModel()
                .category("Air to Water Heat Pump")
                .projectName("Heat Pump Project")
                .dualMode(true)
                .heatingAmbient("7.0").heatingWaterInlet("40.0").heatingWaterOutlet("45.0")
                .heatingCapacityKcalh("103,200").heatingCapacityKw("120.0")
                .heatingInputPowerKw("34.0").heatingCopValue("3.53")
                .heatingFullLoad(List.of(
                        row("-15.0", "95", "30.1", "3.16"),
                        row("-5.0", "108", "31.7", "3.41"),
                        row("5.0", "120", "33.2", "3.61"),
                        row("15.0", "131", "34.8", "3.76"),
                        row("25.0", "140", "36.0", "3.89"),
                        row("35.0", "150", "37.4", "4.01")))
                .heatingWorkingLimit(UnitReportModel.WorkingLimit.builder()
                        .minWaterOutlet(0).maxWaterOutlet(25)
                        .minAmbient(-10).maxAmbient(52)
                        .pointWaterOutlet(45).pointAmbient(7).build())
                .heatingPressureCurve(UnitReportModel.PressureCurve.builder()
                        .designFlowRate(20.64).designPressureDrop(50).build())
                .build();

        byte[] pdf = service.render(model);
        Files.write(Path.of("target/heatpump-report.pdf"), pdf);

        assertTrue(pdf.length > 1000, "Dual-mode PDF should be non-trivial in size");
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F',
                "Output should start with the %PDF magic header");
    }

    // Shared technical model; tests set the project-specific (locale-sensitive) fields.
    private static UnitReportModel.UnitReportModelBuilder baseModel() {
        return UnitReportModel.builder()
                .t(new org.offitec.osp.application.report.ReportMessages().labels(java.util.Locale.ENGLISH))
                .email("sametoffitec2026@gmail.com")
                .phone("+90 216 642 70 42")
                .printedDate("Saturday, 21 June 2026")
                .model("EAS 1102 - V0 - Standard Version")
                .category("Air Cooled Chiller")
                .ambient("35.0").waterInlet("12.0").waterOutlet("7.0")
                .coolingCapacityKcalh("91,295").coolingCapacityKw("106.2")
                .inputPowerKw("38.0").eerCopLabel("EER").eerCopValue("2.80")
                .fullLoad(List.of(
                        row("-10.0", "142", "19.1", "7.40"),
                        row("0.0", "142", "20.7", "6.83"),
                        row("10.0", "142", "22.3", "6.34"),
                        row("20.0", "129", "28.2", "4.57"),
                        row("30.0", "114", "34.4", "3.31"),
                        row("40.0", "98", "41.9", "2.34")))
                .refrigerantCode("R410A")
                .compressorModel("Hermetic Scroll").compressorBrand("DANFOSS")
                .compressorQty("2").circuitQty("2").condenserType("Microchannel")
                .evaporatorType("Shell And Tube").flowRate("18.26")
                .waterPressure("1.5").pressureDrop("50")
                .fanType("-").fanQty("2").airFlowRate("40,000")
                .waterInletConnection("-").waterOutletConnection("-")
                .moc("480").lra("135.3")
                .length("3.00").width("1.20").height("2.30")
                .workingLimit(UnitReportModel.WorkingLimit.builder()
                        .minWaterOutlet(0).maxWaterOutlet(25)
                        .minAmbient(-10).maxAmbient(52)
                        .pointWaterOutlet(7).pointAmbient(35).build())
                .pressureCurve(UnitReportModel.PressureCurve.builder()
                        .designFlowRate(18.26).designPressureDrop(50).build());
    }

    private static UnitReportModel.FullLoadRow row(String a, String cap, String pow, String eer) {
        return UnitReportModel.FullLoadRow.builder()
                .ambient(a).capacity(cap).power(pow).eerCop(eer).build();
    }
}
