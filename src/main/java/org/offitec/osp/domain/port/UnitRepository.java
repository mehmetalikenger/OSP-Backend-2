package org.offitec.osp.domain.port;

import org.offitec.osp.domain.entity.Unit;

import java.util.Optional;

public interface UnitRepository {

    Optional<Unit> findByModelAndDeletedFalse(String model);
}
