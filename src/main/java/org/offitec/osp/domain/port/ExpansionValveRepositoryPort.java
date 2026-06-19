package org.offitec.osp.domain.port;

public interface ExpansionValveRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
