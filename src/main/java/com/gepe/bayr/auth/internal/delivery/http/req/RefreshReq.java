package com.gepe.bayr.auth.internal.delivery.http.req;

import jakarta.validation.constraints.NotBlank;

public record RefreshReq(
        @NotBlank
        String refreshToken
) {
}
