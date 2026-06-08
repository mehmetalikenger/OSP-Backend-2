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

        UserUpdateData userData = new UserUpdateData(dto.getId(), dto.getUsername(), dto.getEmail(), dto.getPhone());
        userProfileService.updateUser(userData);

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

    public org.offitec.osp.presentation.dto.UserProfileDTO getUserProfile(Long id) {
        org.offitec.osp.domain.entity.User user = userProfileService.getUserProfile(id);
        return mapToDTO(user);
    }

    public java.util.List<org.offitec.osp.presentation.dto.UserProfileDTO> getAllUsers() {
        return userProfileService.getAllUsersByRole(org.offitec.osp.domain.enums.UserRole.USER)
                .stream().map(this::mapToDTO).toList();
    }

    public java.util.List<org.offitec.osp.presentation.dto.UserProfileDTO> getAllAdmins() {
        return userProfileService.getAllUsersByRole(org.offitec.osp.domain.enums.UserRole.ADMIN)
                .stream().map(this::mapToDTO).toList();
    }

    public void deleteUser(Long id) {
        userProfileService.deleteUser(id);
    }

    public void updateUserCategory(Long id, org.offitec.osp.domain.enums.UserCategory category) {
        userProfileService.updateUserCategory(id, category);
    }

    private org.offitec.osp.presentation.dto.UserProfileDTO mapToDTO(org.offitec.osp.domain.entity.User user) {
        String surname = null;
        if (user instanceof org.offitec.osp.domain.entity.Admin admin) {
            surname = admin.getSurname();
        }
        return new org.offitec.osp.presentation.dto.UserProfileDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getAddress(),
            user.getCountry(),
            user.getCity(),
            surname,
            user.getCategory(),
            user.getCreatedAt()
        );
    }
}
