package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Chassis;
import org.offitec.osp.domain.port.ChassisRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChassisRepository extends JpaRepository<Chassis, Long>, ChassisRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
