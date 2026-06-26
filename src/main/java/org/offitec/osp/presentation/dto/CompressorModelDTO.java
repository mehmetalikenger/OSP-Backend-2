package org.offitec.osp.presentation.dto;

import java.util.List;

/**
 * One imported compressor model for the admin picker, with the refrigerants it can be rated on.
 * The admin selects a model + refrigerant, which resolves to a CompressorRating (ratingId).
 */
public record CompressorModelDTO(String model, String type, List<RatingOption> refrigerants) {
    public record RatingOption(Long ratingId, String refrigerant) {}
}
