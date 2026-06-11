package org.offitec.osp.infrastructure.bootstrap;

import org.offitec.osp.domain.data.AdminRegisterData;
import org.offitec.osp.domain.data.UserPasswordData;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.offitec.osp.domain.service.UserProfileService;
import org.offitec.osp.domain.service.UserRegisterService;
import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRegisterService userRegisterService;
    private final UserProfileService userProfileService;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public AdminBootstrap(UserRegisterService userRegisterService, UserProfileService userProfileService, UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
        this.userRegisterService = userRegisterService;
        this.userProfileService = userProfileService;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public void run(String... args) throws Exception {
        // Admin
        String adminEmail = "admin@email.com";
        Optional<User> adminOpt = userRepositoryPort.findByEmail(adminEmail);

        if (adminOpt.isEmpty()) {
            userRegisterService.AdminRegister(new AdminRegisterData(adminEmail, null));
            adminOpt = userRepositoryPort.findByEmail(adminEmail);
        }

        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            admin.setPassword(passwordEncoderPort.encode("Qwer123.4"));
            admin.setStatus(org.offitec.osp.domain.enums.UserStatus.ACTIVE);
            userRepositoryPort.save(admin);
            System.out.println("Default admin updated: " + adminEmail);
        }

        // User
        String userEmail = "user@email.com";
        Optional<User> userOpt = userRepositoryPort.findByEmail(userEmail);

        if (userOpt.isEmpty()) {
            userRegisterService.UserRegister(new org.offitec.osp.domain.data.UserRegisterData(userEmail, org.offitec.osp.domain.enums.UserCategory.A, null));
            userOpt = userRepositoryPort.findByEmail(userEmail);
        }

        if (userOpt.isPresent()) {
            User standardUser = userOpt.get();
            standardUser.setPassword(passwordEncoderPort.encode("Qwer123.4"));
            standardUser.setStatus(org.offitec.osp.domain.enums.UserStatus.ACTIVE);
            userRepositoryPort.save(standardUser);
            System.out.println("Default user updated: " + userEmail);
        }
    }
}
