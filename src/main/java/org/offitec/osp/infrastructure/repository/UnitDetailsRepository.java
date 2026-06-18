package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.UnitDetails;
import org.offitec.osp.domain.enums.Mod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitDetailsRepository extends JpaRepository<UnitDetails, Long> {
    Optional<UnitDetails> findByUnitIdAndMod(Long unitId, Mod mod);
}
