package org.offitec.osp.application.service;

import org.offitec.osp.domain.data.AdminUpdateData;
import org.offitec.osp.domain.data.UserAddressData;
import org.offitec.osp.domain.data.UserPasswordData;
import org.offitec.osp.domain.data.UserUpdateData;
import org.offitec.osp.domain.service.UserProfileService;
import org.offitec.osp.presentation.dto.AdminUpdateDTO;
import org.offitec.osp.presentation.dto.UserAddressDTO;
import org.offitec.osp.presentation.dto.UserPasswordDTO;
import org.offitec.osp.presentation.dto.UserUpdateDTO;
import org.springframework.stereotype.Service;

@Service
public class UserProfileAppService {

    private final UserProfileService userProfileService;

    public UserProfileAppService(UserProfileService userProfileService){
        this.userProfileService = userProfileService;
    }

    public void updateUser(UserUpdateDTO dto){

        UserUpdateData data = new UserUpdateData(dto.getId(), dto.getUsername(), dto.getEmail(), dto.getPhone());
        userProfileService.updateUser(data);
    }

    //For admin specific attributes
    public void updateAdmin(AdminUpdateDTO dto){

        AdminUpdateData data = new AdminUpdateData(dto.getId(), dto.getSurname());
        userProfileService.updateAdmin(data);
    }

    public void updateUserAddress(UserAddressDTO dto){

        UserAddressData data = new UserAddressData(dto.getId(), dto.getAddress(), dto.getCountry(), dto.getCity());

        userProfileService.updateUserAddress(data);
    }

    public void updateUserPassword(UserPasswordDTO dto){

        UserPasswordData data = new UserPasswordData(dto.getId(), dto.getPassword());
        userProfileService.updateUserPassword(data);
    }
}
