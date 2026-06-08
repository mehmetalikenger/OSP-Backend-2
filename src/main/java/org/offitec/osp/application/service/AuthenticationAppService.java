package org.offitec.osp.application.service;

import org.offitec.osp.domain.data.UserAuthData;
import org.offitec.osp.domain.data.UserAuthResponseData;
import org.offitec.osp.domain.port.TokenGeneratorPort;
import org.offitec.osp.domain.service.AuthenticationService;
import org.offitec.osp.presentation.dto.UserAuthDTO;
import org.offitec.osp.presentation.dto.UserAuthResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationAppService {

    private final AuthenticationService authenticationService;
    private final TokenGeneratorPort tokenGeneratorPort;

    public AuthenticationAppService(AuthenticationService authenticationService, TokenGeneratorPort tokenGeneratorPort) {
        this.authenticationService = authenticationService;
        this.tokenGeneratorPort = tokenGeneratorPort;
    }

    public UserAuthResponseDTO authenticate(UserAuthDTO dto){

        UserAuthData authData = new UserAuthData(dto.getEmail(), dto.getPassword(), dto.isRememberMe());

        UserAuthResponseData authResponseData = authenticationService.authenticate(authData);

        UserAuthResponseDTO responseDTO = new UserAuthResponseDTO();

        responseDTO.setId(authResponseData.id());
        responseDTO.setRole(authResponseData.role());
        responseDTO.setAccessToken(authResponseData.accessToken());
        responseDTO.setRefreshToken(authResponseData.refreshToken());

        return responseDTO;
    }

    
    public UserAuthResponseData generateAccessToken(){

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        assert authentication != null;
        String email = (String) authentication.getPrincipal();
        Boolean rememberMe = (boolean) authentication.getCredentials();
        String role = authentication.getAuthorities().isEmpty() ? null : authentication.getAuthorities().iterator().next().getAuthority();

        return new UserAuthResponseData(null, role, tokenGeneratorPort.generateAccessToken(email, role),
                tokenGeneratorPort.generateRefreshToken(email, role, rememberMe));
    }
}
