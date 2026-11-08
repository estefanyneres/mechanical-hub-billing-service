package com.fiap.mechanical_hub.billing_service.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * FAIL_ON_UNKNOWN_PROPERTIES desligado: os DTOs de integracao
 * (RepairServiceGatewayHttpClient, CoreServiceClient) espelham so os
 * campos que este servico precisa, nao a resposta completa do outro
 * lado -- um "new RestTemplate()" puro usa um ObjectMapper Jackson padrao
 * (FAIL_ON_UNKNOWN_PROPERTIES=true), que quebraria a desserializacao
 * assim que o payload real tiver qualquer campo extra.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(objectMapper));
        return restTemplate;
    }
}
