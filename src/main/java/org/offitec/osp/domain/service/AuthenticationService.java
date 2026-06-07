package org.offitec.osp.domain.service;

import org.offitec.osp.domain.data.UserAuthData;
import org.offitec.osp.domain.data.UserAuthResponseData;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.exception.PasswordsDontMatchException;
import org.offitec.osp.domain.exception.UserNotFoundException;
import org.offitec.osp.domain.port.AdminRepositoryPort;
import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.offitec.osp.domain.port.TokenGeneratorPort;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenGeneratorPort tokenGeneratorPort;

    public AuthenticationService(UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort, TokenGeneratorPort tokenGeneratorPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
    }

    public UserAuthResponseData authenticate(UserAuthData data){

        Optional<User> dbUser = userRepositoryPort.findByEmail(data.email());

        if(dbUser.isEmpty()){

            throw new UserNotFoundException("User not found.");
        }

        if(!passwordEncoderPort.matches(data.password(), dbUser.get().getPassword())){

            throw new PasswordsDontMatchException("Passwords don't match.");
        }

        String accessToken = tokenGeneratorPort.generateAccessToken(dbUser.get().getEmail(), dbUser.get().getRole().toString());
        String refreshToken = tokenGeneratorPort.generateRefreshToken(dbUser.get().getEmail(), dbUser.get().getRole().toString(), data.rememberMe());

      return new UserAuthResponseData(dbUser.get().getId(), dbUser.get().getRole().toString(), accessToken, refreshToken);
    }
}
