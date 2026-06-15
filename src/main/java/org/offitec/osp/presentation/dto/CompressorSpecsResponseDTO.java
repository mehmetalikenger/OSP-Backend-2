package org.offitec.osp.presentation.dto;

public class CompressorSpecsResponseDTO {

    private Long id;
    private String brand;
    private String model;
    private String type;
    private double capacity;
    private double powerInput;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getPowerInput() {
        return powerInput;
    }

    public void setPowerInput(double powerInput) {
        this.powerInput = powerInput;
    }
}
