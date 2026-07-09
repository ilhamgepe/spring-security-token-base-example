package com.gepe.bayr.user.exception;

import com.gepe.bayr.shared.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RoleError implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "role.not_found");

    private final HttpStatus httpStatus;
    private final String messageKey;

    RoleError(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
