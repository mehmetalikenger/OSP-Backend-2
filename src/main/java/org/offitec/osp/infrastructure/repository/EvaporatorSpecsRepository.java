package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.EvaporatorSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaporatorSpecsRepository extends JpaRepository<EvaporatorSpecs, Long> {
}
