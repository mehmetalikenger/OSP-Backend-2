package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompressorSpecsDTO {

    @NotNull(message = "Compressor Id can't be null.")
    private Long compressorId;

    @NotNull(message = "Capacity can't be null.")
    private double capacity;

    @NotNull(message = "Power input can't be null.")
    private double powerInput;
}
