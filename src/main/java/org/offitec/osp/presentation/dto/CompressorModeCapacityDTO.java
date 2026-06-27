package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin payload for an upsert of a {@link org.offitec.osp.domain.entity.CompressorModeCapacity}
 * (nominal capacity/power per operating mode of a rating). Keyed by (compressorRatingId, mod).
 * Used by both Frascold and Copeland ratings to fill their per-mode nominal duties.
 */
@Getter
@Setter
public class CompressorModeCapacityDTO {

    @NotNull(message = "Compressor rating Id can't be null.")
    private Long compressorRatingId;

    @NotNull(message = "Mode can't be null.")
    private String mod; // COOLING / HEATING

    private double capacity;
    private double powerInput;

    // Upper capacity for variable-speed ratings; null for fixed-speed.
    private Double maxCapacity;
}
