package pe.com.carlosh.tallyapi.budget;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.budget.dto.BudgetRequestDTO;
import pe.com.carlosh.tallyapi.budget.dto.BudgetResponseDTO;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.core.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.ExpenseRepository;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public Page<BudgetResponseDTO> findAll(Long userId, Pageable pageable) {
        return budgetRepository.findByUserIdAndActiveTrue(userId, pageable)
                .map(budget -> {
                    BigDecimal spent = expenseRepository.sumTotalByBudgetId(budget.getId());
                    return BudgetMapper.toResponse(budget, spent);
                });
    }

    public BudgetResponseDTO findById(Long id, Long userId) {
        Budget budget = findActiveOrThrow(id, userId);
        BigDecimal spent = expenseRepository.sumTotalByBudgetId(budget.getId());
        return BudgetMapper.toResponse(budget, spent);
    }

    @Transactional
    public BudgetResponseDTO create(BudgetRequestDTO req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId, req.name())) {
            throw new AlreadyExistsException("Budget already exists with name: " + req.name());
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findByIdAndUserIdAndActiveTrue(req.categoryId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Budget budget = new Budget(req.name(), req.description(), req.maxAmount(), user, category);
        budgetRepository.save(budget);

        return BudgetMapper.toResponse(budget, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetResponseDTO update(Long id, Long userId, BudgetRequestDTO req) {
        Budget budget = findActiveOrThrow(id, userId);

        if (budget.nameChanged(req.name()) &&
                budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId, req.name())) {
            throw new AlreadyExistsException("Budget already exists with name: " + req.name());
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findByIdAndUserIdAndActiveTrue(req.categoryId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        budget.update(req.name(), req.description(), req.maxAmount(), category);
        BigDecimal spent = expenseRepository.sumTotalByBudgetId(budget.getId());

        return BudgetMapper.toResponse(budget, spent);
    }

    @Transactional
    public BudgetResponseDTO delete(Long id, Long userId) {
        Budget budget = findActiveOrThrow(id, userId);
        budget.deactivate();
        BigDecimal spent = expenseRepository.sumTotalByBudgetId(budget.getId());
        return BudgetMapper.toResponse(budget, spent);
    }

    @Transactional
    public BudgetResponseDTO enable(Long id, Long userId) {
        Budget budget = findAnyOrThrow(id, userId);
        budget.activate();
        BigDecimal spent = expenseRepository.sumTotalByBudgetId(budget.getId());
        return BudgetMapper.toResponse(budget, spent);
    }

    private Budget findActiveOrThrow(Long id, Long userId) {
        return budgetRepository.findByIdAndUserIdAndActiveTrue(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found or you don't have permission"));
    }

    private Budget findAnyOrThrow(Long id, Long userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found or you don't have permission"));
    }
}