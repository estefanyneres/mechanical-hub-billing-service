package com.fiap.mechanical_hub.billing_service.domain.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
