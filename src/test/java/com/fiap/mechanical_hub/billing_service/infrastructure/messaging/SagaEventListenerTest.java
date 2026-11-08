package com.fiap.mechanical_hub.billing_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SagaEventListenerTest {

    @Mock
    private BudgetUseCase budgetUseCase;

    private SagaEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SagaEventListener(budgetUseCase, new ObjectMapper());
    }

    @Test
    @DisplayName("Deve extrair o serviceOrderId do payload e delegar pro BudgetUseCase")
    void onMessage_ShouldDelegateToBudgetUseCase() throws Exception {
        UUID serviceOrderId = UUID.randomUUID();
        String payload = """
                {"serviceOrderId": "%s", "orderNumber": "OS-1", "requestDescription": "desc"}
                """.formatted(serviceOrderId);
        Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), new MessageProperties());

        listener.onMessage(message);

        verify(budgetUseCase).generateBudget(serviceOrderId);
    }
}
