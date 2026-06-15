package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CondenserSpecsDTO {
    private Long condenserId;

    @NotNull(message = "Capacity cannot be null.")
    @Min(value = 0, message = "Capacity must be greater than or equal to 0.")
    private Double capacity;

    public Long getCondenserId() { return condenserId; }
    public void setCondenserId(Long condenserId) { this.condenserId = condenserId; }
    public Double getCapacity() { return capacity; }
    public void setCapacity(Double capacity) { this.capacity = capacity; }
}
