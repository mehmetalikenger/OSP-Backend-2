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
    public HttpStatus updateUserPassword(@Valid @RequestBody UserPasswordDTO dto){

        userProfileAppService.updateUserPassword(dto);

        return HttpStatus.OK;
    }
}
