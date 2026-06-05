package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserAddressDTO {

    @NotNull(message = "ID can't be blank.")
    private Long id;

    private String address;

    @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO code (e.g., DE).")
    private String country;

    @Size(max = 100)
    private String city;
}
