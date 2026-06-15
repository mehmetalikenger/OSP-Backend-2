package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.FourWayReversingValve;
import org.offitec.osp.domain.port.FourWayReversingValveRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FourWayReversingValveRepository extends JpaRepository<FourWayReversingValve, Long>, FourWayReversingValveRepositoryPort {
    boolean existsByModel(String model);
    boolean existsByModelAndIdNot(String model, Long id);
}
