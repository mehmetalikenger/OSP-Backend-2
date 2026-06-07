package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.AuthenticationAppService;
import org.offitec.osp.presentation.dto.UserAuthDTO;
import org.offitec.osp.presentation.dto.UserAuthResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    private final AuthenticationAppService authenticationAppService;

    private final String sameSite;

    private final int accessTokenMaxAge;

    private final int refreshTokenMaxAge;

    public AuthenticationController(AuthenticationAppService authenticationAppService,
                              @Value("${spring.security.jwt.same-site}") String sameSite,
                              @Value("${spring.security.jwt.access-token-exp-time}") int accessTokenMaxAge,
                              @Value("${spring.security.jwt.refresh-token-exp-time}") int refreshTokenMaxAge){

        this.authenticationAppService = authenticationAppService;
        this.sameSite = sameSite;
        this.accessTokenMaxAge = accessTokenMaxAge;
        this.refreshTokenMaxAge = refreshTokenMaxAge;
    }

    @PostMapping("/auth")
    public ResponseEntity<UserAuthResponseDTO> auth(@RequestBody @Valid UserAuthDTO dto){

        UserAuthResponseDTO responseDTO = authenticationAppService.authenticate(dto);

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", responseDTO.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite(sameSite)
                .path("/")
                .maxAge(accessTokenMaxAge)
                .build();

        ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from("refreshToken", responseDTO.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite(sameSite)
                .path("/");

        if(dto.isRememberMe()){
            refreshCookieBuilder.maxAge(refreshTokenMaxAge);
        }

        ResponseCookie refreshCookie = refreshCookieBuilder.build();
        
        // Null out tokens in body to avoid exposing them since we set them in HttpOnly cookies
        responseDTO.setAccessToken(null);
        responseDTO.setRefreshToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseDTO);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }
}
