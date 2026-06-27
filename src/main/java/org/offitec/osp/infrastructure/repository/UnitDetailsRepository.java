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

    // All (unitId, mod, capacity, maxCapacity) rows for the given units in ONE query, so
    // the catalog can build each card's per-mode capacity range without an N+1. Capacity now lives
    // on the rating's per-mode CompressorModeCapacity, matched to the unit-mode's mod.
    // Returns Object[]{ Long unitId, Mod mod, double capacity, Double maxCapacity }.
    @Query("""
            SELECT d.unit.id, d.mod, mc.capacity, mc.maxCapacity
            FROM UnitDetails d
            JOIN d.techSpecs ts
            JOIN ts.compressorRating cr
            JOIN cr.modeCapacities mc
            WHERE d.unit.id IN :unitIds AND mc.mod = d.mod
            """)
    List<Object[]> findModeCapacitiesByUnitIds(@Param("unitIds") Collection<Long> unitIds);
}
