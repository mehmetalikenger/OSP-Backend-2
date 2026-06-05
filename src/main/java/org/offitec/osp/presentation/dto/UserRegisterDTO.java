package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserRegisterDTO {

    @NotBlank(message = "Email can't be blank.")
    @Email
    private String email;
}
