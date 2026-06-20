package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// Technical attributes shared across all modes of a unit (stored on the Unit).
@Getter
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
}
