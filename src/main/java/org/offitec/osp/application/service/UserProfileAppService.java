package org.offitec.osp.application.service;

import org.offitec.osp.domain.data.AdminUpdateData;
import org.offitec.osp.domain.data.UserAddressData;
import org.offitec.osp.domain.data.UserPasswordData;
import org.offitec.osp.domain.data.UserUpdateData;
import org.offitec.osp.domain.service.UserProfileService;
import org.offitec.osp.infrastructure.storage.S3Service;
import org.offitec.osp.presentation.dto.AdminUpdateDTO;
import org.offitec.osp.presentation.dto.UserAddressDTO;
import org.offitec.osp.presentation.dto.UserPasswordDTO;
import org.offitec.osp.presentation.dto.UserUpdateDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileAppService {

    private final UserProfileService userProfileService;
    private final S3Service s3Service;

    public UserProfileAppService(UserProfileService userProfileService, S3Service s3Service){
        this.userProfileService = userProfileService;
        this.s3Service = s3Service;
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

        UserPasswordData data = new UserPasswordData(dto.getId(), dto.getCurrentPassword(), dto.getPassword());
        userProfileService.updateUserPassword(data);
    }

    public org.offitec.osp.presentation.dto.UserProfileDTO getUserProfile(Long id) {
        org.offitec.osp.domain.entity.User user = userProfileService.getUserProfile(id);
        return mapToDTO(user);
    }

    public java.util.List<org.offitec.osp.presentation.dto.UserProfileDTO> getAllUsers() {
        return userProfileService.getAllUsersByRole(org.offitec.osp.domain.enums.UserRole.USER)
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(this::mapToDTO).toList();
    }

    public java.util.List<org.offitec.osp.presentation.dto.UserProfileDTO> getAllAdmins() {
        return userProfileService.getAllUsersByRole(org.offitec.osp.domain.enums.UserRole.ADMIN)
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(this::mapToDTO).toList();
    }


    public void uploadProfilePicture(Long userId, MultipartFile file) {
        org.offitec.osp.domain.entity.User user = userProfileService.getUserProfile(userId);
        if (user.getImageUrl() != null) {
            s3Service.deleteUserPicture(user.getImageUrl());
        }
        String key = userId + "-" + file.getOriginalFilename();
        String url = s3Service.uploadUserPicture(key, file);
        userProfileService.updateImageUrl(userId, url);
    }

    public void softDeleteUser(Long id, String adminEmail) {
        userProfileService.softDeleteUser(id, adminEmail);
    }

    public void updateUserCategory(Long id, org.offitec.osp.domain.enums.UserCategory category) {
        userProfileService.updateUserCategory(id, category);
    }

    private org.offitec.osp.presentation.dto.UserProfileDTO mapToDTO(org.offitec.osp.domain.entity.User user) {
        String surname = null;
        if (user instanceof org.offitec.osp.domain.entity.Admin admin) {
            surname = admin.getSurname();
        }
        String signedImageUrl = null;
        if (user.getImageUrl() != null) {
            signedImageUrl = s3Service.presignUserPicture(user.getImageUrl());
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
            user.getCreatedAt(),
            user.getStatus(),
            signedImageUrl
        );
    }
}
