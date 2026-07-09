package com.gepe.bayr.auth.exception;

import com.gepe.bayr.shared.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthError implements ErrorCode {
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "auth.access_denied"),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "auth.account_disabled"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "auth.account_locked"),
    AUTH_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "auth.account_not_found"),
    AUTH_CREDENTIAL_LOCAL_NOT_FOUND(HttpStatus.NOT_FOUND, "auth.credential_local_not_found"),
    AUTH_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "auth.email_already_registered"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "auth.invalid_credentials"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "http.unauthorized"),
    AUTH_TOO_MANY_ATTEMPTS_WITH_DURATION(HttpStatus.TOO_MANY_REQUESTS, "auth.too_many_attempts_with_duration"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "auth.user.disabled"),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "auth.user.suspended");

    private final HttpStatus httpStatus;
    private final String messageKey;

    AuthError(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
