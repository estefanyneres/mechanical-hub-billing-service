package com.fiap.mechanical_hub.billing_service.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerSecurityFilterTest {

    @Mock
    private CustomerJwtValidator customerJwtValidator;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CustomerSecurityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CustomerSecurityFilter(customerJwtValidator);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_ShouldNotAuthenticate_WhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/budgets/abc/approve");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldNotAuthenticate_WhenTokenInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(request.getRequestURI()).thenReturn("/budgets/abc/approve");
        when(customerJwtValidator.validateAndExtractCustomerId("token-invalido")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldAuthenticateAsCustomer_WhenTokenValid() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(request.getRequestURI()).thenReturn("/budgets/abc/approve");
        when(customerJwtValidator.validateAndExtractCustomerId("token-valido")).thenReturn(customerId);

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(customerId);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTOMER");
        verify(filterChain).doFilter(request, response);
    }
}
