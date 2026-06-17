package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitTechSpecsDTO {

    public double capacity;

    @NotNull(message = "Compressor must be selected.")
    public Long compressorSpecsId;
    public int compressorQty;

    @NotNull(message = "Condenser must be selected.")
    public Long condenserSpecsId;
    public int condenserQty;

    @NotNull(message = "Expansion valve must be selected.")
    public Long expansionValveSpecsId;
    public int expansionValveQty;

    @NotNull(message = "Evaporator must be selected.")
    public Long evaporatorSpecsId;

    @NotNull(message = "Chassis must be selected.")
    public Long chassisId;

    // Optional for chillers (used by heat pumps in heating mode)
    public Long fourWayReversingValveSpecsId;

    @NotNull(message = "Refrigerant must be selected.")
    public Long refrigerantId;

    public double condenserRequiredDuty;
    public double quietCondenserRequiredDuty;
    public double fanPI;
    public double copErr;
    public double width;
    public double length;
    public double height;
    public int numberOfFans;
    public double fanDiameter;
    public double airflowRate;
    public String dischargeLineDiameter;
    public String liquidLineDiameter;
    public String suctionLineDiameter;
    public double gasTank;
}
