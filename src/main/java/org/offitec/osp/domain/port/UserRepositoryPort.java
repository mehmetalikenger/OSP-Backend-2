package org.offitec.osp.domain.port;

import org.offitec.osp.domain.entity.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User save(User user);
    java.util.List<User> findAllByRole(org.offitec.osp.domain.enums.UserRole role);
    void deleteById(Long id);
}
