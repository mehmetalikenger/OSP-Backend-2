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

    // Unit-level compressor (model + refrigerant) shared by both heat-pump modes, picked on the model form.
    public Long compressorRatingId;

    @NotNull(message = "Chassis must be selected.")
    public Long chassisId;

    public double fanPI;
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
