package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chamada REST direta na API de Preferences do Mercado Pago (TASK-034) --
 * sem SDK, mesmo padrao do resto do projeto (RestTemplate puro). Sem
 * back_urls reais: este projeto nao tem frontend pro cliente, os campos
 * existem so porque a API do Mercado Pago exige.
 */
@Component
public class MercadoPagoClient {

    private static final String PREFERENCES_URL = "https://api.mercadopago.com/checkout/preferences";
    private static final String PAYMENTS_URL = "https://api.mercadopago.com/v1/payments";

    private final RestTemplate restTemplate;
    private final String accessToken;
    private final String apiGatewayUrl;

    public MercadoPagoClient(RestTemplate restTemplate,
                              @Value("${mercadopago.access-token}") String accessToken,
                              @Value("${api-gateway.public-url}") String apiGatewayUrl) {
        this.restTemplate = restTemplate;
        this.accessToken = accessToken;
        this.apiGatewayUrl = apiGatewayUrl;
    }

    public MercadoPagoPreference createPreference(String orderNumber, UUID serviceOrderId, BigDecimal totalValue) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", "Ordem de serviço " + orderNumber);
        item.put("quantity", 1);
        item.put("unit_price", totalValue);
        item.put("currency_id", "BRL");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", serviceOrderId.toString());
        body.put("notification_url", apiGatewayUrl + "/webhooks/mercadopago");
        body.put("back_urls", Map.of(
                "success", apiGatewayUrl + "/payment/success",
                "failure", apiGatewayUrl + "/payment/failure",
                "pending", apiGatewayUrl + "/payment/pending"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = restTemplate.exchange(PREFERENCES_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers), PreferenceResponseDto.class);

        PreferenceResponseDto responseBody = response.getBody();
        return new MercadoPagoPreference(responseBody.id(), responseBody.initPoint());
    }

    /**
     * TASK-035: busca o pagamento real depois do webhook chegar -- o
     * payload da notificacao so traz o id, nunca o status de verdade.
     */
    public MercadoPagoPaymentInfo getPayment(String paymentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        var response = restTemplate.exchange(PAYMENTS_URL + "/" + paymentId, HttpMethod.GET,
                new HttpEntity<>(headers), PaymentResponseDto.class);

        PaymentResponseDto body = response.getBody();
        return new MercadoPagoPaymentInfo(body.status(), body.statusDetail(), body.externalReference());
    }
}
