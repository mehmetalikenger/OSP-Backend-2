package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.CompressorRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompressorRatingRepository extends JpaRepository<CompressorRating, Long> {

    List<CompressorRating> findByCompressorId(Long compressorId);

    Optional<CompressorRating> findByCompressorIdAndRefrigerantId(Long compressorId, Long refrigerantId);

    @Query("select count(r) from CompressorRating r where r.compressor.imported = true")
    long countImported();

    @Query("select cr from CompressorRating cr join fetch cr.compressor c join fetch cr.refrigerant r "
            + "where c.model = :model and r.code = :refrigerant")
    List<CompressorRating> findByModelAndRefrigerant(String model, String refrigerant);

    // Catalogue for the admin cascade picker: flat rows of (ratingId, refrigerant, brand, kind, model)
    // for every calculable rating on a live compressor. The frontend filters Refrigerant -> Brand ->
    // Kind -> Model. Ordered so distinct values come out stable.
    @Query("select cr.id, r.code, c.brand, c.type, c.model "
            + "from CompressorRating cr join cr.compressor c join cr.refrigerant r "
            + "where cr.calculable = true and c.deleted = false "
            + "order by r.code, c.brand, c.type, c.model")
    List<Object[]> findCalculableCatalog();
}
