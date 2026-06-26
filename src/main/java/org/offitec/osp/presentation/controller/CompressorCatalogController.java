package org.offitec.osp.presentation.controller;

import org.offitec.osp.infrastructure.repository.CompressorRatingRepository;
import org.offitec.osp.presentation.dto.CompressorCatalogRowDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only catalogue of imported Frascold ratings for the admin unit-builder's cascade picker
 * (Refrigerant → Brand → Kind → Model). Returns one flat row per calculable (compressor × refrigerant).
 */
@RestController
@RequestMapping("/admin/component")
public class CompressorCatalogController {

    private final CompressorRatingRepository ratingRepository;

    public CompressorCatalogController(CompressorRatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @GetMapping("/compressor-catalog")
    public List<CompressorCatalogRowDTO> catalog() {
        List<Object[]> rows = ratingRepository.findCalculableCatalog();
        List<CompressorCatalogRowDTO> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long ratingId = ((Number) row[0]).longValue();
            String refrigerant = (String) row[1];
            String brand = (String) row[2];
            String kind = row[3] != null ? row[3].toString() : null; // CompressorKind enum
            String model = (String) row[4];
            out.add(new CompressorCatalogRowDTO(ratingId, refrigerant, brand, kind, model));
        }
        return out;
    }
}
