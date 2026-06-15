package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class EvaporatorSpecsDTO {

    private Long evaporatorId;

    @NotNull(message = "Capacity cannot be null.")
    @Min(value = 0, message = "Capacity must be greater than or equal to 0.")
    private Double capacity;

    public Long getEvaporatorId() {
        return evaporatorId;
    }

    public void setEvaporatorId(Long evaporatorId) {
        this.evaporatorId = evaporatorId;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }
}
