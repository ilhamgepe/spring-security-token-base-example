package com.gepe.bayr.auditLogs.internal.repo;

import com.gepe.bayr.auditLogs.internal.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
}
