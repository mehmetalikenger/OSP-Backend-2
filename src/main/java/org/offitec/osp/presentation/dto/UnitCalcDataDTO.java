package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UnitCalcDataDTO {
    private String name;
    private String model;
    private String category;
    private List<CalcAssetDTO> images;
    private List<CalcAssetDTO> drawings;
    private List<CalcAssetDTO> documents;
    private List<TechSpecItemDTO> specs;
    private DefCalcValuesPublicDTO coolingDefaults;
    private DefCalcValuesPublicDTO heatingDefaults;
}
