package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Products-page capacity match request. The user supplies the operating conditions and a
 * target refrigerating capacity; the backend computes each candidate unit's capacity from
 * those conditions (via the unit's compressor polynomial) and returns the units whose
 * capacity falls within {@code targetCapacity ± diffPercent}.
 */
@Getter
@Setter
public class UnitMatchRequestDTO {

    @NotBlank(message = "Category can't be blank.")
    private String category;        // CHILLER / HEAT_PUMP

    @NotBlank(message = "Type can't be blank.")
    private String type;            // AW / WW

    // Optional refrigerant code filter; null/blank means "any refrigerant".
    private String refrigerant;

    private double ambient;
    private double evapIn;           // captured for context; not used by the polynomial
    private double evapOut;

    @NotNull(message = "Target capacity can't be null.")
    @Positive(message = "Target capacity must be positive.")
    private Double targetCapacity;   // kW

    // Allowed +/- band around the target capacity, as a percentage (e.g. 10 = ±10%).
    private double diffPercent;
}
