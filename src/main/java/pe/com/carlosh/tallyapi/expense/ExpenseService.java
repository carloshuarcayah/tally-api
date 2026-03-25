package pe.com.carlosh.tallyapi.expense;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseRequestDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseResponseDTO;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public Page<ExpenseResponseDTO> findByUser(Long userId, Pageable pageable) {
        return expenseRepository.findByUserId(userId, pageable).map(ExpenseMapper::toResponse);
    }

    public Page<ExpenseResponseDTO> findByUserAndCategory(Long userId, Long categoryId, Pageable pageable) {
        return expenseRepository.findByUserIdAndCategoryId(userId, categoryId, pageable).map(ExpenseMapper::toResponse);
    }

    public ExpenseResponseDTO findById(Long id, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);
        return ExpenseMapper.toResponse(expense);
    }

    @Transactional
    public ExpenseResponseDTO create(ExpenseRequestDTO req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Category category = categoryRepository.findByIdAndActiveTrue(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + req.categoryId()));

        Expense expense = new Expense(req.amount(), req.description(), req.expenseDate(), user, category);

        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponseDTO update(Long id, ExpenseRequestDTO req, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);

        Category category = categoryRepository.findByIdAndActiveTrue(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + req.categoryId()));

        expense.update(req.amount(), req.description(), req.expenseDate(), category);
        return ExpenseMapper.toResponse(expense);
    }

    @Transactional
    public ExpenseResponseDTO delete(Long id, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);
        expense.deactivate();
        return ExpenseMapper.toResponse(expense);
    }

    @Transactional
    public ExpenseResponseDTO enable(Long id, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);
        expense.activate();
        return ExpenseMapper.toResponse(expense);
    }


    private Expense findByIdAndUserOrThrow(Long id, Long userId) {
        return expenseRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
    }
}