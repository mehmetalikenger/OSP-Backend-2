package org.offitec.osp.domain.port;

public interface CompressorRepositoryPort {

    public boolean existsByModel(String model);

    public boolean existsByModelAndIdNot(String model, Long id);
}
