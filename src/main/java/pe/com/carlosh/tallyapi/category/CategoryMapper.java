package pe.com.carlosh.tallyapi.category;

import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;

public class CategoryMapper {
    public static Category toEntity(CategoryRequestDTO req){
        return new Category(req.name(), req.description());
    }

    public static CategoryResponseDTO toResponse(Category category){
        return new CategoryResponseDTO(category.getId(), category.getName(), category.getDescription(), category.isActive());
    }
}
