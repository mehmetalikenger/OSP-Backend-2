package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.enums.UserStatus;
import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.offitec.osp.infrastructure.mail.MailService;
import org.offitec.osp.infrastructure.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Service
public class AccountAppService {

    private final UserRepositoryPort userRepositoryPort;
    private final JwtService jwtService;
    private final MailService mailService;
    private final PasswordEncoderPort passwordEncoderPort;

    public AccountAppService(UserRepositoryPort userRepositoryPort, JwtService jwtService, MailService mailService, PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public void activateAccount(String email, String newPassword) {
        User user = userRepositoryPort.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already activated");
        }
        user.setPassword(passwordEncoderPort.encode(newPassword));
        user.setStatus(UserStatus.ACTIVE);
        userRepositoryPort.save(user);
    }

    public void requestForgotPassword(String email) {
        User user = userRepositoryPort.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found");
        }
        try {
            String token = jwtService.generateResetPasswordToken(email, Integer.parseInt(jwtService.getResetPasswordTokenExpTime()));
            mailService.sendForgotPasswordEmail(email, token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send reset password email", e);
        }
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepositoryPort.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getPassword() != null && passwordEncoderPort.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as the current password");
        }
        user.setPassword(passwordEncoderPort.encode(newPassword));
        user.setPasswordUpdateDate(LocalDateTime.now());
        userRepositoryPort.save(user);
    }

    public void requestAccountDeletion(String email) {
        User user = userRepositoryPort.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        try {
            String token = jwtService.generateDeleteAccountToken(email, Integer.parseInt(jwtService.getAccountDeletionTokenExpTime()));
            mailService.sendAccountDeletionConfirmationEmail(email, token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send account deletion email", e);
        }
    }

    public void deleteAccount(String email) {
        User user = userRepositoryPort.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already deleted");
        }
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(user.getId());
        userRepositoryPort.save(user);
    }
}
