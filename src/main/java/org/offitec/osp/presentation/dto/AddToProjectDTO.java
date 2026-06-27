package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for adding a unit-calculation to a project.
 * Carries the same inputs the user entered for the calculation; the backend
 * recomputes and persists the custom inputs + outputs as a ProjectDetails row.
 */
@Getter
@Setter
public class AddToProjectDTO {

    @NotNull
    private Long unitId;

    private String mod = "COOLING";
    private double ambient;
    private double evapIn;
    private double evapOut;
    private double condIn;
    private double condOut;

    // Faithful-engine operating inputs for the COOLING point — must match what the calc page sent
    // so the persisted outputs and PDF reproduce the same numbers. Optional (defaults 50 / 0 / 10).
    private Double frequencyHz;
    private Double subcooling;
    private Double superheat;
    private Double suctionGasTemp;

    // Heat pumps store both modes under one ProjectDetails and render a single dual-mode PDF.
    // When dualMode is true the inputs above are the COOLING point and these are the HEATING point.
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

    // Language of the stored report PDF: "en" (default) or "de". Persisted on the
    // ProjectDetails row so the report regenerates in the same language later.
    private String language = "en";
}
