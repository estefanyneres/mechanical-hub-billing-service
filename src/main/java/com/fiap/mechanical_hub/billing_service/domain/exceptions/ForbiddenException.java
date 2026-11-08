package com.fiap.mechanical_hub.billing_service.domain.exceptions;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
