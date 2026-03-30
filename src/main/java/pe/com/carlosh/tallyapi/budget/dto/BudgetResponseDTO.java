package pe.com.carlosh.tallyapi.budget.dto;

import java.math.BigDecimal;

public record BudgetResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal maxAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        Long categoryId,
        String categoryName,
        Boolean active
) {}