package org.offitec.osp.domain.port;

public interface ChassisRepositoryPort {
    boolean existsByModel(String model);
    boolean existsByModelAndIdNot(String model, Long id);
}
