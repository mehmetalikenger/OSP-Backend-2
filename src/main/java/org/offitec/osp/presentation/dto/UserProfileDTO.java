package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String city;
    private String surname;
    private org.offitec.osp.domain.enums.UserCategory category;
    private java.time.LocalDateTime createdAt;
    private org.offitec.osp.domain.enums.UserStatus status;
}
