package com.gepe.bayr.auth.api.event;

import java.util.UUID;

public record ReplayAttackDetectedEvent(UUID userId) {
}