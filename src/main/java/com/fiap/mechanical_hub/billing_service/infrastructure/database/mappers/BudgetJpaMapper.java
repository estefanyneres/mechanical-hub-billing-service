package com.fiap.mechanical_hub.billing_service.infrastructure.database.mappers;

import com.fiap.mechanical_hub.billing_service.domain.entities.Budget;
import com.fiap.mechanical_hub.billing_service.infrastructure.database.models.BudgetModel;

public class BudgetJpaMapper {

    private BudgetJpaMapper() {}

    public static BudgetModel toJpa(Budget budget) {
        return new BudgetModel(budget.getId(), budget.getServiceOrderId(), budget.getTotalValue(),
                budget.getStatus(), budget.getMercadoPagoPreferenceId(), budget.getPaymentLink(),
                budget.getRejectionReason(), budget.getCreatedAt());
    }

    public static Budget toDomain(BudgetModel model) {
        return new Budget(model.getId(), model.getServiceOrderId(), model.getTotalValue(), model.getStatus(),
                model.getMercadoPagoPreferenceId(), model.getPaymentLink(), model.getRejectionReason(),
                model.getCreatedAt());
    }
}
