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

            throw new AdminAlreadyExistsException("This user already exists.");
        }

        String rawPassword = temporaryPasswordGeneratorPort.generate();
        String hashedPassword = passwordEncoderPort.encode(rawPassword);

        User user = User.builder()
                .username(data.email().split("@")[0])
                .email(data.email())
                .password(hashedPassword)
                .build();

        userRepositoryPort.save(user);
    }

    public void AdminRegister(AdminRegisterData data){

        Optional<Admin> dbAdmin =  adminRepositoryPort.findByEmail(data.email());

        if(dbAdmin.isPresent()){

            throw new AdminAlreadyExistsException("This admin already exists.");
        }

        String rawPassword = temporaryPasswordGeneratorPort.generate();
        String hashedPassword = passwordEncoderPort.encode(rawPassword);

        Admin admin = Admin.builder()
                        .username(data.email().split("@")[0])
                        .email(data.email())
                        .password(hashedPassword)
                        .role(UserRole.ADMIN)
                        .build();

        adminRepositoryPort.save(admin);
    }
}
