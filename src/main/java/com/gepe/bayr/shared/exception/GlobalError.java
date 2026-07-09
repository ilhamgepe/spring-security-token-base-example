package com.gepe.bayr.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalError implements ErrorCode {

    // ##################################################
    // AUTH
    // ##################################################
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "auth.access_denied"),
    AUTH_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "auth.account_disabled"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "auth.account_locked"),
    AUTH_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "auth.account_not_found"),
    AUTH_CREDENTIAL_LOCAL_NOT_FOUND(HttpStatus.NOT_FOUND, "auth.credential_local_not_found"),
    AUTH_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "auth.email_already_registered"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "auth.invalid_credentials"),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "http.unauthorized"),
    AUTH_TOO_MANY_ATTEMPTS_WITH_DURATION(HttpStatus.TOO_MANY_REQUESTS, "auth.too_many_attempts_with_duration"),

    // ##################################################
    // DATABASE
    // ##################################################
    DB_CONSTRAINT_VIOLATION(HttpStatus.CONFLICT, "db.constraint_violation"),
    DB_DATA_INTEGRITY(HttpStatus.CONFLICT, "db.data_integrity"),
    DB_DUPLICATE_ENTRY(HttpStatus.CONFLICT, "db.duplicate_entry"),

    // ##################################################
    // DONATION
    // ##################################################
    DONATION_CLOSED(HttpStatus.BAD_REQUEST, "donation.closed"),
    DONATION_MAXIMUM_AMOUNT(HttpStatus.BAD_REQUEST, "donation.maximum_amount"),
    DONATION_MINIMUM_AMOUNT(HttpStatus.BAD_REQUEST, "donation.minimum_amount"),
    DONATION_NOT_FOUND(HttpStatus.NOT_FOUND, "donation.not_found"),

    // ##################################################
    // EXCEPTION (generic, fallback)
    // ##################################################
    EXCEPTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "exception.access_denied"),
    EXCEPTION_ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "exception.entity_not_found"),
    EXCEPTION_ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "exception.illegal_argument"),
    EXCEPTION_ILLEGAL_STATE(HttpStatus.CONFLICT, "exception.illegal_state"),
    EXCEPTION_OPTIMISTIC_LOCK(HttpStatus.CONFLICT, "exception.optimistic_lock"),

    // ##################################################
    // FILE
    // ##################################################
    FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "file.invalid_type"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "file.not_found"),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "file.too_large"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "file.upload_failed"),

    // ##################################################
    // HTTP
    // ##################################################
    HTTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "http.too_many_attempts"),


    // ##################################################
    // KYC
    // ##################################################
    KYC_NOT_VERIFIED(HttpStatus.FORBIDDEN, "kyc.not_verified"),
    KYC_PENDING(HttpStatus.BAD_REQUEST, "kyc.pending"),
    KYC_REJECTED(HttpStatus.BAD_REQUEST, "kyc.rejected"),

    // ##################################################
    // PAYMENT
    // ##################################################
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "payment.already_paid"),
    PAYMENT_EXPIRED(HttpStatus.BAD_REQUEST, "payment.expired"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "payment.failed"),
    PAYMENT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "payment.invalid_status"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "payment.not_found"),
    PAYMENT_GATEWAY_NOT_FOUND(HttpStatus.NOT_FOUND, "payment_gateway.not_found"),

    // ##################################################
    // SYSTEM
    // ##################################################
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "system.error"),
    SYSTEM_MAINTENANCE(HttpStatus.SERVICE_UNAVAILABLE, "system.maintenance"),
    SYSTEM_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "system.timeout"),

    // ##################################################
    // VALIDATION
    // ##################################################
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "validation.failed");


    private final HttpStatus httpStatus;
    private final String messageKey;

    GlobalError(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}