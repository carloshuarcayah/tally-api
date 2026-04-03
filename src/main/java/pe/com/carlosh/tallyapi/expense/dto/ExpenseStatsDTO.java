package pe.com.carlosh.tallyapi.expense.dto;

import java.math.BigDecimal;

public record ExpenseStatsDTO(
        BigDecimal total,
        BigDecimal thisMonth,
        long count
) {
}
