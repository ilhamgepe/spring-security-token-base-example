package com.gepe.bayr.shared.web.response;

public record ValidationError(
        String field,
        String message
) {
}