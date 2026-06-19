package org.offitec.osp.domain.port;

public interface CompressorRepositoryPort {

    public boolean existsByModelAndDeletedFalse(String model);

    public boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);
}
