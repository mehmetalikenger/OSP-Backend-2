package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAuthResponseDTO {

    private Long id;
    private String role;
    private String accessToken;
    private String refreshToken;
}
