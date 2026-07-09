package com.gepe.bayr.user.api.dto;

import lombok.Builder;

@Builder
public record RoleDto(
        Long id,
        String code,
        String name,
        String description
) {
}
