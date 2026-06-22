package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculationRequestDTO {

    @NotNull(message = "Unit ID can't be null.")
    private Long unitId;

    @NotNull(message = "Mod can't be null.")
    private String mod;

    private double ambient;
    private double evapIn;
    private double evapOut;
    private double condIn;
    private double condOut;

    // Optional glycol mixture correction. type e.g. "Ethylene Glycol" / "Propylene Glycol",
    // percentage 5..50. When set, capacity/power are scaled by the correction factors.
    private String glycolType;
    private Integer glycolPercentage;
}
