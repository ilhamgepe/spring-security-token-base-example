package com.gepe.bayr.auth.internal.delivery.http.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginReq(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 70)
        String password
) {
}
