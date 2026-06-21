package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body for updating a project's information (name + contact/address).
 * All fields are optional; a blank name is ignored so the project is never left unnamed.
 * Saving an update re-renders every stored report so it reflects the new project info.
 */
@Getter
@Setter
public class UpdateProjectDTO {

    private String name;
    private String company;
    private String address;
    private String country;
    private String city;
    private String phone;
}
