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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public UserRegisterService(AdminRepositoryPort adminRepositoryPort, UserRepositoryPort userRepositoryPort, TemporaryPasswordGeneratorPort temporaryPasswordGeneratorPort,
                               PasswordEncoderPort passwordEncoderPort){

        this.userRepositoryPort = userRepositoryPort;
        this.adminRepositoryPort = adminRepositoryPort;
        this.temporaryPasswordGeneratorPort = temporaryPasswordGeneratorPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public void UserRegister(UserRegisterData data){

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
                userRepositoryPort.save(user);
                return;
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

        userRepositoryPort.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void AdminRegister(AdminRegisterData data){

        Optional<User> dbUser =  userRepositoryPort.findByEmail(data.email());

        if(dbUser.isPresent()){
            User user = dbUser.get();
            if (user instanceof Admin) {
                Admin admin = (Admin) user;
                if (admin.getDeletedAt() != null) {
                    admin.setDeletedAt(null);
                    admin.setDeletedBy(null);
                    admin.setStatus(org.offitec.osp.domain.enums.UserStatus.PENDING);
                    String rawPassword = temporaryPasswordGeneratorPort.generate();
                    String hashedPassword = passwordEncoderPort.encode(rawPassword);
                    admin.setPassword(hashedPassword);
                    adminRepositoryPort.save(admin);
                    return;
                }
                throw new AdminAlreadyExistsException("This admin already exists.");
            } else {
                user.setDeletedAt(null);
                user.setDeletedBy(null);
                user.setRole(UserRole.ADMIN);
                user.setStatus(org.offitec.osp.domain.enums.UserStatus.PENDING);
                String rawPassword = temporaryPasswordGeneratorPort.generate();
                String hashedPassword = passwordEncoderPort.encode(rawPassword);
                user.setPassword(hashedPassword);
                
                userRepositoryPort.save(user);
                
                entityManager.createNativeQuery("INSERT INTO admin (id) VALUES (:id)")
                        .setParameter("id", user.getId())
                        .executeUpdate();
                return;
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

        adminRepositoryPort.save(admin);
    }
}
