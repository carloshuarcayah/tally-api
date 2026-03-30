package pe.com.carlosh.tallyapi.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BudgetRequestDTO(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal maxAmount,
        Long categoryId
) {}