package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DefCalcValuesPublicDTO {
    private double ambient;
    private double condensation;
    private double evaporation;
    private double subcooling;
    private double superheat;
    private double evapIn;
    private double evapOut;
    private double condIn;
    private double condOut;
}
