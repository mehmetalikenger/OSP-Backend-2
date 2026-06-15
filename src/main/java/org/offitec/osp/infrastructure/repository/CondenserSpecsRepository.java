package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.CondenserSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CondenserSpecsRepository extends JpaRepository<CondenserSpecs, Long> {
}
