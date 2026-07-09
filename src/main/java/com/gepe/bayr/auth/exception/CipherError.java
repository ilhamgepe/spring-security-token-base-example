package com.gepe.bayr.auth.exception;

import com.gepe.bayr.shared.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CipherError implements ErrorCode {
    CIPHER_DECRYPT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.cipher.decrypt-failed"),
    CIPHER_ENCRYPT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.cipher.encrypt-failed"),
    CIPHER_INVALID_FORMAT(HttpStatus.INTERNAL_SERVER_ERROR, "error.cipher.invalid-format"),
    CIPHER_KEY_ENCRYPTION_SECRET_LENGTH(HttpStatus.INTERNAL_SERVER_ERROR, "error.cipher.key-encryption-secret-length");

    private final HttpStatus httpStatus;
    private final String messageKey;

    CipherError(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
