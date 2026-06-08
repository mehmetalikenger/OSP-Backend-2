package org.offitec.osp.domain.service;

import org.offitec.osp.domain.data.AdminUpdateData;
import org.offitec.osp.domain.data.UserAddressData;
import org.offitec.osp.domain.data.UserPasswordData;
import org.offitec.osp.domain.data.UserUpdateData;
import org.offitec.osp.domain.entity.Admin;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.port.AdminRepositoryPort;
import org.offitec.osp.domain.port.PasswordEncoderPort;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final AdminRepositoryPort adminRepositoryPort;

    public UserProfileService(UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort, AdminRepositoryPort adminRepositoryPort){
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.adminRepositoryPort = adminRepositoryPort;
    }

    public void updateUser(UserUpdateData data){

        Optional<User> dbUser = userRepositoryPort.findById(data.id());

        if(dbUser.isEmpty()){
            throw new RuntimeException();
        }

        User user = dbUser.get();

        user.setUsername(data.username());
        user.setEmail(data.email());
        user.setPhone(data.phone());

        userRepositoryPort.save(user);
    }

    //For admin specific attributes
    public void updateAdmin(AdminUpdateData data){

        Optional<Admin> dbAdmin = adminRepositoryPort.findById(data.id());

        if(dbAdmin.isEmpty()){

            throw new RuntimeException();
        }

        Admin admin = dbAdmin.get();
        admin.setSurname(data.surname());

        adminRepositoryPort.save(admin);
    }

    public void updateUserAddress(UserAddressData data){

        Optional <User> dbUser = userRepositoryPort.findById(data.id());

        if(dbUser.isEmpty()){
            throw new RuntimeException();
        }

        User user = dbUser.get();

        user.setAddress(data.address());
        user.setCountry(data.country());
        user.setCity(data.city());

        userRepositoryPort.save(user);
    }

    public void updateUserPassword(UserPasswordData data){

        Optional<User> dbUser = userRepositoryPort.findById(data.id());

        if(dbUser.isEmpty()){
            throw new RuntimeException();
        }

        User user = dbUser.get();

        String hashedPassword = passwordEncoderPort.encode(data.password());

        user.setPassword(hashedPassword);
        userRepositoryPort.save(user);
    }

    public User getUserProfile(Long id) {
        Optional<User> dbUser = userRepositoryPort.findById(id);
        if(dbUser.isEmpty()){
            throw new RuntimeException("User not found");
        }
        return dbUser.get();
    }

    public java.util.List<User> getAllUsersByRole(org.offitec.osp.domain.enums.UserRole role) {
        return userRepositoryPort.findAllByRole(role);
    }

    public void deleteUser(Long id) {
        Optional<User> dbUser = userRepositoryPort.findById(id);
        if(dbUser.isPresent()){
            User user = dbUser.get();
            // Free up the email so a new user can register with it
            user.setEmail(user.getEmail() + "_DELETED_" + java.util.UUID.randomUUID().toString());
            userRepositoryPort.save(user);
        }
        userRepositoryPort.deleteById(id);
    }

    public void updateUserCategory(Long id, org.offitec.osp.domain.enums.UserCategory category) {
        Optional<User> dbUser = userRepositoryPort.findById(id);
        if(dbUser.isEmpty()){
            throw new RuntimeException("User not found");
        }
        User user = dbUser.get();
        user.setCategory(category);
        userRepositoryPort.save(user);
    }
}
