package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.FourWayReversingValve;
import org.offitec.osp.domain.port.FourWayReversingValveRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FourWayReversingValveRepository extends JpaRepository<FourWayReversingValve, Long>, FourWayReversingValveRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
