package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public class CondenserDTO {
    @NotBlank(message = "Model cannot be blank.")
    private String model;
    private String brand;
    private String type; // CondenserType enum name, e.g. "MICROCHANNEL" (optional)

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
