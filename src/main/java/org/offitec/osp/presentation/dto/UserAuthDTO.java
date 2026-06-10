package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserAuthDTO {

    @NotBlank(message = "Email can't be blank.")
    private String email;

    @NotBlank(message = "Password can't be blank.")
    private String password;

    private boolean rememberMe;
}

