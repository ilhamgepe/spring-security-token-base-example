package com.gepe.bayr.auditLogs.internal.service;

import com.gepe.bayr.auditLogs.api.event.AuditLogEvent;
import com.gepe.bayr.auditLogs.internal.entity.AuditLog;
import com.gepe.bayr.auditLogs.internal.repo.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class AuditLogService {
    private final AuditLogRepo auditLogRepo;

    public void publish(AuditLogEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .actorUserId(event.actorUserId())
                .actorEmail(event.actorEmail())
                .requestId(event.requestId())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .action(event.action())
                .oldData(event.oldData())
                .newData(event.newData())
                .metadata(event.metadata())
                .createdAt(Instant.now())
                .build();
        auditLogRepo.save(auditLog);
    }
}