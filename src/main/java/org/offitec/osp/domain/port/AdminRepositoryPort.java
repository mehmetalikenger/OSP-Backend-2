package org.offitec.osp.domain.port;

import org.offitec.osp.domain.entity.Admin;

import java.util.Optional;

public interface AdminRepositoryPort {

    Optional<Admin> findByEmail(String email);
    Optional<Admin> findById(Long id);
    Admin save(Admin admin);
}
