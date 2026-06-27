package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin payload for creating/editing a {@link org.offitec.osp.domain.entity.CompressorRating}
 * (one coefficient set per compressor + refrigerant). Used by the Copeland add/edit flow.
 */
@Getter
@Setter
public class CompressorRatingDTO {

    @NotNull(message = "Compressor Id can't be null.")
    private Long compressorId;

    @NotNull(message = "Refrigerant Id can't be null.")
    private Long refrigerantId;

    // EN12900-style coefficient arrays: length 10 (bivariate S,D) or 20 (trivariate S,D,R).
    private double[] capCoeffs;
    private double[] powerCoeffs;
    private double[] massCoeffs;

    // Reference condition. Defaults applied server-side: ohRef=10, scRef=0 when null.
    private Double ohRef;
    private Double scRef;

    // Inverter operating range (Hz / rpm). Null for fixed speed.
    private Double minFrequency;
    private Double maxFrequency;
    private Double minSpeed;
    private Double maxSpeed;
}
