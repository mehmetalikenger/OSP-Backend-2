package org.offitec.osp.domain.port;

public interface FourWayReversingValveRepositoryPort {
    boolean existsByModelAndDeletedFalse(String model);
    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
