package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class HeatPumpDTO {

    @NotBlank(message = "Unit model can't be blank.")
    public String model;

    @NotBlank(message = "Unit type can't be blank.")
    public String type;
}
