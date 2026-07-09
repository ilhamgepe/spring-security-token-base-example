package com.gepe.bayr.user.api.dto;


import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public record UserProfileDto(
        UUID userId,
        String avatarUrl,
        String streamKey,
        String messageForSupporter,
        String bio,
        Map<String, Object> socialLinks,
        Map<String, Object> meta
) {
}
