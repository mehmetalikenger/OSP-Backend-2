package org.offitec.osp.domain.port;

public interface RefrigerantRepositoryPort {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
