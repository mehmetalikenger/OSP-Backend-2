package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Compressor;
import org.offitec.osp.domain.port.CompressorRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompressorRepository extends JpaRepository<Compressor, Long>, CompressorRepositoryPort {

    boolean existsByModel(String model);

    boolean existsByModelAndIdNot(String model, Long id);
}
