package org.offitec.osp.domain.port;

public interface ChassisRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
