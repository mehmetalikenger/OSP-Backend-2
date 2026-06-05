package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserPasswordDTO {

    @NotNull(message = "ID can't be blank.")
    private Long id;

    @NotBlank(message = "Password can't be blank.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#%^&*._+])(?!.*\\$).{8,}$", message = "Password should be minimum 8 characters long. " +
            "Password should contain at least one lowercase, " +
            "one uppercase, one number and one special character. " +
            "Allowed special characters are (!@#%^&*._+)")
    private String password;
}
