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

    // --- Faithful-engine extras (0/false when the legacy engine is used). ---
    private double massFlow;        // kg/h, suction mass flow (× compressor qty)
    private double condenserDuty;   // kW, heat rejected at the condenser
    private double evaporatorDuty;  // kW, heat absorbed at the evaporator (source side)
    private double dischargeTemp;   // °C, compressor discharge gas temperature
    private boolean withinEnvelope; // operating point inside the compressor envelope
    private boolean faithfulEngine; // true when computed by CompressorPerformanceEngine
}
