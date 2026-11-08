package com.fiap.mechanical_hub.billing_service.infrastructure.http.controllers;

import com.fiap.mechanical_hub.billing_service.application.usecases.PaymentWebhookUseCase;
import com.fiap.mechanical_hub.billing_service.infrastructure.security.MercadoPagoSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publico de proposito (ver SecurityConfiguration) -- a autenticidade e
 * validada pelo x-signature do Mercado Pago, nao por JWT (TASK-035).
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Notificações de pagamento do Mercado Pago")
public class MercadoPagoWebhookResource {

    private final PaymentWebhookUseCase useCase;
    private final MercadoPagoSignatureValidator signatureValidator;

    public MercadoPagoWebhookResource(PaymentWebhookUseCase useCase,
                                       MercadoPagoSignatureValidator signatureValidator) {
        this.useCase = useCase;
        this.signatureValidator = signatureValidator;
    }

    @PostMapping("/mercadopago")
    @Operation(summary = "Receber notificação de pagamento",
            description = "Endpoint público chamado pelo Mercado Pago quando o status de um pagamento muda. "
                    + "Responde em até 22s, conforme exigido por eles. A autenticidade é validada pelo header "
                    + "x-signature (HMAC-SHA256), não por JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificação recebida (processada ou não - ver logs)"),
            @ApiResponse(responseCode = "401", description = "Assinatura inválida")
    })
    public ResponseEntity<Void> receiveNotification(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataIdQueryParam,
            @RequestBody(required = false) WebhookNotificationBody body) {

        String dataId = resolveDataId(dataIdQueryParam, body);

        if (!signatureValidator.isValid(xSignature, xRequestId, dataId)) {
            log.warn("Assinatura x-signature inválida no webhook do Mercado Pago - requisição rejeitada");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            useCase.processNotification(dataId);
        } catch (Exception e) {
            // Nunca deixa isso virar 500: a assinatura ja foi validada (a
            // notificacao e legitima), uma falha interna transitoria
            // (API do Mercado Pago fora do ar, etc.) nao deve fazer eles
            // entrarem num loop de retentativas por nossa causa.
            log.error("Falha ao processar notificação do Mercado Pago (payment id: {})", dataId, e);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * data.id "oficial" vem do query param (?data.id=...) -- o corpo e só
     * um fallback defensivo caso algum modo de integração não mande o
     * query param.
     */
    private String resolveDataId(String queryParam, WebhookNotificationBody body) {
        if (queryParam != null && !queryParam.isBlank()) {
            return queryParam;
        }
        return (body != null && body.data() != null) ? body.data().id() : null;
    }
}
