package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;

// One operating mode of a heat pump: per-mode tech + calc values.
@Getter
@Setter
public class HeatPumpModeDTO {

    private String mod;

    private double capacity;
    private double copErr;
    private double condenserRequiredDuty;
    private double quietCondenserRequiredDuty;

    private Long compressorSpecsId;
    private Long condenserSpecsId;
    private Long evaporatorSpecsId;
    private Long expansionValveSpecsId;
    private Long fourWayReversingValveSpecsId;
    private Long chassisId;

    private double ambient;
    private double evapIn;
    private double evapOut;
    private double condIn;
    private double condOut;
}
