package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Technical attributes shared across all modes of a unit (stored on the Unit).
@Getter
@Setter
public class UnitCommonSpecsDTO {

    public int compressorQty;
    public int condenserQty;
    public int expansionValveQty;

    @NotNull(message = "Refrigerant must be selected.")
    public Long refrigerantId;

    @NotNull(message = "Chassis must be selected.")
    public Long chassisId;

    public double fanPI;
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

    // Working envelope for the report's Working Limit graph (safe-area bounds).
    public double minWaterInlet;
    public double maxWaterInlet;
    public double minWaterOutlet;
    public double maxWaterOutlet;
    public double minAmbient;
    public double maxAmbient;
}
