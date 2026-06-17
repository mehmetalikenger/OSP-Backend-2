package org.offitec.osp.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// Heat pump shell creation: model/type + the common (shared-across-modes) tech details.
@Getter
public class HeatPumpModelWrapperDTO {

    @Valid
    @NotNull(message = "Model information is required.")
    public HeatPumpDTO heatPumpDto;

    @Valid
    @NotNull(message = "Common technical specifications are required.")
    public UnitCommonSpecsDTO commonSpecsDto;
}
