package pe.com.carlosh.tallyapi.user.dto;

public record TierInfoDTO(
        String name,
        int maxCategories,
        int maxBudgets
) {
}
