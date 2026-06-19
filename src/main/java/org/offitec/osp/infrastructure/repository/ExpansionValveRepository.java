package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.ExpansionValve;
import org.offitec.osp.domain.port.ExpansionValveRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpansionValveRepository extends JpaRepository<ExpansionValve, Long>, ExpansionValveRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
