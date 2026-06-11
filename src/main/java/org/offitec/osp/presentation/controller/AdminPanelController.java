package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.AdminRegisterAppService;
import org.offitec.osp.presentation.dto.AdminRegisterDTO;
import org.offitec.osp.presentation.dto.UserUpdateDTO;
import org.offitec.osp.presentation.dto.UserRegisterDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminPanelController {

    private final AdminRegisterAppService adminRegisterAppService;

    private final org.offitec.osp.application.service.AdminPanelAppService adminPanelAppService;

    public AdminPanelController(AdminRegisterAppService adminRegisterAppService, org.offitec.osp.application.service.AdminPanelAppService adminPanelAppService){
        this.adminRegisterAppService = adminRegisterAppService;
        this.adminPanelAppService = adminPanelAppService;
    }

    @PostMapping("/user-register")
    public HttpStatus userAdmin(@Valid @RequestBody UserRegisterDTO dto){

        String adminEmail = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        adminRegisterAppService.UserRegister(dto, adminEmail);

        return HttpStatus.OK;
    }

    @PostMapping("/admin-register")
    public HttpStatus registerAdmin(@Valid @RequestBody AdminRegisterDTO dto){

        String adminEmail = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        adminRegisterAppService.AdminRegister(dto, adminEmail);

        return HttpStatus.OK;
    }

    @org.springframework.web.bind.annotation.GetMapping("/users")
    public org.springframework.http.ResponseEntity<java.util.List<org.offitec.osp.presentation.dto.UserProfileDTO>> getAllUsers() {
        return org.springframework.http.ResponseEntity.ok(adminPanelAppService.getAllUsers());
    }

    @org.springframework.web.bind.annotation.GetMapping("/admins")
    public org.springframework.http.ResponseEntity<java.util.List<org.offitec.osp.presentation.dto.UserProfileDTO>> getAllAdmins() {
        return org.springframework.http.ResponseEntity.ok(adminPanelAppService.getAllAdmins());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/user/{id}")
    public HttpStatus deleteUser(@org.springframework.web.bind.annotation.PathVariable Long id) {
        String adminEmail = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        adminPanelAppService.deleteUser(id, adminEmail);
        return HttpStatus.OK;
    }

    @PostMapping("/update-category")
    public HttpStatus updateCategory(@RequestBody java.util.Map<String, String> payload) {
        Long userId = Long.valueOf(payload.get("userId"));
        org.offitec.osp.domain.enums.UserCategory category = org.offitec.osp.domain.enums.UserCategory.valueOf(payload.get("category"));
        String adminEmail = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        adminPanelAppService.updateUserCategory(userId, category, adminEmail);
        return HttpStatus.OK;
    }
}
