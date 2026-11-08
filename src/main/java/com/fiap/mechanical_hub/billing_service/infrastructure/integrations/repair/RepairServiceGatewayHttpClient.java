package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.repair;

import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.RepairServiceTask;
import com.fiap.mechanical_hub.billing_service.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Chamada REST sincrona ao repair-service (TASK-033). Cada chamada monta
 * o proprio token de servico (ServiceTokenIssuer) porque os endpoints do
 * repair-service exigem JWT com perfil Administrador/Mecanico (TASK-023)
 * -- este servico nao tem usuario nenhum pra apresentar, so a chave
 * compartilhada entre os servicos.
 */
@Component
public class RepairServiceGatewayHttpClient implements RepairServiceGateway {

    private final RestTemplate restTemplate;
    private final ServiceTokenIssuer tokenIssuer;
    private final String repairServiceUrl;

    public RepairServiceGatewayHttpClient(RestTemplate restTemplate, ServiceTokenIssuer tokenIssuer,
                                           @Value("${repair-service.url}") String repairServiceUrl) {
        this.restTemplate = restTemplate;
        this.tokenIssuer = tokenIssuer;
        this.repairServiceUrl = repairServiceUrl;
    }

    @Override
    public List<RepairServiceTask> findTasks(UUID serviceOrderId) {
        var response = restTemplate.exchange(
                repairServiceUrl + "/service-orders/" + serviceOrderId + "/tasks",
                HttpMethod.GET, authenticatedRequest(), OrderTaskResponseDto[].class);

        OrderTaskResponseDto[] body = response.getBody();
        if (body == null) {
            return List.of();
        }

        return java.util.Arrays.stream(body)
                .map(dto -> new RepairServiceTask(
                        dto.serviceId(),
                        dto.materials().stream()
                                .map(m -> new RepairServiceTask.MaterialUsage(m.materialId(), m.quantity()))
                                .toList()))
                .toList();
    }

    @Override
    public BigDecimal findServiceLaborCost(UUID serviceId) {
        var response = restTemplate.exchange(
                repairServiceUrl + "/services/" + serviceId,
                HttpMethod.GET, authenticatedRequest(), ServiceResponseDto.class);
        return response.getBody() != null ? response.getBody().laborCost() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal findMaterialUnitPrice(UUID materialId) {
        var response = restTemplate.exchange(
                repairServiceUrl + "/materials/" + materialId,
                HttpMethod.GET, authenticatedRequest(), MaterialResponseDto.class);
        return response.getBody() != null ? response.getBody().unitPrice() : BigDecimal.ZERO;
    }

    private HttpEntity<Void> authenticatedRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenIssuer.issue());
        return new HttpEntity<>(headers);
    }
}
