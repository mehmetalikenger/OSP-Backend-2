package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateDTO extends UserUpdateDTO {

    @NotBlank(message = "Surname can't be blank.")
    private String surname;

    private org.offitec.osp.domain.enums.UserStatus status;
}
