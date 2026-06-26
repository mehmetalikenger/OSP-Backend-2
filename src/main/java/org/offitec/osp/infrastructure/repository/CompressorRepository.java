package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Compressor;
import org.offitec.osp.domain.port.CompressorRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompressorRepository extends JpaRepository<Compressor, Long>, CompressorRepositoryPort {

    boolean existsByModelAndDeletedFalse(String model);

    boolean existsByModelAndIdNotAndDeletedFalse(String model, Long id);

    boolean existsBySrcKey(Integer srcKey);

    @Query("select c.srcKey from Compressor c where c.srcKey is not null")
    List<Integer> findAllSrcKeys();
}
