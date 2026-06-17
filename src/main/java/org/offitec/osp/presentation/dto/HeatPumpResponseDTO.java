package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Full heat pump: shell + common tech (for the model edit page) + its modes (for the mod edit page).
@Getter
@Setter
public class HeatPumpResponseDTO {

    private List<UnitAssetDTO> assets;

    private Long id;
    private String model;
    private String name;
    private String description;
    private String type; // AW / WW

    // common (unit-level)
    private int compressorQty;
    private int condenserQty;
    private int expansionValveQty;
    private Long refrigerantId;
    private double fanPI;
    private double width;
    private double length;
    private double height;
    private int numberOfFans;
    private double fanDiameter;
    private double airflowRate;
    private String dischargeLineDiameter;
    private String liquidLineDiameter;
    private String suctionLineDiameter;
    private double gasTank;

    private List<HeatPumpModeDTO> modes;
}
