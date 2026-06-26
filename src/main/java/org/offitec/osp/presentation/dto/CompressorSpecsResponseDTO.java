package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompressorSpecsResponseDTO {

    private Long id;
    private String brand;
    private String model;
    private String type;
    private double capacity;
    private double powerInput;

    // Refrigerant selected on the compressor (for preselecting the edit form).
    private Long refrigerantId;

    // ISCR (variable-speed) only; null for other types.
    private Double rpmBase;
    private Double rpmMin;
    private Double rpmMax;

    private double qC1;
    private double qC2;
    private double qC3;
    private double qC4;
    private double qC5;
    private double qC6;
    private double qC7;
    private double qC8;
    private double qC9;
    private double qC10;

    // Second capacity curve, ISCR only (null otherwise).
    private Double qC11;
    private Double qC12;
    private Double qC13;
    private Double qC14;
    private Double qC15;
    private Double qC16;
    private Double qC17;
    private Double qC18;
    private Double qC19;
    private Double qC20;

    private double pC1;
    private double pC2;
    private double pC3;
    private double pC4;
    private double pC5;
    private double pC6;
    private double pC7;
    private double pC8;
    private double pC9;
    private double pC10;

    // Second power-input curve, ISCR only (null otherwise).
    private Double pC11;
    private Double pC12;
    private Double pC13;
    private Double pC14;
    private Double pC15;
    private Double pC16;
    private Double pC17;
    private Double pC18;
    private Double pC19;
    private Double pC20;
}
