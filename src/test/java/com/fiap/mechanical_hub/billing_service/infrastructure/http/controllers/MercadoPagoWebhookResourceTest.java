package com.fiap.mechanical_hub.billing_service.infrastructure.http.controllers;

import com.fiap.mechanical_hub.billing_service.application.usecases.PaymentWebhookUseCase;
import com.fiap.mechanical_hub.billing_service.infrastructure.security.MercadoPagoSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookResourceTest {

    @Mock
    private PaymentWebhookUseCase useCase;
    @Mock
    private MercadoPagoSignatureValidator signatureValidator;

    private MercadoPagoWebhookResource resource;

    @BeforeEach
    void setUp() {
        resource = new MercadoPagoWebhookResource(useCase, signatureValidator);
    }

    @Test
    @DisplayName("Deve retornar 401 e nao processar quando a assinatura for invalida")
    void receiveNotification_ShouldReturn401_WhenSignatureInvalid() {
        when(signatureValidator.isValid("ts=1,v1=bad", "req-1", "999")).thenReturn(false);

        ResponseEntity<Void> result = resource.receiveNotification("ts=1,v1=bad", "req-1", "999", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(useCase, org.mockito.Mockito.never()).processNotification(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Deve retornar 200 e processar quando a assinatura for valida, usando o data.id do query param")
    void receiveNotification_ShouldReturn200AndProcess_UsingQueryParamDataId() {
        when(signatureValidator.isValid("ts=1,v1=ok", "req-1", "999")).thenReturn(true);

        ResponseEntity<Void> result = resource.receiveNotification("ts=1,v1=ok", "req-1", "999", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(useCase).processNotification("999");
    }

    @Test
    @DisplayName("Deve usar o data.id do corpo como fallback quando o query param estiver ausente")
    void receiveNotification_ShouldFallBackToBodyDataId_WhenQueryParamMissing() {
        var body = new WebhookNotificationBody("payment", "payment.created", new WebhookNotificationBody.WebhookData("777"));
        when(signatureValidator.isValid("ts=1,v1=ok", null, "777")).thenReturn(true);

        ResponseEntity<Void> result = resource.receiveNotification("ts=1,v1=ok", null, null, body);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(useCase).processNotification("777");
    }

    @Test
    @DisplayName("Deve retornar 200 mesmo quando o processamento lancar uma exception inesperada")
    void receiveNotification_ShouldReturn200_WhenProcessingThrows() {
        when(signatureValidator.isValid("ts=1,v1=ok", "req-1", "999")).thenReturn(true);
        doThrow(new RuntimeException("Mercado Pago API indisponível"))
                .when(useCase).processNotification("999");

        ResponseEntity<Void> result = resource.receiveNotification("ts=1,v1=ok", "req-1", "999", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
