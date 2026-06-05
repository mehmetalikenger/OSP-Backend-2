package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryPort {

    Optional<User> findById(Long id);
}
