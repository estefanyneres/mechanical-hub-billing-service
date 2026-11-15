package com.fiap.mechanical_hub.billing_service.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Valida o header x-signature das notificacoes do webhook do Mercado Pago
 * (TASK-035).
 *
 * Algoritmo confirmado na documentacao oficial (Mercado Pago Developers,
 * secao "Webhooks - Notifications", consultada em 2026-10):
 *   - header x-signature no formato "ts=&lt;timestamp&gt;,v1=&lt;hash&gt;"
 *   - manifesto: "id:&lt;data.id&gt;;request-id:&lt;x-request-id&gt;;ts:&lt;ts&gt;;"
 *     -- cada par e OMITIDO do manifesto se o valor correspondente nao
 *     estiver presente na notificacao (ex: sem x-request-id, o manifesto
 *     fica so "id:...;ts:...;")
 *   - data.id vem do QUERY PARAM da requisicao (?data.id=...), confirmado
 *     nos exemplos oficiais (request.query['data.id'] / request.args.get)
 *     -- NAO do campo data.id do corpo JSON, mesmo o corpo tambem carregando
 *     esse valor
 *   - hash = HMAC-SHA256(secret, manifesto) em hexadecimal minusculo,
 *     comparado em tempo constante contra v1
 *   - secret e uma credencial PROPRIA do webhook (painel Mercado Pago:
 *     Suas integracoes > Webhooks > Configurar notificacoes > revelar a
 *     chave secreta) -- DIFERENTE do access token usado pra chamar a API
 *     deles (MercadoPagoClient)
 *
 * NAO confirmado na documentacao consultada: se o data.id precisa ser
 * normalizado (ex: minusculas) antes de entrar no manifesto -- os
 * exemplos oficiais usam o valor bruto do query param, sem nenhuma
 * transformacao mencionada, e e isso que esta implementado aqui. Vale
 * re-validar contra uma notificacao real de teste antes de confiar nisso
 * em produção.
 */
@Slf4j
@Component
public class MercadoPagoSignatureValidator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String webhookSecret;

    public MercadoPagoSignatureValidator(@Value("${mercadopago.webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * @param xSignatureHeader valor bruto do header x-signature ("ts=...,v1=...")
     * @param xRequestId       valor do header x-request-id, ou null se ausente
     * @param dataId           valor do query param data.id da notificacao
     */
    public boolean isValid(String xSignatureHeader, String xRequestId, String dataId) {
        if (xSignatureHeader == null || xSignatureHeader.isBlank() || dataId == null || dataId.isBlank()) {
            return false;
        }

        Map<String, String> parts = parseHeader(xSignatureHeader);
        String ts = parts.get("ts");
        String expectedHash = parts.get("v1");
        if (ts == null || expectedHash == null) {
            return false;
        }

        String manifest = buildManifest(dataId, xRequestId, ts);
        String computedHash = hmacSha256Hex(manifest, webhookSecret);
        log.warn("[DEBUG-MP] manifest='{}' secretLen={} computed={} expected={}",
                manifest, webhookSecret == null ? -1 : webhookSecret.length(), computedHash, expectedHash);

        return constantTimeEquals(computedHash, expectedHash);
    }

    private Map<String, String> parseHeader(String header) {
        Map<String, String> result = new HashMap<>();
        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }

    private String buildManifest(String dataId, String requestId, String ts) {
        StringBuilder manifest = new StringBuilder();
        manifest.append("id:").append(dataId).append(";");
        if (requestId != null && !requestId.isBlank()) {
            manifest.append("request-id:").append(requestId).append(";");
        }
        manifest.append("ts:").append(ts).append(";");
        return manifest.toString();
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular o HMAC-SHA256 da assinatura do webhook", e);
        }
    }

    private boolean constantTimeEquals(String computed, String expected) {
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
