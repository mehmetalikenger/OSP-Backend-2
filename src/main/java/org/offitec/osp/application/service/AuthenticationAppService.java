package org.offitec.osp.application.service;

import org.offitec.osp.domain.data.UserAuthData;
import org.offitec.osp.domain.data.UserAuthResponseData;
import org.offitec.osp.domain.service.AuthenticationService;
import org.offitec.osp.presentation.dto.UserAuthDTO;
import org.offitec.osp.presentation.dto.UserAuthResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationAppService {

    private final AuthenticationService authenticationService;

    public AuthenticationAppService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public UserAuthResponseDTO authenticate(UserAuthDTO dto){

        boolean shouldRemember = Boolean.TRUE.equals(dto.getRememberMe());

        UserAuthData authData = new UserAuthData(dto.getEmail(), dto.getPassword(), shouldRemember);

        UserAuthResponseData authResponseData = authenticationService.authenticate(authData);

        UserAuthResponseDTO responseDTO = new UserAuthResponseDTO();

        responseDTO.setId(authResponseData.id());
        responseDTO.setRole(authResponseData.role());
        responseDTO.setAccessToken(authResponseData.accessToken());
        responseDTO.setRefreshToken(authResponseData.refreshToken());

        return responseDTO;
    }
}
