package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.CalculationOutputValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculationOutputValuesRepository extends JpaRepository<CalculationOutputValues, Long> {
}
