package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.SavedUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedUnitRepository extends JpaRepository<SavedUnit, Long> {

    Optional<SavedUnit> findByUserIdAndUnitId(Long userId, Long unitId);

    List<SavedUnit> findByUserId(Long userId);

    boolean existsByUserIdAndUnitId(Long userId, Long unitId);
}
