package org.offitec.osp.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// Adds one operating mode's details (per-mode tech + calc values) to an existing heat pump.
@Getter
public class HeatPumpDetailsWrapperDTO {

    @NotNull(message = "Heat pump must be selected.")
    public Long heatPumpId;

    @NotBlank(message = "Mod can't be blank.")
    public String mod;

    @Valid
    @NotNull(message = "Technical specifications are required.")
    public UnitModeSpecsDTO modeSpecsDto;

    @Valid
    @NotNull(message = "Calculation values are required.")
    public UnitDefCalcValuesDTO unitDefCalcValuesDTO;
}
