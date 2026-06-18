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
}
