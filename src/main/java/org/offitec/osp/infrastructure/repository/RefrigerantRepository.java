package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Refrigerant;
import org.offitec.osp.domain.port.RefrigerantRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefrigerantRepository extends JpaRepository<Refrigerant, Long>, RefrigerantRepositoryPort {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
