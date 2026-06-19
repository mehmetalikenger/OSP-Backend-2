package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Condenser;
import org.offitec.osp.domain.port.CondenserRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CondenserRepository extends JpaRepository<Condenser, Long>, CondenserRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
