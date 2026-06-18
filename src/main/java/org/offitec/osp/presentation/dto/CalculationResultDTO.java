package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalculationResultDTO {
    private double refrigeratingCapacity;
    private double powerInput;
    private double copEer;
    private Long customCalcValsId;
    private Long calcOutputValsId;
}
