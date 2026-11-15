package com.fiap.mechanical_hub.billing_service.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Autenticacao stateless so pros endpoints de aprovacao/recusa do
 * orcamento (os unicos protegidos neste servico) -- o webhook do Mercado
 * Pago (TASK-035) e publico de proposito, validado por x-signature, nao
 * por JWT. Principal e o proprio UUID do cliente (sem UserDetails: este
 * servico nao tem entidade Customer).
 */
@Slf4j
@Component
public class CustomerSecurityFilter extends OncePerRequestFilter {

    private final CustomerJwtValidator customerJwtValidator;

    public CustomerSecurityFilter(CustomerJwtValidator customerJwtValidator) {
        this.customerJwtValidator = customerJwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = recoverToken(request);
        log.warn("[DEBUG-JWT] path={} tokenPresente={}", request.getRequestURI(), token != null);

        if (token != null) {
            UUID customerId = customerJwtValidator.validateAndExtractCustomerId(token);
            log.warn("[DEBUG-JWT] customerId resolvido={}", customerId);
            if (customerId != null) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        customerId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.warn("[DEBUG-JWT] authentication setada, authorities={}", authentication.getAuthorities());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
