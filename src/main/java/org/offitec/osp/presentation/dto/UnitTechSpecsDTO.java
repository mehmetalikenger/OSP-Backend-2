package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitTechSpecsDTO {

    public double capacity;
    public double maxCapacity;

    // Legacy detailed-coefficient compressor (optional now). For AW units the admin instead selects
    // an imported Frascold rating (model + refrigerant) via compressorRatingId.
    public Long compressorSpecsId;
    // Imported Frascold rating id (model + refrigerant). When set, the faithful engine is used.
    public Long compressorRatingId;
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

    public double condenserRequiredDuty;
    public double quietCondenserRequiredDuty;
    public double fanPI;
    public double copErr;
    public double width;
    public double length;
    public double height;
    public String fanType;
    public int numberOfFans;
    public double fanDiameter;
    public double airflowRate;
    public String dischargeLineDiameter;
    public String liquidLineDiameter;
    public String suctionLineDiameter;
    public double gasTank;
    public String waterInletConnection;
    public String waterOutletConnection;

    // Working envelope for the report's Working Limit graph (safe-area bounds).
    public double minWaterInlet;
    public double maxWaterInlet;
    public double minWaterOutlet;
    public double maxWaterOutlet;
    public double minAmbient;
    public double maxAmbient;
}
