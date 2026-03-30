package pe.com.carlosh.tallyapi.budget;

import pe.com.carlosh.tallyapi.budget.dto.BudgetResponseDTO;
import java.math.BigDecimal;

public class BudgetMapper {

    public static BudgetResponseDTO toResponse(Budget budget, BigDecimal spentAmount) {
        BigDecimal remaining = budget.getMaxAmount().subtract(spentAmount);

        return new BudgetResponseDTO(
                budget.getId(),
                budget.getName(),
                budget.getDescription(),
                budget.getMaxAmount(),
                spentAmount,
                remaining,
                budget.getCategory() != null ? budget.getCategory().getId() : null,
                budget.getCategory() != null ? budget.getCategory().getName() : null,
                budget.isActive()
        );
    }
}