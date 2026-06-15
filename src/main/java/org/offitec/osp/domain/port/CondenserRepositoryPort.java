package org.offitec.osp.domain.port;

public interface CondenserRepositoryPort {
    boolean existsByModel(String model);
    boolean existsByModelAndIdNot(String model, Long id);
}
