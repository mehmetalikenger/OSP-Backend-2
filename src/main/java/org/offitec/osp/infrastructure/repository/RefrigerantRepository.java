package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Refrigerant;
import org.offitec.osp.domain.port.RefrigerantRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefrigerantRepository extends JpaRepository<Refrigerant, Long>, RefrigerantRepositoryPort {
    boolean existsByCodeAndDeletedFalse(String code);
    boolean existsByCodeAndIdNotAndDeletedFalse(String code, Long id);
}
