package pe.com.carlosh.tallyapi.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.core.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public Page<CategoryResponseDTO> findByActiveTrue(Long userId, Pageable pageable){
        return categoryRepository.findByUserIdAndActiveTrue(userId, pageable)
                .map(CategoryMapper::toResponse);
    }

    public CategoryResponseDTO findById(Long id, Long userId){
        Category category = findActiveOrThrow(id,userId);

        return CategoryMapper.toResponse(category);
    }

//    public Page<CategoryResponseDTO> findByActiveFalse(Pageable pageable){
//        return categoryRepository.findByActiveFalse(pageable).map(CategoryMapper::toResponse);
//    }

    public Page<CategoryResponseDTO> findByName(Long userId,String name, Pageable pageable){
        return categoryRepository.findByUserIdAndNameContainingIgnoreCaseAndActiveTrue(userId, name,pageable).map(CategoryMapper::toResponse);
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO req,Long userId){

        User user = userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found with id: "+userId));

        if(categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId,req.name())){
            throw new AlreadyExistsException("Category already exists with name: "+req.name());
        }

        Category category = CategoryMapper.toEntity(req,user);

        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO update(Long id,Long userId,CategoryRequestDTO req){
        Category category = findActiveOrThrow(id,userId);

        if(category.nameChanged(req.name())&& categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId,req.name())){
            throw new AlreadyExistsException("Category already exists with name: " + req.name());
        }

        category.update(req.name(), req.description());
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public  CategoryResponseDTO delete(Long id,Long userId){
        Category category = findActiveOrThrow(id,userId);


        category.deactivate();
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public  CategoryResponseDTO enable(Long id,Long userId){
        Category category = findAnyOrThrow(id,userId);

            category.activate();
            return CategoryMapper.toResponse(category);

    }

    private Category findActiveOrThrow(Long id,Long userId){
        return categoryRepository.findByIdAndUserIdAndActiveTrue(id,userId).orElseThrow(()->new ResourceNotFoundException("Category not found or you don't have permission"));
    }

    private Category findAnyOrThrow(Long id,Long userId){
        return categoryRepository.findByIdAndUserId(id,userId).orElseThrow(()->new ResourceNotFoundException("Category not found or you don't have permission"));
    }
}
