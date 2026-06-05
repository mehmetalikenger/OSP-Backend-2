package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserUpdateDTO {

    @NotNull(message = "ID can't be blank.")
    private Long id;

    private String username;

    @Email
    private String email;

    private String phone;
}
