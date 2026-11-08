package com.fiap.mechanical_hub.billing_service.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoSignatureValidatorTest {

    private static final String SECRET = "fake-webhook-secret";

    private final MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(SECRET);

    @Test
    @DisplayName("Deve aceitar uma assinatura valida, com request-id presente")
    void isValid_ShouldAcceptValidSignature_WithRequestId() {
        String dataId = "123456789";
        String requestId = "req-abc";
        String ts = "1704908010";
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String header = "ts=" + ts + ",v1=" + hmacSha256Hex(manifest, SECRET);

        assertThat(validator.isValid(header, requestId, dataId)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar uma assinatura valida quando x-request-id esta ausente (par omitido do manifesto)")
    void isValid_ShouldAcceptValidSignature_WithoutRequestId() {
        String dataId = "123456789";
        String ts = "1704908010";
        String manifest = "id:" + dataId + ";ts:" + ts + ";";
        String header = "ts=" + ts + ",v1=" + hmacSha256Hex(manifest, SECRET);

        assertThat(validator.isValid(header, null, dataId)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar quando o hash foi calculado com o secret errado")
    void isValid_ShouldReject_WhenSecretIsWrong() {
        String dataId = "123456789";
        String ts = "1704908010";
        String manifest = "id:" + dataId + ";ts:" + ts + ";";
        String header = "ts=" + ts + ",v1=" + hmacSha256Hex(manifest, "wrong-secret");

        assertThat(validator.isValid(header, null, dataId)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar quando o data.id foi adulterado depois de assinado")
    void isValid_ShouldReject_WhenDataIdTampered() {
        String ts = "1704908010";
        String manifest = "id:123456789;ts:" + ts + ";";
        String header = "ts=" + ts + ",v1=" + hmacSha256Hex(manifest, SECRET);

        assertThat(validator.isValid(header, null, "999999999")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar quando o header x-signature esta ausente")
    void isValid_ShouldReject_WhenHeaderMissing() {
        assertThat(validator.isValid(null, null, "123456789")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar quando o data.id esta ausente")
    void isValid_ShouldReject_WhenDataIdMissing() {
        assertThat(validator.isValid("ts=123,v1=abc", null, null)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar um header mal formado (sem ts ou v1)")
    void isValid_ShouldReject_WhenHeaderMalformed() {
        assertThat(validator.isValid("ts=123", null, "123456789")).isFalse();
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
