package com.gepe.bayr.user.api.dto;



import com.gepe.bayr.user.api.type.KycStatusType;
import com.gepe.bayr.user.api.type.UserStatusType;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record UserDto(
        UUID id,
        String email,
        String nickname,
        UserStatusType status,
        KycStatusType kycStatus,
        UserProfileDto userProfile,
        Set<RoleDto> roles
) {
}
