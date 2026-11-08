package com.fiap.mechanical_hub.billing_service.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerJwtValidatorTest {

    private static final String SECRET = "test-secret-shared-between-services";

    private CustomerJwtValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new CustomerJwtValidator();
        Field secretField = CustomerJwtValidator.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(validator, SECRET);
    }

    @Test
    @DisplayName("Deve extrair o id do cliente de um token valido com type=customer")
    void validateAndExtractCustomerId_ShouldReturnCustomerId_WhenTokenIsValid() {
        UUID customerId = UUID.randomUUID();
        String token = customerToken(customerId, "customer");

        UUID result = validator.validateAndExtractCustomerId(token);

        assertThat(result).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Deve retornar null quando o claim type nao for customer")
    void validateAndExtractCustomerId_ShouldReturnNull_WhenTypeClaimIsNotCustomer() {
        UUID customerId = UUID.randomUUID();
        String token = customerToken(customerId, "employee");

        UUID result = validator.validateAndExtractCustomerId(token);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar null quando o token for assinado com outro secret")
    void validateAndExtractCustomerId_ShouldReturnNull_WhenSignedWithDifferentSecret() {
        String token = JWT.create()
                .withIssuer("auth-api")
                .withSubject(UUID.randomUUID().toString())
                .withClaim("type", "customer")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(Algorithm.HMAC256("outro-secret-completamente-diferente"));

        UUID result = validator.validateAndExtractCustomerId(token);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar null quando o subject nao for um UUID valido")
    void validateAndExtractCustomerId_ShouldReturnNull_WhenSubjectIsNotAUuid() {
        String token = JWT.create()
                .withIssuer("auth-api")
                .withSubject("nao-e-um-uuid")
                .withClaim("type", "customer")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        UUID result = validator.validateAndExtractCustomerId(token);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve retornar null quando o token estiver expirado")
    void validateAndExtractCustomerId_ShouldReturnNull_WhenTokenIsExpired() {
        String token = JWT.create()
                .withIssuer("auth-api")
                .withSubject(UUID.randomUUID().toString())
                .withClaim("type", "customer")
                .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        UUID result = validator.validateAndExtractCustomerId(token);

        assertThat(result).isNull();
    }

    private String customerToken(UUID customerId, String type) {
        return JWT.create()
                .withIssuer("auth-api")
                .withSubject(customerId.toString())
                .withClaim("type", type)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(900)))
                .sign(Algorithm.HMAC256(SECRET));
    }
}
