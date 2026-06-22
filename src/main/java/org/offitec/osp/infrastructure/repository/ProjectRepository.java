package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.Project;
import org.offitec.osp.presentation.dto.AdminProjectRowDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    // Admin Projects table: every project across all users, flattened to one row per
    // project-detail (unit evaluation). LEFT JOINs so a project with no details still
    // shows up (unit/document columns null). One query, no per-project lazy loads.
    @Query("""
            SELECT new org.offitec.osp.presentation.dto.AdminProjectRowDTO(
                p.id, p.name, u.username, p.company, p.country,
                unit.category, unit.unitType, unit.model, pd.pdfUrl, pd.id)
            FROM Project p
            JOIN p.user u
            LEFT JOIN ProjectDetails pd ON pd.project = p
            LEFT JOIN pd.unit unit
            ORDER BY p.createdAt DESC, pd.id
            """)
    List<AdminProjectRowDTO> findAllAdminRows();
}
