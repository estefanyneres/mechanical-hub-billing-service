package com.fiap.mechanical_hub.billing_service.infrastructure.http.exception;

import com.fiap.mechanical_hub.billing_service.domain.exceptions.BusinessRuleException;
import com.fiap.mechanical_hub.billing_service.domain.exceptions.ForbiddenException;
import com.fiap.mechanical_hub.billing_service.domain.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleBusinessRuleException() {
        ResponseEntity<Object> response = handler.handleBusinessRuleException(new BusinessRuleException("regra violada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body(response)).containsEntry("message", "regra violada");
    }

    @Test
    void shouldHandleNotFoundException() {
        ResponseEntity<Object> response = handler.handleNotFound(new NotFoundException("não encontrado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(response)).containsEntry("message", "não encontrado");
    }

    @Test
    void shouldHandleForbiddenException() {
        ResponseEntity<Object> response = handler.handleForbidden(new ForbiddenException("acesso negado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(response)).containsEntry("message", "acesso negado");
    }

    @Test
    void shouldHandleHttpMessageNotReadableException() {
        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(mock(HttpMessageNotReadableException.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response)).containsEntry("message", "Corpo da requisição ausente ou com formato inválido");
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        ResponseEntity<Object> response = handler.handleIllegalArgument(new IllegalArgumentException("argumento inválido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response)).containsEntry("message", "argumento inválido");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<Object> response) {
        return (Map<String, Object>) response.getBody();
    }
}
