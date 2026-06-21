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
}
