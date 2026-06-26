package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChillerResponseDTO {

    private List<UnitAssetDTO> assets;

    private Long id;
    private String model;
    private String name;
    private String description;
    private String type; // AW / WW
    private String mod;  // COOLING / HEATING

    // Calculation values
    private double ambient;
    private double evapIn;
    private double evapOut;
    private double condIn;
    private double condOut;

    // Technical specifications. Component selections are exposed as their *specs* ids
    // so the frontend can preselect the matching dropdown option.
    private double capacity;
    private double maxCapacity;
    private Long compressorSpecsId;
    // Imported Frascold rating (model + refrigerant) the unit is assigned to, for the new picker.
    private Long compressorRatingId;
    private int compressorQty;
    private Long condenserSpecsId;
    private int condenserQty;
    private Long expansionValveSpecsId;
    private int expansionValveQty;
    private Long evaporatorSpecsId;
    private Long chassisId;
    private Long fourWayReversingValveSpecsId;
    private double condenserRequiredDuty;
    private double quietCondenserRequiredDuty;
    private double fanPI;
    private double copErr;
    private double width;
    private double length;
    private double height;
    private String fanType;
    private int numberOfFans;
    private double fanDiameter;
    private double airflowRate;
    private String dischargeLineDiameter;
    private String liquidLineDiameter;
    private String suctionLineDiameter;
    private double gasTank;
    private String waterInletConnection;
    private String waterOutletConnection;

    // Working envelope (safe-area bounds for the report's Working Limit graph).
    private double minWaterInlet;
    private double maxWaterInlet;
    private double minWaterOutlet;
    private double maxWaterOutlet;
    private double minAmbient;
    private double maxAmbient;
}
