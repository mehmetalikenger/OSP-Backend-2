package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.UnitDetails;
import org.offitec.osp.domain.enums.Mod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UnitDetailsRepository extends JpaRepository<UnitDetails, Long> {
    Optional<UnitDetails> findByUnitIdAndMod(Long unitId, Mod mod);

    // All (unitId, mod, capacity) rows for the given units in ONE query, so the
    // catalog can build each card's per-mode capacity string without an N+1.
    // Returns Object[]{ Long unitId, Mod mod, double capacity }.
    @Query("""
            SELECT d.unit.id, d.mod, ts.capacity
            FROM UnitDetails d JOIN d.techSpecs ts
            WHERE d.unit.id IN :unitIds
            """)
    List<Object[]> findModeCapacitiesByUnitIds(@Param("unitIds") Collection<Long> unitIds);
}
