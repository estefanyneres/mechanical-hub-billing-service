package com.fiap.mechanical_hub.billing_service.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * So decodifica/valida o JWT do CLIENTE (emitido pelo auth-lambda via CPF
 * -- shape diferente do TokenService do core-service: "sub" e o id do
 * cliente, claim "type"="customer"). Mesmo JWT_SECRET compartilhado. Sem
 * consulta a banco de clientes (este servico nao tem esse dado -
 * ADR-001) -- confia no "sub" depois de verificar a assinatura, mesmo
 * espirito do JwtValidator do repair-service pro claim "profile".
 */
@Component
public class CustomerJwtValidator {

    private static final String CUSTOMER_TYPE = "customer";

    @Value("${api.security.jwt.secret}")
    private String secret;

    public UUID validateAndExtractCustomerId(String token) {
        DecodedJWT decoded = decode(token);
        if (decoded == null) {
            return null;
        }

        String type = decoded.getClaim("type").asString();
        if (!CUSTOMER_TYPE.equals(type)) {
            return null;
        }

        try {
            return UUID.fromString(decoded.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private DecodedJWT decode(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}
