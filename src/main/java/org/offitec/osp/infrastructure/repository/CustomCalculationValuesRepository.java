package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.CustomCalculationValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomCalculationValuesRepository extends JpaRepository<CustomCalculationValues, Long> {
}
