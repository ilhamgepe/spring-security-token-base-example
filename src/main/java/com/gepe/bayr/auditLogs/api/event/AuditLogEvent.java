package com.gepe.bayr.auditLogs.api.event;

import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public record AuditLogEvent(
        UUID actorUserId,
        String actorEmail,
        UUID requestId,
        String entityType,
        String entityId,
        String action,
        Map<String, Object> oldData,
        Map<String, Object> newData,
        Map<String, Object> metadata
) {
}
