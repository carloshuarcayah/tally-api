package pe.com.carlosh.tallyapi.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.exception.ResourceNotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Page<CategoryResponseDTO> findByActiveTrue(Pageable pageable){
        return categoryRepository.findByActiveTrue(pageable).map(CategoryMapper::toResponse);
    }

    public Page<CategoryResponseDTO> findByActiveFalse(Pageable pageable){
        return categoryRepository.findByActiveFalse(pageable).map(CategoryMapper::toResponse);
    }

    public Page<CategoryResponseDTO> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable){
        return categoryRepository.findByNameContainingIgnoreCaseAndActiveTrue(name,pageable).map(CategoryMapper::toResponse);
    }

    public CategoryResponseDTO findById(Long id){
        return  CategoryMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO req){

        if(categoryRepository.existsByNameIgnoreCase(req.name())){
            throw new AlreadyExistsException("Category already exists with name: "+req.name());
        }

        Category category = CategoryMapper.toEntity(req);

        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO req){
        Category category = findActiveOrThrow(id);
        if(category.nameChanged(req.name())&& categoryRepository.existsByNameIgnoreCase(req.name())){
            throw new AlreadyExistsException("Category already exists with name: " + req.name());
        }

        category.update(req.name(), req.description());
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public  CategoryResponseDTO delete(Long id){
        Category category = findActiveOrThrow(id);
        category.deactivate();
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public  CategoryResponseDTO enable(Long id){
        Category category = findAnyOrThrow(id);
        category.activate();
        return CategoryMapper.toResponse(category);
    }

    private Category findActiveOrThrow(Long id){
        return categoryRepository.findByIdAndActiveTrue(id).orElseThrow(()->new ResourceNotFoundException("Category not found with id: "+id));
    }

    private Category findAnyOrThrow(Long id){
        return categoryRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Category not found with id: "+id));
    }
}
