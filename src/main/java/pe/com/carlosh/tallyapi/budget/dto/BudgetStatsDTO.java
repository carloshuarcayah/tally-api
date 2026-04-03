package pe.com.carlosh.tallyapi.budget.dto;

public record BudgetStatsDTO(
        long total,
        long exceeded
) {
}
