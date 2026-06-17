package com.gepe.bayr.user.api.dto;


import com.gepe.bayr.user.api.type.KycStatus;
import com.gepe.bayr.user.api.type.UserStatus;

import java.util.Set;
import java.util.UUID;

public record UserRes(
        UUID id,
        String email,
        String nickname,
        UserStatus status,
        KycStatus kycStatus,
        UserProfileRes userProfile,
        Set<RoleRes> roles
) { }
