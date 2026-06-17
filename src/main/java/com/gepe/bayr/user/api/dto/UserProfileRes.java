package com.gepe.bayr.user.api.dto;


import java.util.Map;
import java.util.UUID;

public record UserProfileRes(
        UUID userId,
        String avatarUrl,
        String streamKey,
        String messageForSupporter,
        String bio,
        Map<String, String> socialLinks,
        Map<String, String> meta
) { }
