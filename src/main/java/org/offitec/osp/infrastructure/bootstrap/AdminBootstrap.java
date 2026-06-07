package org.offitec.osp.infrastructure.bootstrap;

import org.offitec.osp.domain.data.AdminRegisterData;
import org.offitec.osp.domain.data.UserPasswordData;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.offitec.osp.domain.service.UserProfileService;
import org.offitec.osp.domain.service.UserRegisterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserRegisterService userRegisterService;
    private final UserProfileService userProfileService;
    private final UserRepositoryPort userRepositoryPort;

    public AdminBootstrap(UserRegisterService userRegisterService, UserProfileService userProfileService, UserRepositoryPort userRepositoryPort) {
        this.userRegisterService = userRegisterService;
        this.userProfileService = userProfileService;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public void run(String... args) throws Exception {
        // Admin
        String adminEmail = "admin@email.com";
        Optional<User> adminOpt = userRepositoryPort.findByEmail(adminEmail);

        if (adminOpt.isEmpty()) {
            userRegisterService.AdminRegister(new AdminRegisterData(adminEmail));
            adminOpt = userRepositoryPort.findByEmail(adminEmail);
        }

        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            UserPasswordData passwordData = new UserPasswordData(admin.getId(), "Qwer123.4");
            userProfileService.updateUserPassword(passwordData);
            System.out.println("Default admin updated: " + adminEmail);
        }

        // User
        String userEmail = "user@email.com";
        Optional<User> userOpt = userRepositoryPort.findByEmail(userEmail);

        if (userOpt.isEmpty()) {
            userRegisterService.UserRegister(new org.offitec.osp.domain.data.UserRegisterData(userEmail, org.offitec.osp.domain.enums.UserCategory.A));
            userOpt = userRepositoryPort.findByEmail(userEmail);
        }

        if (userOpt.isPresent()) {
            User standardUser = userOpt.get();
            UserPasswordData userPasswordData = new UserPasswordData(standardUser.getId(), "Qwer123.4");
            userProfileService.updateUserPassword(userPasswordData);
            System.out.println("Default user updated: " + userEmail);
        }
    }
}
