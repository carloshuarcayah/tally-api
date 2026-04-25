package pe.com.carlosh.tallyapi.category.dto;

import java.math.BigDecimal;

public record CategoryWithStatsResponseDTO(
        Long id,
        String name,
        String description,
        Boolean predefined,
        BigDecimal spentAmount,
        long expenseCount
) {
}
