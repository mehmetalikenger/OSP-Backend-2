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
}
