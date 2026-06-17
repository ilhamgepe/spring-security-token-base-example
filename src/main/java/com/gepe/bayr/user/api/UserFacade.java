package com.gepe.bayr.user.api;

import com.gepe.bayr.user.api.dto.UserRes;

import java.util.UUID;

public interface UserFacade {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    UserRes getById(UUID id);
    UserRes getByIdDetails(UUID id);

    UserRes registerUser(String email, String nickname);
}
