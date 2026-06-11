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

        User user = dbUser.get();
        boolean reactivated = false;

        if (user.getDeletedAt() != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(user.getDeletedAt(), java.time.LocalDateTime.now());
            
            if (user.getDeletedBy() != null) {
                if (user.getDeletedBy().equals(user.getId()) && user.getRole() != org.offitec.osp.domain.enums.UserRole.ADMIN) {
                    // Regular user self-deletion, allow recovery
                } else {
                    // Admin deleted this account (either themselves or someone else)
                    throw new UserNotFoundException("This account is deleted by an admin.");
                }
            }

            if (daysBetween > 30) {
                throw new UserNotFoundException("This account is deleted");
            } else {
                user.setDeletedAt(null);
                user.setDeletedBy(null);
                user.setStatus(org.offitec.osp.domain.enums.UserStatus.ACTIVE);
                userRepositoryPort.save(user);
                reactivated = true;
            }
        }

        if(!passwordEncoderPort.matches(data.password(), user.getPassword())){

            throw new PasswordsDontMatchException("Passwords don't match.");
        }

        String accessToken = tokenGeneratorPort.generateAccessToken(user.getEmail(), user.getRole().toString());
        String refreshToken = tokenGeneratorPort.generateRefreshToken(user.getEmail(), user.getRole().toString(), data.rememberMe());

      return new UserAuthResponseData(user.getId(), user.getRole().toString(), accessToken, refreshToken, reactivated);
    }
}
