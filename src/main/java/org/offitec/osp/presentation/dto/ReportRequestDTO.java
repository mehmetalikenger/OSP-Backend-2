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

    // Optional glycol mixture correction (see GlycolCorrection).
    private String glycolType;
    private Integer glycolPercentage;

    // Report language: "en" (default) or "de". Drives all labels, the localized charts,
    // category name, and date format on the generated PDF.
    private String language = "en";
}
