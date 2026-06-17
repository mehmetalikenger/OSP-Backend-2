package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnitCardDTO {
    private Long id;
    private String name;
    private String model;
    private String primaryImageUrl;
    private String capacityRange;
    private String refrigerant;
    private String unitType;
    private String category;
    private boolean saved;
}
