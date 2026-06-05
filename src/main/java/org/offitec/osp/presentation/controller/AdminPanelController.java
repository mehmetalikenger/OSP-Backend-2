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

    public AdminPanelController(AdminRegisterAppService adminRegisterAppService){
        this.adminRegisterAppService = adminRegisterAppService;
    }

    @PostMapping("/user-register")
    public HttpStatus userAdmin(@Valid @RequestBody UserRegisterDTO dto){

        adminRegisterAppService.UserRegister(dto);

        return HttpStatus.OK;
    }

    @PostMapping("/admin-register")
    public HttpStatus registerAdmin(@Valid @RequestBody AdminRegisterDTO dto){

        adminRegisterAppService.AdminRegister(dto);

        return HttpStatus.OK;
    }

}
