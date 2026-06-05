package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Admin;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.port.AdminRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long>, AdminRepositoryPort {

    Optional<Admin> findById(Long id);
    Admin save(Admin admin);
}
