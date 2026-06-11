package org.offitec.osp.application.service;

import org.offitec.osp.domain.service.AdminPanelService;
import org.offitec.osp.presentation.dto.UserProfileDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminPanelAppService {

    private final AdminPanelService adminPanelService;

    public AdminPanelAppService(AdminPanelService adminPanelService){
        this.adminPanelService = adminPanelService;
    }

    public List<UserProfileDTO> getAllUsers() {
        return adminPanelService.getAllUsersByRole(org.offitec.osp.domain.enums.UserRole.USER)
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(this::mapToDTO).toList();
    }

    public List<UserProfileDTO> getAllAdmins() {
        return adminPanelService.getAllUsersByRole(org.offitec.osp.domain.enums.UserRole.ADMIN)
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(this::mapToDTO).toList();
    }

    public void deleteUser(Long id, String adminEmail) {
        adminPanelService.deleteUser(id, adminEmail);
    }

    public void updateUserCategory(Long id, org.offitec.osp.domain.enums.UserCategory category, String adminEmail) {
        adminPanelService.updateUserCategory(id, category, adminEmail);
    }

    private UserProfileDTO mapToDTO(org.offitec.osp.domain.entity.User user) {
        String surname = null;
        if (user instanceof org.offitec.osp.domain.entity.Admin admin) {
            surname = admin.getSurname();
        }
        return new UserProfileDTO(
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
            user.getStatus()
        );
    }
}
