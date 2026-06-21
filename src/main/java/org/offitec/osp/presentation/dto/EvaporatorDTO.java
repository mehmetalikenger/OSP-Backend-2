package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public class EvaporatorDTO {

    @NotBlank(message = "Model cannot be blank.")
    private String model;

    private String brand;

    private String type; // EvaporatorType enum name: PLATE / COIL / SHELL_AND_TUBE (optional)

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
