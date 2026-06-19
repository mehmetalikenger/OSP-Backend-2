package org.offitec.osp.domain.port;

public interface RefrigerantRepositoryPort {
    boolean existsByCodeAndDeletedFalse(String code);
    boolean existsByCodeAndIdNotAndDeletedFalse(String code, Long id);
}
