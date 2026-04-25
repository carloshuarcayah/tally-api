package pe.com.carlosh.tallyapi.category;

import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryWithStatsResponseDTO;
import pe.com.carlosh.tallyapi.user.User;

import java.math.BigDecimal;

public class CategoryMapper {
    public static Category toEntity(CategoryRequestDTO req, User user){
        return new Category(req.name(), req.description(), user);
    }

    public static CategoryResponseDTO toResponse(Category category){
        return new CategoryResponseDTO(category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getUser().getId(),
                category.isPredefined()
        );
    }

    public static CategoryWithStatsResponseDTO toStatsResponse(Category category, BigDecimal spentAmount, long expenseCount){
        return new CategoryWithStatsResponseDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isPredefined(),
                spentAmount,
                expenseCount
        );
    }
}
