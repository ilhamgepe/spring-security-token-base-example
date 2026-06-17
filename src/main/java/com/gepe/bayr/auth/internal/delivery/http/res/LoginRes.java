package com.gepe.bayr.auth.internal.delivery.http.res;

import com.gepe.bayr.user.api.dto.UserRes;

public record LoginRes(
    String accessToken,
    String refreshToken,
    UserRes user
) {
}
