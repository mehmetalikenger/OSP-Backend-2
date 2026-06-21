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
    /** One added unit-calculation inside a project. */
    public record ProjectDetailDTO(
            Long id,
            Long unitId,
            String unitName,
            String unitModel,
            String primaryImageUrl,
            double ambient,
            double evapIn,
            double evapOut,
            double capacityKw,
            double powerInputKw,
            double copEer,
            String pdfUrl
    ) {}
}
