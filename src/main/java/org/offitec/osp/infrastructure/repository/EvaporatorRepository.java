package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Evaporator;
import org.offitec.osp.domain.port.EvaporatorRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaporatorRepository extends JpaRepository<Evaporator, Long>, EvaporatorRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
