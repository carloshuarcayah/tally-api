package pe.com.carlosh.tallyapi.expense;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.budget.Budget;
import pe.com.carlosh.tallyapi.budget.BudgetRepository;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.core.exception.InvalidOperationException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseListResponseDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseRequestDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseResponseDTO;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetRepository  budgetRepository;

    public Page<ExpenseResponseDTO> findByUser(Long userId, Pageable pageable) {
        return expenseRepository.findByUserIdAndActiveTrue(userId, pageable).map(ExpenseMapper::toResponse);
    }

    //SOLO EXPENSES ACTIVAS
    public ExpenseListResponseDTO findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable) {
        Page<ExpenseResponseDTO> expenses;
        BigDecimal total;

        if(categoryId!=null){
            expenses = expenseRepository.findByUserIdAndActiveTrueAndCategoryId(userId, categoryId, pageable).map(ExpenseMapper::toResponse);
            total = expenseRepository.sumTotalByUserIdAndCategoryId(userId, categoryId);
        }
        else{
            expenses = expenseRepository.findByUserId(userId,pageable).map(ExpenseMapper::toResponse);
            total = expenseRepository.sumTotalByUserId(userId);
        }

        return new ExpenseListResponseDTO(expenses,total);
    }

    public ExpenseResponseDTO findById(Long id, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);
        return ExpenseMapper.toResponse(expense);
    }


    @Transactional
    public ExpenseResponseDTO create(ExpenseRequestDTO req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Category category = findCategoryOrThrow(req.categoryId(),userId);

        Budget budget = findBudgetOrNull(req.budgetId(),userId);

        validateBudgetCategory(budget, category);

        Expense expense = new Expense(req.amount(),req.description(),user,category,budget);

        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponseDTO update(Long id, ExpenseRequestDTO req, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);

        Category category = findCategoryOrThrow(req.categoryId(),userId);

        Budget budget = findBudgetOrNull(req.budgetId(),userId);

        validateBudgetCategory(budget, category);

        expense.update(req.amount(), req.description(), category, budget);
        return ExpenseMapper.toResponse(expense);
    }

    public ExpenseListResponseDTO findByFilters(Long userId, Long categoryId, Long budgetId, Pageable pageable) {
        Page<ExpenseResponseDTO> expenses;
        BigDecimal total;

        if (budgetId != null) {
            expenses = expenseRepository.findByUserIdAndActiveTrueAndBudgetId(userId, budgetId, pageable)
                    .map(ExpenseMapper::toResponse);
            total = expenseRepository.sumTotalByBudgetId(budgetId);
        } else if (categoryId != null) {
            expenses = expenseRepository.findByUserIdAndActiveTrueAndCategoryId(userId, categoryId, pageable)
                    .map(ExpenseMapper::toResponse);
            total = expenseRepository.sumTotalByUserIdAndCategoryId(userId, categoryId);
        } else {
            expenses = expenseRepository.findByUserIdAndActiveTrue(userId, pageable)
                    .map(ExpenseMapper::toResponse);
            total = expenseRepository.sumTotalByUserId(userId);
        }

        return new ExpenseListResponseDTO(expenses, total);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Expense expense = findByIdAndUserOrThrow(id, userId);
        expenseRepository.delete(expense);
    }

    private void validateBudgetCategory(Budget budget, Category category) {
        if (budget == null) return;
        if (budget.getCategory() == null) return;

        if (!budget.getCategory().getId().equals(category.getId())) {
            throw new InvalidOperationException(
                    "This budget only accepts expenses from category: " + budget.getCategory().getName()
            );
        }
    }

    private Category findCategoryOrThrow(Long categoryId, Long userId){
        return categoryRepository.findByIdAndUserIdAndActiveTrue(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private Budget findBudgetOrNull(Long budgetId,Long userId){
        if (budgetId == null) return null;
        return budgetRepository.findByIdAndUserIdAndActiveTrue(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));
    }

    private Expense findByIdAndUserOrThrow(Long id, Long userId) {
        return expenseRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
    }

    public BigDecimal getTotalByUser(Long userId) {
        return expenseRepository.sumTotalByUserId(userId);
    }

    public BigDecimal getTotalByCategory(Long userId, Long categoryId) {
        return expenseRepository.sumTotalByUserIdAndCategoryId(userId,categoryId);
    }
}