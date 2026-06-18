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

    @NotNull(message = "Q-C1 can't be null.") private double qC1;
    @NotNull(message = "Q-C2 can't be null.") private double qC2;
    @NotNull(message = "Q-C3 can't be null.") private double qC3;
    @NotNull(message = "Q-C4 can't be null.") private double qC4;
    @NotNull(message = "Q-C5 can't be null.") private double qC5;
    @NotNull(message = "Q-C6 can't be null.") private double qC6;
    @NotNull(message = "Q-C7 can't be null.") private double qC7;
    @NotNull(message = "Q-C8 can't be null.") private double qC8;
    @NotNull(message = "Q-C9 can't be null.") private double qC9;
    @NotNull(message = "Q-C10 can't be null.") private double qC10;

    @NotNull(message = "P-C1 can't be null.") private double pC1;
    @NotNull(message = "P-C2 can't be null.") private double pC2;
    @NotNull(message = "P-C3 can't be null.") private double pC3;
    @NotNull(message = "P-C4 can't be null.") private double pC4;
    @NotNull(message = "P-C5 can't be null.") private double pC5;
    @NotNull(message = "P-C6 can't be null.") private double pC6;
    @NotNull(message = "P-C7 can't be null.") private double pC7;
    @NotNull(message = "P-C8 can't be null.") private double pC8;
    @NotNull(message = "P-C9 can't be null.") private double pC9;
    @NotNull(message = "P-C10 can't be null.") private double pC10;
}
