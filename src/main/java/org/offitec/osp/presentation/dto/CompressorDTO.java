package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompressorDTO {

    @NotBlank(message = "Brand name can't be empty.")
    private String brand;

    @NotBlank(message = "Model can't be empty.")
    private String model;

    @NotBlank(message = "Compressor type can't be empty.")
    @Pattern(regexp = "^(?i)(RC|SC|SCR|ISCR)$", message = "Invalid compressor type.")
    private String type;

    // Maximum Operating Current and Locked Rotor Amperage (A).
    private double moc;
    private double lra;

    // Refrigerant is selected on the compressor (after LRA). Optional.
    private Long refrigerantId;
}
