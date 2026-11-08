package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.mercadopago;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoClientTest {

    private static final String ACCESS_TOKEN = "fake-access-token";
    private static final String API_GATEWAY_URL = "https://abc123.execute-api.us-east-1.amazonaws.com";

    @Mock
    private RestTemplate restTemplate;

    private MercadoPagoClient client;

    @BeforeEach
    void setUp() {
        client = new MercadoPagoClient(restTemplate, ACCESS_TOKEN, API_GATEWAY_URL);
    }

    @Test
    @DisplayName("Deve criar a preferencia e mapear id/init_point da resposta")
    void createPreference_ShouldMapResponse() {
        UUID serviceOrderId = UUID.randomUUID();
        var responseDto = new PreferenceResponseDto("pref-123", "https://mercadopago.com/pay/pref-123");
        when(restTemplate.exchange(eq("https://api.mercadopago.com/checkout/preferences"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(PreferenceResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        MercadoPagoPreference result = client.createPreference("OS-202601-0001", serviceOrderId,
                new BigDecimal("150.00"));

        assertThat(result.preferenceId()).isEqualTo("pref-123");
        assertThat(result.paymentLink()).isEqualTo("https://mercadopago.com/pay/pref-123");
    }

    @Test
    @DisplayName("Deve montar o corpo da preferencia com external_reference, notification_url e back_urls")
    void createPreference_ShouldBuildCorrectRequestBody() {
        UUID serviceOrderId = UUID.randomUUID();
        var responseDto = new PreferenceResponseDto("pref-123", "https://mercadopago.com/pay/pref-123");
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(eq("https://api.mercadopago.com/checkout/preferences"), eq(HttpMethod.POST),
                captor.capture(), eq(PreferenceResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        client.createPreference("OS-202601-0001", serviceOrderId, new BigDecimal("150.00"));

        Map<String, Object> body = captor.getValue().getBody();
        assertThat(body.get("external_reference")).isEqualTo(serviceOrderId.toString());
        assertThat(body.get("notification_url")).isEqualTo(API_GATEWAY_URL + "/webhooks/mercadopago");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("title")).isEqualTo("Ordem de serviço OS-202601-0001");
        assertThat(items.get(0).get("unit_price")).isEqualTo(new BigDecimal("150.00"));

        assertThat(captor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Deve buscar o pagamento e mapear status/motivo/external_reference da resposta")
    void getPayment_ShouldMapResponse() {
        var responseDto = new PaymentResponseDto("approved", "accredited", UUID.randomUUID().toString());
        when(restTemplate.exchange(eq("https://api.mercadopago.com/v1/payments/999"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(PaymentResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        MercadoPagoPaymentInfo result = client.getPayment("999");

        assertThat(result.status()).isEqualTo("approved");
        assertThat(result.statusDetail()).isEqualTo("accredited");
        assertThat(result.externalReference()).isEqualTo(responseDto.externalReference());
    }

    @Test
    @DisplayName("Deve autenticar a busca do pagamento com o access token")
    void getPayment_ShouldAuthenticateWithAccessToken() {
        var responseDto = new PaymentResponseDto("approved", "accredited", UUID.randomUUID().toString());
        ArgumentCaptor<HttpEntity<Void>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(eq("https://api.mercadopago.com/v1/payments/999"), eq(HttpMethod.GET),
                captor.capture(), eq(PaymentResponseDto.class)))
                .thenReturn(ResponseEntity.ok(responseDto));

        client.getPayment("999");

        assertThat(captor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
    }
}
