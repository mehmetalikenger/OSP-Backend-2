package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Unit;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.port.UnitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitJpaRepository extends JpaRepository<Unit, Long>, UnitRepository {

    List<Unit> findByCategory(UnitCategory category);
}
