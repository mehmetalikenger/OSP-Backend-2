package org.offitec.osp.domain.port;

public interface FourWayReversingValveRepositoryPort {
    boolean existsByModel(String model);
    boolean existsByModelAndIdNot(String model, Long id);
}
