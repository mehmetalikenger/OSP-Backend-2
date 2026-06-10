package org.offitec.osp.presentation.controller;

import org.offitec.osp.application.service.AccountAppService;
import org.offitec.osp.presentation.dto.AccountActivationDTO;
import org.offitec.osp.presentation.dto.ForgotPasswordRequestDTO;
import org.offitec.osp.presentation.dto.ResetPasswordDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountAppService accountAppService;

    public AccountController(AccountAppService accountAppService) {
        this.accountAppService = accountAppService;
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@Valid @RequestBody AccountActivationDTO dto) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            accountAppService.activateAccount(email, dto.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason()));
        }
    }

    @PostMapping("/forgot-password-request")
    public ResponseEntity<?> forgotPasswordRequest(@RequestBody ForgotPasswordRequestDTO dto) {
        try {
            accountAppService.requestForgotPassword(dto.getEmail());
            return ResponseEntity.ok().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            accountAppService.resetPassword(email, dto.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason()));
        }
    }

    @PostMapping("/delete-account-request")
    public ResponseEntity<Void> deleteAccountRequest() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        accountAppService.requestAccountDeletion(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        accountAppService.deleteAccount(email);
        return ResponseEntity.ok().build();
    }
}
