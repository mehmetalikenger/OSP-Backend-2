package org.offitec.osp.infrastructure.repository;

import org.offitec.osp.domain.entity.AuditLog;
import org.offitec.osp.domain.port.AuditLogRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, AuditLogRepositoryPort {
}
