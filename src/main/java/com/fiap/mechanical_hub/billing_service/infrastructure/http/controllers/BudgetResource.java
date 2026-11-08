package com.fiap.mechanical_hub.billing_service.infrastructure.http.controllers;

import com.fiap.mechanical_hub.billing_service.application.dto.RejectBudgetRequest;
import com.fiap.mechanical_hub.billing_service.application.usecases.BudgetApprovalUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Aprovacao/recusa do orcamento pelo CLIENTE (TASK-034) -- autenticado
 * via CustomerSecurityFilter, nunca pelo corpo da requisicao (o id do
 * cliente vem do JWT, nao e informado pelo chamador).
 */
@RestController
@RequestMapping("/budgets")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orçamentos", description = "Aprovação/recusa do orçamento pelo cliente")
public class BudgetResource {

    private final BudgetApprovalUseCase useCase;

    public BudgetResource(BudgetApprovalUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{serviceOrderId}/approve")
    @Operation(summary = "Aprovar orçamento",
            description = "Cliente aprova o orçamento de uma OS, gerando a preferência de pagamento no Mercado "
                    + "Pago e retornando o link de pagamento. Requer autenticação do cliente (JWT emitido pelo "
                    + "auth-lambda via CPF).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orçamento aprovado, link de pagamento retornado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "OS não encontrada ou não pertence ao cliente autenticado"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado pra essa OS"),
            @ApiResponse(responseCode = "422", description = "Orçamento já foi decidido anteriormente")
    })
    public ResponseEntity<Map<String, String>> approve(
            @PathVariable UUID serviceOrderId,
            @AuthenticationPrincipal UUID customerId) {
        String paymentLink = useCase.approve(serviceOrderId, customerId);
        return ResponseEntity.ok(Map.of("paymentLink", paymentLink));
    }

    @PostMapping("/{serviceOrderId}/reject")
    @Operation(summary = "Recusar orçamento",
            description = "Cliente recusa o orçamento de uma OS. Requer autenticação do cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Orçamento recusado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "OS não encontrada ou não pertence ao cliente autenticado"),
            @ApiResponse(responseCode = "404", description = "Orçamento não encontrado pra essa OS"),
            @ApiResponse(responseCode = "422", description = "Orçamento já foi decidido anteriormente")
    })
    public ResponseEntity<Void> reject(
            @PathVariable UUID serviceOrderId,
            @AuthenticationPrincipal UUID customerId,
            @RequestBody RejectBudgetRequest request) {
        useCase.reject(serviceOrderId, customerId, request.motivo());
        return ResponseEntity.noContent().build();
    }
}
