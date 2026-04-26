package pe.com.carlosh.tallyapi.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.budget.BudgetRepository;
import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryWithStatsResponseDTO;
import pe.com.carlosh.tallyapi.core.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.core.exception.InvalidOperationException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.ExpenseRepository;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public Page<CategoryResponseDTO> findAll(Long userId, String name, Pageable pageable){
        Page<Category> result = (name == null || name.isBlank())
                ? categoryRepository.findByUserIdAndActiveTrue(userId, pageable)
                : categoryRepository.findByUserIdAndNameContainingIgnoreCaseAndActiveTrue(userId, name, pageable);
        return result.map(CategoryMapper::toResponse);
    }

    public Page<CategoryWithStatsResponseDTO> findAllWithStats(Long userId, String name, Pageable pageable){
        Page<Category> page = (name == null || name.isBlank())
                ? categoryRepository.findByUserIdAndActiveTrue(userId, pageable)
                : categoryRepository.findByUserIdAndNameContainingIgnoreCaseAndActiveTrue(userId, name, pageable);

        if (page.isEmpty()) {
            return page.map(c -> CategoryMapper.toStatsResponse(c, BigDecimal.ZERO, 0L));
        }

        List<Long> ids = page.getContent().stream().map(Category::getId).toList();
        Map<Long, BigDecimal> sums = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : expenseRepository.sumByCategoryIds(userId, ids)) {
            Long catId = (Long) row[0];
            sums.put(catId, (BigDecimal) row[1]);
            counts.put(catId, (Long) row[2]);
        }

        return page.map(c -> CategoryMapper.toStatsResponse(
                c,
                sums.getOrDefault(c.getId(), BigDecimal.ZERO),
                counts.getOrDefault(c.getId(), 0L)
        ));
    }

    public CategoryResponseDTO findById(Long id, Long userId){
        Category category = findActiveOrThrow(id,userId);

        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO req,Long userId){

        User user = userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found with id: "+userId));

        int max = user.getTier().getMaxCategories();
        long current = categoryRepository.countByUserIdAndActiveTrueAndPredefinedFalse(userId);
        if (current >= max) {
            throw new InvalidOperationException(
                    "Has alcanzado el límite de " + max + " categorías para tu plan " + user.getTier().getName().name().toLowerCase()
            );
        }

        if(categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId,req.name())){
            throw new AlreadyExistsException("Category already exists with name: "+req.name());
        }

        Category category = CategoryMapper.toEntity(req,user);

        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponseDTO update(Long id,Long userId,CategoryRequestDTO req){
        Category category = findActiveOrThrow(id,userId);
        ensureNotPredefined(category);

        if(category.nameChanged(req.name())&& categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId,req.name())){
            throw new AlreadyExistsException("Category already exists with name: " + req.name());
        }

        category.update(req.name(), req.description());
        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Category category = findActiveOrThrow(id, userId);
        ensureNotPredefined(category);

        Category fallback = categoryRepository.findByUserIdAndPredefinedTrue(userId)
                .orElseGet(() -> {
                    Category created = new Category(Category.DEFAULT_SYSTEM_NAME, null, category.getUser());
                    created.setPredefined(true);
                    return categoryRepository.save(created);
                });

        expenseRepository.reassignCategory(category, fallback);
        budgetRepository.clearCategory(category);

        categoryRepository.delete(category);
    }

    @Transactional
    public void setActive(Long id, Long userId, boolean active) {
        Category category = findAnyOrThrow(id, userId);
        
        ensureNotPredefined(category);
        
        if (active) {
            category.activate();
        } else {
            category.deactivate();
        }
    }

    private void ensureNotPredefined(Category category) {
        if (category.isPredefined()) {
            throw new InvalidOperationException("Predefined category cannot be modified or deleted");
        }
    }

    private Category findActiveOrThrow(Long id,Long userId){
        return categoryRepository.findByIdAndUserIdAndActiveTrue(id,userId).orElseThrow(()->new ResourceNotFoundException("Category not found or you don't have permission"));
    }

    private Category findAnyOrThrow(Long id,Long userId){
        return categoryRepository.findByIdAndUserId(id,userId).orElseThrow(()->new ResourceNotFoundException("Category not found or you don't have permission"));
    }
}
