package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.repair;

import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceTask;
import com.fiap.mechanical_hub.billing_service.infrastructure.security.ServiceTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairServiceGatewayHttpClientTest {

    private static final String BASE_URL = "http://repair-service.repair-service.svc.cluster.local";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ServiceTokenIssuer tokenIssuer;

    private RepairServiceGatewayHttpClient client;

    @BeforeEach
    void setUp() {
        client = new RepairServiceGatewayHttpClient(restTemplate, tokenIssuer, BASE_URL);
        when(tokenIssuer.issue()).thenReturn("fake-token");
    }

    @Test
    @DisplayName("Deve buscar as tarefas da OS e mapear pra RepairServiceTask")
    void findTasks_ShouldMapResponse() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        var dto = new OrderTaskResponseDto(serviceId, List.of(new OrderTaskResponseDto.MaterialDto(materialId, 2)));

        when(restTemplate.exchange(
                eq(BASE_URL + "/service-orders/" + serviceOrderId + "/tasks"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(OrderTaskResponseDto[].class)))
                .thenReturn(ResponseEntity.ok(new OrderTaskResponseDto[]{dto}));

        List<RepairServiceTask> tasks = client.findTasks(serviceOrderId);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).serviceId()).isEqualTo(serviceId);
        assertThat(tasks.get(0).materials()).containsExactly(new RepairServiceTask.MaterialUsage(materialId, 2));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando a resposta do repair-service vem sem corpo")
    void findTasks_ShouldReturnEmptyList_WhenBodyIsNull() {
        UUID serviceOrderId = UUID.randomUUID();
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(OrderTaskResponseDto[].class)))
                .thenReturn(ResponseEntity.ok(null));

        List<RepairServiceTask> tasks = client.findTasks(serviceOrderId);

        assertThat(tasks).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar o custo de mao de obra de um servico, autenticando com o token do servico")
    void findServiceLaborCost_ShouldReturnLaborCost() {
        UUID serviceId = UUID.randomUUID();
        when(restTemplate.exchange(eq(BASE_URL + "/services/" + serviceId), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(ServiceResponseDto.class)))
                .thenReturn(ResponseEntity.ok(new ServiceResponseDto(new BigDecimal("80.00"))));

        BigDecimal result = client.findServiceLaborCost(serviceId);

        assertThat(result).isEqualTo(new BigDecimal("80.00"));
        verify(tokenIssuer).issue();
    }

    @Test
    @DisplayName("Deve buscar o preco unitario de um material")
    void findMaterialUnitPrice_ShouldReturnUnitPrice() {
        UUID materialId = UUID.randomUUID();
        when(restTemplate.exchange(eq(BASE_URL + "/materials/" + materialId), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(MaterialResponseDto.class)))
                .thenReturn(ResponseEntity.ok(new MaterialResponseDto(new BigDecimal("12.50"))));

        BigDecimal result = client.findMaterialUnitPrice(materialId);

        assertThat(result).isEqualTo(new BigDecimal("12.50"));
    }
}
