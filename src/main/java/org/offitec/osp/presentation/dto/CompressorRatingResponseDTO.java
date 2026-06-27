package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Listing row for a {@link org.offitec.osp.domain.entity.CompressorRating}: identity for the
 * unit-build compressor selector and the admin edit page (which filter by brand/type client-side),
 * the coefficient arrays, the reference/speed fields, and the per-mode capacities.
 */
@Getter
@Setter
@NoArgsConstructor
public class CompressorRatingResponseDTO {

    private Long id;
    private Long compressorId;
    private String brand;
    private String type;   // CompressorKind
    private String model;

    private Long refrigerantId;
    private String refrigerantCode;

    private double[] capCoeffs;
    private double[] powerCoeffs;
    private double[] massCoeffs;

    private double ohRef;
    private double scRef;
    private Double minFrequency;
    private Double maxFrequency;
    private Double minSpeed;
    private Double maxSpeed;

    private List<CompressorModeCapacityDTO> modeCapacities;
}
