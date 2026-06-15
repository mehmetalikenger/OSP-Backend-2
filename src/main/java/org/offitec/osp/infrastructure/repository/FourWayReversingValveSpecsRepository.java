package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.FourWayReversingValveSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FourWayReversingValveSpecsRepository extends JpaRepository<FourWayReversingValveSpecs, Long> {
}
