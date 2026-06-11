package org.offitec.osp.domain.port;

import org.offitec.osp.domain.entity.AuditLog;

public interface AuditLogRepositoryPort {
    AuditLog save(AuditLog auditLog);
}
