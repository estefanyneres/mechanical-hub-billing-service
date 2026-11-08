package com.fiap.mechanical_hub.billing_service.infrastructure.integrations.core;

import com.fiap.mechanical_hub.billing_service.application.gateways.CoreServiceGateway;
import com.fiap.mechanical_hub.billing_service.application.gateways.ServiceOrderInfo;
import com.fiap.mechanical_hub.billing_service.infrastructure.security.ServiceTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Chamada REST sincrona ao core-service (TASK-034). Mesmo
 * ServiceTokenIssuer da TASK-033 (perfil ADMINISTRATOR) -- os endpoints
 * de /service-orders exigem Administrador ou Mecanico (ver
 * SecurityConfiguration do core-service), e este servico nao tem usuario
 * nenhum pra apresentar.
 */
@Component
public class CoreServiceClient implements CoreServiceGateway {

    private final RestTemplate restTemplate;
    private final ServiceTokenIssuer tokenIssuer;
    private final String coreServiceUrl;

    public CoreServiceClient(RestTemplate restTemplate, ServiceTokenIssuer tokenIssuer,
                              @Value("${core-service.url}") String coreServiceUrl) {
        this.restTemplate = restTemplate;
        this.tokenIssuer = tokenIssuer;
        this.coreServiceUrl = coreServiceUrl;
    }

    @Override
    public ServiceOrderInfo findServiceOrder(UUID serviceOrderId) {
        try {
            var response = restTemplate.exchange(
                    coreServiceUrl + "/service-orders/" + serviceOrderId,
                    HttpMethod.GET, authenticatedRequest(), ServiceOrderDto.class);

            ServiceOrderDto body = response.getBody();
            if (body == null) {
                return null;
            }

            return new ServiceOrderInfo(body.orderNumber(), body.customer().id());
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        }
    }

    private HttpEntity<Void> authenticatedRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenIssuer.issue());
        return new HttpEntity<>(headers);
    }
}
