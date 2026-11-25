package com.ucop.service;

import com.ucop.entity.AuditLog;
import java.util.List;

public interface AuditService {
    void log(String action, String entityType, Long entityId, String performedBy,
             String oldValue, String newValue, String detail);
    List<AuditLog> getRecentLogs(String entityType, String action);
}
