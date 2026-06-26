package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for generating a unit selection PDF report.
 * Mirrors the calculation inputs; projectId is optional (set when the report is
 * generated for a unit that has been added to a project).
 */
@Getter
@Setter
public class ReportRequestDTO {
    private String mod = "COOLING";   // COOLING / HEATING
    private double ambient;
    private double evapIn;
    private double evapOut;
    private Long projectId;           // optional

    // Faithful-engine operating inputs for the COOLING point — must match what the calc page sent
    // so the PDF reproduces the same numbers. All optional (defaults 50 Hz / 0 K SC / 10 K SH).
    private Double frequencyHz;
    private Double subcooling;
    private Double superheat;
    private Double suctionGasTemp;

    // Heat pumps render a single PDF covering both modes. When dualMode is true the
    // primary inputs above are the COOLING point and these are the HEATING point.
    private boolean dualMode;
    private double heatingAmbient;
    private double heatingWaterInlet;
    private double heatingWaterOutlet;
    private Double heatingFrequencyHz;
    private Double heatingSubcooling;
    private Double heatingSuperheat;
    private Double heatingSuctionGasTemp;

    // Optional glycol mixture correction (see GlycolCorrection).
    private String glycolType;
    private Integer glycolPercentage;

    // Report language: "en" (default) or "de". Drives all labels, the localized charts,
    // category name, and date format on the generated PDF.
    private String language = "en";
}
