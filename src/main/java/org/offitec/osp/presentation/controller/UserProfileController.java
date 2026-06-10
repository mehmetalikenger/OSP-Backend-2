package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.UserProfileAppService;
import org.offitec.osp.presentation.dto.AdminUpdateDTO;
import org.offitec.osp.presentation.dto.UserAddressDTO;
import org.offitec.osp.presentation.dto.UserPasswordDTO;
import org.offitec.osp.presentation.dto.UserUpdateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final UserProfileAppService userProfileAppService;

    public UserProfileController(UserProfileAppService userProfileAppService){
        this.userProfileAppService = userProfileAppService;
    }

    @PostMapping("/update-user")
    public HttpStatus updateUser(@Valid @RequestBody UserUpdateDTO dto){

        userProfileAppService.updateUser(dto);

        return HttpStatus.OK;
    }

    @PostMapping("/update-admin")
    public HttpStatus updateAdmin(@Valid @RequestBody AdminUpdateDTO dto){

        userProfileAppService.updateAdmin(dto);

        return HttpStatus.OK;
    }

    @PostMapping("/update-adress")
    public HttpStatus updateUserAddress(@Valid @RequestBody UserAddressDTO dto){

        userProfileAppService.updateUserAddress(dto);

        return HttpStatus.OK;
    }

    @PostMapping("/update-password")
    public org.springframework.http.ResponseEntity<?> updateUserPassword(@Valid @RequestBody UserPasswordDTO dto){
        try {
            userProfileAppService.updateUserPassword(dto);
            return org.springframework.http.ResponseEntity.ok().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason()));
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public org.springframework.http.ResponseEntity<org.offitec.osp.presentation.dto.UserProfileDTO> getUserProfile(@org.springframework.web.bind.annotation.PathVariable Long id) {
        org.offitec.osp.presentation.dto.UserProfileDTO profile = userProfileAppService.getUserProfile(id);
        return org.springframework.http.ResponseEntity.ok(profile);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/delete-account/{id}")
    public org.springframework.http.ResponseEntity<Void> deleteUserProfile(@org.springframework.web.bind.annotation.PathVariable Long id) {
        String adminEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        userProfileAppService.softDeleteUser(id, adminEmail);
        return org.springframework.http.ResponseEntity.ok().build();
    }
}
