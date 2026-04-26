package pe.com.carlosh.tallyapi.user.dto;

import pe.com.carlosh.tallyapi.budget.dto.BudgetStatsDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryStatsDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseStatsDTO;

public record UserStatsDTO(
        TierInfoDTO tier,
        BudgetStatsDTO budgetStats,
        ExpenseStatsDTO expenseStats,
        CategoryStatsDTO categoryStats
) {
}
