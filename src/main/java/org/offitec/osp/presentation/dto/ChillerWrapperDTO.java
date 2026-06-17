package org.offitec.osp.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChillerWrapperDTO {

    @Valid
    @NotNull(message = "Model information is required.")
    public ChillerDTO chillerDto;

    @Valid
    @NotNull(message = "Technical specifications are required.")
    public UnitTechSpecsDTO unitTechSpecsDTO;

    @Valid
    @NotNull(message = "Calculation values are required.")
    public UnitDefCalcValuesDTO unitDefCalcValuesDTO;
}
