package org.offitec.osp.application.service;

import org.offitec.osp.domain.data.AdminRegisterData;
import org.offitec.osp.domain.data.UserRegisterData;
import org.offitec.osp.domain.service.UserRegisterService;
import org.offitec.osp.presentation.dto.AdminRegisterDTO;
import org.offitec.osp.presentation.dto.UserRegisterDTO;
import org.springframework.stereotype.Service;

@Service
public class AdminRegisterAppService {

    private final UserRegisterService userRegisterService;

    public AdminRegisterAppService(UserRegisterService userRegisterService){
        this.userRegisterService = userRegisterService;
    }

    public void AdminRegister(AdminRegisterDTO dto){

        AdminRegisterData data = new AdminRegisterData(dto.getEmail());
        userRegisterService.AdminRegister(data);
    }

    public void UserRegister(UserRegisterDTO dto){

        UserRegisterData data = new UserRegisterData(dto.getEmail());
        userRegisterService.UserRegister(data);
    }
}
