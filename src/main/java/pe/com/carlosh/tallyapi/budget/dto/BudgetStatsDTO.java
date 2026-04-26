package pe.com.carlosh.tallyapi.budget.dto;

public record BudgetStatsDTO(
        long total,
        long limit,
        long exceeded
) {
}
