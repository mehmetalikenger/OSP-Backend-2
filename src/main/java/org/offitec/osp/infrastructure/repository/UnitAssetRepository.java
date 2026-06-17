package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.UnitAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitAssetRepository extends JpaRepository<UnitAsset, Long> {

    List<UnitAsset> findByUnitId(Long unitId);
}
