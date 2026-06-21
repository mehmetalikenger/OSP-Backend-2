package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Unit;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.port.UnitRepository;
import org.offitec.osp.presentation.dto.UnitCardDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitJpaRepository extends JpaRepository<Unit, Long>, UnitRepository {

    List<Unit> findByCategory(UnitCategory category);

    List<Unit> findByCategoryAndUnitType(UnitCategory category, UnitTypeEnum unitType);

    // Catalog list as a single-query projection: only the columns a card shows, no
    // entity graph, no per-unit lazy loads. The primary image and the per-user `saved`
    // flag are correlated subqueries so the whole page is one round trip.
    // `capacityRange` is left empty here and filled by the service from a second
    // batched query (capacity is one value per mode, so it can't live in this flat row).
    @Query("""
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
            """)
    List<UnitCardDTO> findCards(@Param("category") UnitCategory category,
                               @Param("unitType") UnitTypeEnum unitType,
                               @Param("userId") Long userId);

    // Same projection, but driven from the user's saved units. `saved` is always true.
    // category/type are optional filters (null = no filter).
    @Query("""
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
            """)
    List<UnitCardDTO> findSavedCards(@Param("userId") Long userId,
                                    @Param("category") UnitCategory category,
                                    @Param("unitType") UnitTypeEnum unitType);

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
}
