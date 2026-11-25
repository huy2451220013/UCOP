package com.ucop.service.impl;

import com.ucop.entity.AuditLog;
import com.ucop.repository.AuditLogRepository;
import com.ucop.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String action, String entityType, Long entityId, String performedBy,
                    String oldValue, String newValue, String detail) {
        AuditLog log = new AuditLog(
                action,
                entityType,
                entityId,
                performedBy != null ? performedBy : "SYSTEM",
                oldValue,
                newValue,
                detail
        );
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(String entityType, String action) {
        String et = (entityType == null || entityType.isBlank()) ? null : entityType.trim();
        String act = (action == null || action.isBlank()) ? null : action.trim();
        if (et == null && act == null) {
            return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
        }
        return auditLogRepository.filterLogs(et, act);
    }
}
