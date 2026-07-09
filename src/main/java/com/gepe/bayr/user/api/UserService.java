package com.gepe.bayr.user.api;

import com.gepe.bayr.user.api.dto.UserDto;

import java.util.UUID;

public interface UserService {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    UserDto getById(UUID id);

    UserDto getByIdDetails(UUID id);

    UserDto registerUser(String email, String nickname);
}

