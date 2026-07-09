package com.gepe.bayr.user.exception;

import com.gepe.bayr.shared.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserError implements ErrorCode {
    DISABLED(HttpStatus.FORBIDDEN, "user.disabled"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "user.email_already_exists"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "user.nickname_already_exists"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "user.not_found"),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "user.phone_already_exists"),
    SUSPENDED(HttpStatus.FORBIDDEN, "user.suspended");

    private final HttpStatus httpStatus;
    private final String messageKey;

    UserError(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
