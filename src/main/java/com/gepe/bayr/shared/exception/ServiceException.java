package com.gepe.bayr.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorMessage errorMessage;
    private final Object[] args; // Tempat menyimpan parameter dinamis

    // Constructor lama (jika tidak butuh parameter dinamis)
    public ServiceException(ErrorMessage errorMessage) {
        super(errorMessage.getMessageKey());
        this.status = errorMessage.getStatus();
        this.errorMessage = errorMessage;
        this.args = new Object[0];
    }

    // Constructor baru untuk pesan dinamis
    public ServiceException(ErrorMessage errorMessage, Object... args) {
        super(errorMessage.getMessageKey());
        this.status = errorMessage.getStatus();
        this.errorMessage = errorMessage;
        this.args = args;
    }
}
