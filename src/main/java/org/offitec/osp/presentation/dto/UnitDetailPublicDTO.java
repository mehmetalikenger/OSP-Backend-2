package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UnitDetailPublicDTO {
    private Long id;
    private String name;
    private String model;
    private String description;
    private String primaryImageUrl;
    private List<String> iconUrls;
    private List<TechSpecItemDTO> specs;
    private String unitType;
    private String category;
    private boolean saved;
}
