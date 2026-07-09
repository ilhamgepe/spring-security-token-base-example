package com.gepe.bayr.auditLogs.internal.event;

import com.gepe.bayr.auditLogs.api.event.AuditLogEvent;
import com.gepe.bayr.auditLogs.internal.entity.AuditLog;
import com.gepe.bayr.auditLogs.internal.repo.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
class AuditLogEventListener {
    private final AuditLogRepo auditLogRepo;

    @ApplicationModuleListener
    public void on(AuditLogEvent event) {
        AuditLog log = AuditLog.builder()
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

        auditLogRepo.save(log);
    }
}