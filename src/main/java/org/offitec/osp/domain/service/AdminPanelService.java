package org.offitec.osp.domain.service;

import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminPanelService {

    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogService auditLogService;

    public AdminPanelService(UserRepositoryPort userRepositoryPort, AuditLogService auditLogService){
        this.userRepositoryPort = userRepositoryPort;
        this.auditLogService = auditLogService;
    }

    public List<User> getAllUsersByRole(org.offitec.osp.domain.enums.UserRole role) {
        return userRepositoryPort.findAllByRole(role);
    }

    public void deleteUser(Long userId, String adminEmail) {
        Optional<User> adminOpt = userRepositoryPort.findByEmail(adminEmail);
        Optional<User> targetUserOpt = userRepositoryPort.findById(userId);
        if (adminOpt.isPresent() && targetUserOpt.isPresent()) {
            User targetUser = targetUserOpt.get();
            targetUser.setDeletedAt(java.time.LocalDateTime.now());
            targetUser.setDeletedBy(adminOpt.get().getId());
            targetUser.setStatus(org.offitec.osp.domain.enums.UserStatus.DELETED);
            userRepositoryPort.save(targetUser);
            
            auditLogService.logAdminAction(adminOpt.get().getId(), "DELETE", "USER", targetUser.getId(), "Deleted user " + targetUser.getEmail());
        }
    }

    public void updateUserCategory(Long id, org.offitec.osp.domain.enums.UserCategory category, String adminEmail) {
        Optional<User> dbUser = userRepositoryPort.findById(id);
        if(dbUser.isEmpty()){
            throw new RuntimeException("User not found");
        }
        User user = dbUser.get();
        String oldCategory = user.getCategory() != null ? user.getCategory().name() : "NONE";
        user.setCategory(category);
        userRepositoryPort.save(user);

        Long adminId = -1L;
        if (adminEmail != null) {
            Optional<User> adminOpt = userRepositoryPort.findByEmail(adminEmail);
            if (adminOpt.isPresent()) {
                adminId = adminOpt.get().getId();
            }
        }
        String detailsJson = String.format("{\"changes\":{\"category\":{\"old\":\"%s\",\"new\":\"%s\"}}}", oldCategory, category.name());

        auditLogService.logAdminAction(adminId, "UPDATE", "USER", user.getId(), detailsJson);
    }
}
