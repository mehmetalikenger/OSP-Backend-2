package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalculationResultDTO {
    private double refrigeratingCapacity;
    private double powerInput;
    private double copEer;
    private double flowRate;       // m³/h, derived from the cooling capacity
    private double pressureDrop;   // kPa, base 50 scaled by the glycol factor
    private Long customCalcValsId;
    private Long calcOutputValsId;
}
