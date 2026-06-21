package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.ProjectDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDetailsRepository extends JpaRepository<ProjectDetails, Long> {

    List<ProjectDetails> findByProjectId(Long projectId);
}
