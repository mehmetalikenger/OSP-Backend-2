package org.offitec.osp.domain.service;

import org.offitec.osp.domain.data.AdminRegisterData;
import org.offitec.osp.domain.data.UserRegisterData;
import org.offitec.osp.domain.entity.Admin;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.enums.UserRole;
import org.offitec.osp.domain.exception.AdminAlreadyExistsException;
import org.offitec.osp.domain.port.AdminRepositoryPort;
import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.offitec.osp.domain.port.TemporaryPasswordGeneratorPort;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserRegisterService {

    private final UserRepositoryPort userRepositoryPort;
    private final AdminRepositoryPort adminRepositoryPort;
    private final TemporaryPasswordGeneratorPort temporaryPasswordGeneratorPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AuditLogService auditLogService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public UserRegisterService(AdminRepositoryPort adminRepositoryPort, UserRepositoryPort userRepositoryPort, TemporaryPasswordGeneratorPort temporaryPasswordGeneratorPort,
                               PasswordEncoderPort passwordEncoderPort, AuditLogService auditLogService){

        this.userRepositoryPort = userRepositoryPort;
        this.adminRepositoryPort = adminRepositoryPort;
        this.temporaryPasswordGeneratorPort = temporaryPasswordGeneratorPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.auditLogService = auditLogService;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean UserRegister(UserRegisterData data){

        Optional<User> dbUser =  userRepositoryPort.findByEmail(data.email());

        if(dbUser.isPresent()){
            if (dbUser.get().getDeletedAt() != null) {
                User user = dbUser.get();
                user.setDeletedAt(null);
                user.setDeletedBy(null);
                user.setStatus(org.offitec.osp.domain.enums.UserStatus.PENDING);
                String rawPassword = temporaryPasswordGeneratorPort.generate();
                String hashedPassword = passwordEncoderPort.encode(rawPassword);
                user.setPassword(hashedPassword);
                user.setCategory(data.category() != null ? data.category() : org.offitec.osp.domain.enums.UserCategory.A);
                
                user.setRole(UserRole.USER);
                user = userRepositoryPort.save(user);
                
                entityManager.createNativeQuery("DELETE FROM admin WHERE id = :id")
                        .setParameter("id", user.getId())
                        .executeUpdate();
                        
                logCreationAction(data.adminEmail(), "USER", user.getId(), "Re-activated standard user");
                return true;
            }
            throw new AdminAlreadyExistsException("This user already exists.");
        }

        String rawPassword = temporaryPasswordGeneratorPort.generate();
        String hashedPassword = passwordEncoderPort.encode(rawPassword);

        User user = User.builder()
                .username(data.email().split("@")[0])
                .email(data.email())
                .password(hashedPassword)
                .category(data.category() != null ? data.category() : org.offitec.osp.domain.enums.UserCategory.A)
                .status(org.offitec.osp.domain.enums.UserStatus.PENDING)
                .build();

        user = userRepositoryPort.save(user);
        logCreationAction(data.adminEmail(), "USER", user.getId(), "Created standard user");
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean AdminRegister(AdminRegisterData data){

        Optional<User> dbUser =  userRepositoryPort.findByEmail(data.email());

        if(dbUser.isPresent()){
            User user = dbUser.get();
            if (user instanceof Admin || user.getRole() == UserRole.ADMIN) {
                if (user.getDeletedAt() != null) {
                    user.setDeletedAt(null);
                    user.setDeletedBy(null);
                    user.setStatus(org.offitec.osp.domain.enums.UserStatus.PENDING);
                    String rawPassword = temporaryPasswordGeneratorPort.generate();
                    String hashedPassword = passwordEncoderPort.encode(rawPassword);
                    user.setPassword(hashedPassword);
                    user = userRepositoryPort.save(user);
                    logCreationAction(data.adminEmail(), "ADMIN", user.getId(), "Re-activated admin user");
                    return true;
                }
                throw new AdminAlreadyExistsException("This admin already exists.");
            } else {
                user.setRole(UserRole.ADMIN);
                boolean requiresActivation = false;

                if (user.getDeletedAt() != null) {
                    user.setDeletedAt(null);
                    user.setDeletedBy(null);
                    user.setStatus(org.offitec.osp.domain.enums.UserStatus.PENDING);
                    String rawPassword = temporaryPasswordGeneratorPort.generate();
                    String hashedPassword = passwordEncoderPort.encode(rawPassword);
                    user.setPassword(hashedPassword);
                    requiresActivation = true;
                } else if (user.getStatus() != org.offitec.osp.domain.enums.UserStatus.ACTIVE) {
                    requiresActivation = true;
                }

                user = userRepositoryPort.save(user);
                
                entityManager.createNativeQuery("INSERT INTO admin (id) VALUES (:id)")
                        .setParameter("id", user.getId())
                        .executeUpdate();
                logCreationAction(data.adminEmail(), "ADMIN", user.getId(), "Promoted user to admin");
                return requiresActivation;
            }
        }

        String rawPassword = temporaryPasswordGeneratorPort.generate();
        String hashedPassword = passwordEncoderPort.encode(rawPassword);

        Admin admin = Admin.builder()
                        .username(data.email().split("@")[0])
                        .email(data.email())
                        .password(hashedPassword)
                        .role(UserRole.ADMIN)
                        .status(org.offitec.osp.domain.enums.UserStatus.PENDING)
                        .build();

        admin = adminRepositoryPort.save(admin);
        logCreationAction(data.adminEmail(), "ADMIN", admin.getId(), "Created admin user");
        return true;
    }

    private void logCreationAction(String adminEmail, String entityType, Long entityId, String details) {
        Long adminId = -1L;
        if (adminEmail != null) {
            Optional<User> adminOpt = userRepositoryPort.findByEmail(adminEmail);
            if (adminOpt.isPresent()) {
                adminId = adminOpt.get().getId();
            }
        }
        auditLogService.logAdminAction(adminId, "CREATE", entityType, entityId, details);
    }
}
