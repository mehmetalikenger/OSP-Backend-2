package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Request body for creating a project. */
@Getter
@Setter
public class CreateProjectDTO {

    @NotBlank
    private String name;

    private String company;
    private String address;
    private String country;
    private String city;
    private String phone;
}
