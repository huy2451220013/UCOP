package com.ucop.repository;

import com.ucop.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT l FROM AuditLog l WHERE (:entityType IS NULL OR l.entityType = :entityType) " +
           "AND (:action IS NULL OR l.action = :action) ORDER BY l.createdAt DESC")
    List<AuditLog> filterLogs(@Param("entityType") String entityType, @Param("action") String action);
}
