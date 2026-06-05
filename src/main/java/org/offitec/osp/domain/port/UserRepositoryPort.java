package org.offitec.osp.domain.port;

import org.offitec.osp.domain.entity.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User save(User user);
}
