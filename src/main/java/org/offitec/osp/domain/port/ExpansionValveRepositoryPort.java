package org.offitec.osp.domain.port;

public interface ExpansionValveRepositoryPort {
    boolean existsByModel(String model);
    boolean existsByModelAndIdNot(String model, Long id);
}
