package com.gepe.bayr.auth.internal.delivery.http.res;


import com.gepe.bayr.user.api.dto.UserDto;

public record LoginRes(
        String accessToken,
        String refreshToken,
        UserDto user
) {
}
