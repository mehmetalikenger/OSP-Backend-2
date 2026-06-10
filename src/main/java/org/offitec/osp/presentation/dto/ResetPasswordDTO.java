package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResetPasswordDTO {
    
    @NotBlank(message = "Password can't be blank.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#%^&*._+])(?!.*\\$).{8,}$", message = "Password should be minimum 8 characters long. " +
            "Password should contain at least one lowercase, " +
            "one uppercase, one number and one special character. " +
            "Allowed special characters are (!@#%^&*._+)")
    private String newPassword;

    public ResetPasswordDTO() {}

    public ResetPasswordDTO(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
