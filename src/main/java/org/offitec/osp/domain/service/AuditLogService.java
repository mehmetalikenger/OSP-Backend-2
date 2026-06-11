package org.offitec.osp.domain.service;

import org.offitec.osp.domain.entity.AuditLog;
import org.offitec.osp.domain.port.AuditLogRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepositoryPort auditLogRepositoryPort;

    public AuditLogService(AuditLogRepositoryPort auditLogRepositoryPort) {
        this.auditLogRepositoryPort = auditLogRepositoryPort;
    }

    public void logAdminAction(Long adminId, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(adminId != null ? adminId : -1L);
        auditLog.setActorType("ADMIN");
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        
        auditLogRepositoryPort.save(auditLog);
    }
}
