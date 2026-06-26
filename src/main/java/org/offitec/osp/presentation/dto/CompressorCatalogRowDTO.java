package org.offitec.osp.presentation.dto;

/**
 * One imported (compressor × refrigerant) rating, flattened for the admin cascade picker
 * (Refrigerant → Brand → Kind → Model → ratingId).
 */
public record CompressorCatalogRowDTO(
        Long ratingId,
        String refrigerant,
        String brand,
        String kind,    // CompressorKind: RC / SC / SCR / ISCR
        String model
) {}
