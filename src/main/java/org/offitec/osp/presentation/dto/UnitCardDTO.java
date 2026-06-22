package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
    // Icon (feature) image URLs, filled by the service from a batched query.
    private List<String> iconUrls = new ArrayList<>();

    // Constructor used by the catalog projection query (UnitJpaRepository.findCards /
    // findSavedCards). It takes the enums straight from the entity and stores them as
    // strings (the shape the frontend already expects). `capacityRange` is passed in
    // empty and filled afterwards by the service, because capacity is multi-valued per
    // unit (one per mode) and can't come from this single flat row.
    public UnitCardDTO(Long id, String name, String model, String primaryImageUrl,
                       String capacityRange, String refrigerant,
                       UnitTypeEnum unitType, UnitCategory category, boolean saved) {
        this.id = id;
        this.name = name;
        this.model = model;
        this.primaryImageUrl = primaryImageUrl;
        this.capacityRange = capacityRange;
        this.refrigerant = refrigerant;
        this.unitType = unitType != null ? unitType.name() : null;
        this.category = category != null ? category.name() : null;
        this.saved = saved;
    }
}
