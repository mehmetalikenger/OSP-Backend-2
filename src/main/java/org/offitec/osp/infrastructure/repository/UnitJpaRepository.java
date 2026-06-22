package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Unit;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.port.UnitRepository;
import org.offitec.osp.presentation.dto.UnitCardDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitJpaRepository extends JpaRepository<Unit, Long>, UnitRepository {

    List<Unit> findByCategory(UnitCategory category);

    List<Unit> findByCategoryAndUnitType(UnitCategory category, UnitTypeEnum unitType);

    // Catalog list as a single-query projection: only the columns a card shows, no
    // entity graph, no per-unit lazy loads. The primary image and the per-user `saved`
    // flag are correlated subqueries so the whole page is one round trip.
    // `capacityRange` is left empty here and filled by the service from a second
    // batched query (capacity is one value per mode, so it can't live in this flat row).
    @Query(value = """
            SELECT new org.offitec.osp.presentation.dto.UnitCardDTO(
                u.id, u.name, u.model,
                (SELECT a.url FROM UnitAsset a
                   WHERE a.unit = u
                     AND a.assetType = org.offitec.osp.domain.enums.AssetType.IMAGE
                     AND a.isPrimary = true),
                '',
                r.code,
                u.unitType, u.category,
                (SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
                   FROM SavedUnit s WHERE s.unit = u AND s.user.id = :userId)
            )
            FROM Unit u
            LEFT JOIN u.refrigerant r
            WHERE u.category = :category AND u.unitType = :unitType AND u.deleted = false
            ORDER BY u.id
            """,
            countQuery = """
            SELECT COUNT(u) FROM Unit u
            WHERE u.category = :category AND u.unitType = :unitType AND u.deleted = false
            """)
    Page<UnitCardDTO> findCards(@Param("category") UnitCategory category,
                               @Param("unitType") UnitTypeEnum unitType,
                               @Param("userId") Long userId,
                               Pageable pageable);

    // Same projection, but driven from the user's saved units. `saved` is always true.
    // category/type are optional filters (null = no filter).
    @Query(value = """
            SELECT new org.offitec.osp.presentation.dto.UnitCardDTO(
                u.id, u.name, u.model,
                (SELECT a.url FROM UnitAsset a
                   WHERE a.unit = u
                     AND a.assetType = org.offitec.osp.domain.enums.AssetType.IMAGE
                     AND a.isPrimary = true),
                '',
                r.code,
                u.unitType, u.category,
                true
            )
            FROM SavedUnit s
            JOIN s.unit u
            LEFT JOIN u.refrigerant r
            WHERE s.user.id = :userId AND u.deleted = false
              AND (:category IS NULL OR u.category = :category)
              AND (:unitType IS NULL OR u.unitType = :unitType)
            ORDER BY u.id
            """,
            countQuery = """
            SELECT COUNT(u) FROM SavedUnit s
            JOIN s.unit u
            WHERE s.user.id = :userId AND u.deleted = false
              AND (:category IS NULL OR u.category = :category)
              AND (:unitType IS NULL OR u.unitType = :unitType)
            """)
    Page<UnitCardDTO> findSavedCards(@Param("userId") Long userId,
                                    @Param("category") UnitCategory category,
                                    @Param("unitType") UnitTypeEnum unitType,
                                    Pageable pageable);

    // Primary image URL per unit (the same image the catalog/saved cards show), fetched
    // for a set of units in one query. Each row is [unitId, url]. Used to put the unit's
    // image on project-detail cards without lazily loading each unit's asset collection.
    @Query("""
            SELECT a.unit.id, a.url FROM UnitAsset a
            WHERE a.unit.id IN :unitIds
              AND a.assetType = org.offitec.osp.domain.enums.AssetType.IMAGE
              AND a.isPrimary = true
            """)
    List<Object[]> findPrimaryImageUrls(@Param("unitIds") List<Long> unitIds);

    // Icon image URLs for a set of units in one query. Each row is [unitId, url]. Used to
    // show the unit's feature icons on catalog/saved cards without lazily loading each
    // unit's asset collection.
    @Query("""
            SELECT a.unit.id, a.url FROM UnitAsset a
            WHERE a.unit.id IN :unitIds
              AND a.assetType = org.offitec.osp.domain.enums.AssetType.ICON
            """)
    List<Object[]> findIconUrls(@Param("unitIds") List<Long> unitIds);

    // Loads the whole detail/calc graph in ONE query so the spec/calc views don't
    // walk a chain of lazy associations (unit -> details -> techSpecs -> each component
    // spec -> component). Only collection fetched is u.unitDetails (a single bag, so no
    // MultipleBagFetchException); everything below it is single-valued. Assets stay lazy
    // and load with one extra batched select when the view touches them.
    @Query("""
            SELECT DISTINCT u FROM Unit u
            LEFT JOIN FETCH u.chassis
            LEFT JOIN FETCH u.refrigerant
            LEFT JOIN FETCH u.unitDetails d
            LEFT JOIN FETCH d.defCalcValues
            LEFT JOIN FETCH d.techSpecs ts
            LEFT JOIN FETCH ts.compressorSpecs cs
            LEFT JOIN FETCH cs.compressor
            LEFT JOIN FETCH ts.condenserSpecs cds
            LEFT JOIN FETCH cds.condenser
            LEFT JOIN FETCH ts.evaporatorSpecs es
            LEFT JOIN FETCH es.evaporator
            LEFT JOIN FETCH ts.expansionValveSpecs evs
            LEFT JOIN FETCH evs.expansionValve
            LEFT JOIN FETCH ts.fourWayReversingValveSpecs fwvs
            LEFT JOIN FETCH fwvs.fourWayReversingValve
            WHERE u.id = :id
            """)
    Optional<Unit> findDetailGraphById(@Param("id") Long id);
}
