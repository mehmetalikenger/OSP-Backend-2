package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public class RefrigerantDTO {
    @NotBlank(message = "Name cannot be blank.")
    private String name;

    @NotBlank(message = "Code cannot be blank.")
    private String code;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
