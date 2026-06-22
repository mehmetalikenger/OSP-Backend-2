package org.offitec.osp.presentation.dto;

import lombok.Getter;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;

/**
 * One row of the admin Projects table. The table is flattened to one row per
 * project-detail (i.e. per unit evaluation), so a project with several units
 * appears as several rows. A project with no details yet appears as a single row
 * with the unit/document columns left null.
 *
 * `username` is the owning user's username and `company` is the project's company
 * (entered when the project was created); the admin UI shows them together.
 */
@Getter
public class AdminProjectRowDTO {

    private final Long projectId;
    private final Long detailId;
    private final String projectName;
    private final String username;
    private final String company;
    private final String country;
    private final String category;   // CHILLER / HEAT_PUMP
    private final String type;       // AW / WW
    private final String model;
    private final String documentUrl;

    // Constructor used by the JPQL projection in ProjectRepository.findAllAdminRows().
    // Enums are stored as their names (the shape the frontend maps to labels).
    public AdminProjectRowDTO(Long projectId, String projectName, String username, String company,
                              String country, UnitCategory category, UnitTypeEnum type, String model,
                              String documentUrl, Long detailId) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.username = username;
        this.company = company;
        this.country = country;
        this.category = category != null ? category.name() : null;
        this.type = type != null ? type.name() : null;
        this.model = model;
        this.documentUrl = documentUrl;
        this.detailId = detailId;
    }
}
