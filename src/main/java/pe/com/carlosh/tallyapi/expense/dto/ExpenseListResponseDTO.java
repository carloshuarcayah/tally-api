package pe.com.carlosh.tallyapi.expense.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record ExpenseListResponseDTO(
        Page<ExpenseResponseDTO> expenses,
        BigDecimal total
) {
}
