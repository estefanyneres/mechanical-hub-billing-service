package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.core;

import com.fiap.mechanical_hub.billing_service.application.gateways.ServiceOrderInfo;
import com.fiap.mechanical_hub.billing_service.infrastructure.security.ServiceTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreServiceClientTest {

    private static final String BASE_URL = "http://core-service.core-service.svc.cluster.local";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ServiceTokenIssuer tokenIssuer;

    private CoreServiceClient client;

    @BeforeEach
    void setUp() {
        client = new CoreServiceClient(restTemplate, tokenIssuer, BASE_URL);
    }

    @Test
    @DisplayName("Deve buscar a OS e mapear pra ServiceOrderInfo")
    void findServiceOrder_ShouldMapResponse() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(tokenIssuer.issue()).thenReturn("fake-token");
        var dto = new ServiceOrderDto("OS-202601-0001", new ServiceOrderDto.CustomerDto(customerId));
        when(restTemplate.exchange(
                eq(BASE_URL + "/service-orders/" + serviceOrderId),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(ServiceOrderDto.class)))
                .thenReturn(ResponseEntity.ok(dto));

        ServiceOrderInfo result = client.findServiceOrder(serviceOrderId);

        assertThat(result.orderNumber()).isEqualTo("OS-202601-0001");
        assertThat(result.customerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Deve retornar null quando o core-service responder 404")
    void findServiceOrder_ShouldReturnNull_WhenNotFound() {
        UUID serviceOrderId = UUID.randomUUID();
        when(tokenIssuer.issue()).thenReturn("fake-token");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(ServiceOrderDto.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(),
                        new byte[0], null));

        ServiceOrderInfo result = client.findServiceOrder(serviceOrderId);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar null quando a resposta vier sem corpo")
    void findServiceOrder_ShouldReturnNull_WhenBodyIsNull() {
        UUID serviceOrderId = UUID.randomUUID();
        when(tokenIssuer.issue()).thenReturn("fake-token");
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(ServiceOrderDto.class)))
                .thenReturn(ResponseEntity.ok(null));

        ServiceOrderInfo result = client.findServiceOrder(serviceOrderId);

        assertThat(result).isNull();
    }
}
