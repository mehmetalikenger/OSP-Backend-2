package org.offitec.osp.presentation.dto;

import java.util.List;

/** A project plus its added unit-calculations (project details). */
public record ProjectDTO(
        Long id,
        String name,
        String company,
        String address,
        String country,
        String city,
        String phone,
        String createdAt,
        String updatedAt,
        List<ProjectDetailDTO> details
) {
    /**
     * One added unit-calculation inside a project. A heat pump carries two mode results
     * (COOLING + HEATING) under the single dual-mode PDF; a chiller carries one.
     */
    public record ProjectDetailDTO(
            Long id,
            Long unitId,
            String unitName,
            String unitModel,
            String primaryImageUrl,
            List<ModeResultDTO> modes,
            String pdfUrl
    ) {}

    /** One operating mode's inputs and computed outputs within a project detail. */
    public record ModeResultDTO(
            String mod,
            double ambient,
            double evapIn,
            double evapOut,
            double condIn,
            double condOut,
            double capacityKw,
            double powerInputKw,
            double copEer
    ) {}
}
