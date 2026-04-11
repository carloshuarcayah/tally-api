package pe.com.carlosh.tallyapi.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.com.carlosh.tallyapi.budget.Budget;
import pe.com.carlosh.tallyapi.budget.BudgetRepository;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.core.exception.InvalidOperationException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseRequestDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseResponseDTO;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private BudgetRepository budgetRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User user;
    private Category categoryFood;
    private Category categoryTransport;
    private Budget budgetFood;
    private final Long USER_ID = 1L;
    private final Long CAT_FOOD_ID = 10L;
    private final Long CAT_TRANS_ID = 20L;
    private final Long BUDGET_FOOD_ID = 100L;

    @BeforeEach
    void setUp() {
        user = new User("test@mail.com", "123", "tester", "pass", "John", "Doe");
        ReflectionTestUtils.setField(user, "id", USER_ID);

        categoryFood = new Category("Food", "On Date", user);
        ReflectionTestUtils.setField(categoryFood, "id", CAT_FOOD_ID);

        categoryTransport = new Category("Transporte", "Pasajes", user);
        ReflectionTestUtils.setField(categoryTransport, "id", CAT_TRANS_ID);

        budgetFood = new Budget("Presupuesto Comida", "Mensual", new BigDecimal("500.00"), user, categoryFood);
        ReflectionTestUtils.setField(budgetFood, "id", BUDGET_FOOD_ID);
    }

    @Test
    @DisplayName("Create Expense - Happy Path: Ok")
    void create_Success() {
        ExpenseRequestDTO req = new ExpenseRequestDTO(new BigDecimal("50.00"), "Breakfast", CAT_FOOD_ID, BUDGET_FOOD_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CAT_FOOD_ID, USER_ID)).thenReturn(Optional.of(categoryFood));
        when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_FOOD_ID, USER_ID)).thenReturn(Optional.of(budgetFood));

        Expense savedExpense = new Expense(req.amount(), req.description(), user, categoryFood, budgetFood);
        ReflectionTestUtils.setField(savedExpense, "id", 999L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        ExpenseResponseDTO response = expenseService.create(req, USER_ID);

        assertNotNull(response);
        assertEquals(new BigDecimal("50.00"), response.amount());
        assertEquals("Food", response.categoryName());
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Create Expense - Error: throws InvalidOperationException When Category Mismatch")
    void create_ThrowsInvalidOperationException() {
        ExpenseRequestDTO req = new ExpenseRequestDTO(new BigDecimal("50.00"), "Taxi", CAT_TRANS_ID, BUDGET_FOOD_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CAT_TRANS_ID, USER_ID)).thenReturn(Optional.of(categoryTransport));
        when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_FOOD_ID, USER_ID)).thenReturn(Optional.of(budgetFood));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> {
            expenseService.create(req, USER_ID);
        });

        assertTrue(ex.getMessage().contains("only accepts expenses from category"));
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    @DisplayName("Delete expense - Error: ResourceNotFound when expense Belongs to another user")
    void delete_ThrowsResourceNotFoundException() {
        Long expenseId = 999L;
        Long hackerId = 2L;

        Expense expense = new Expense(new BigDecimal("10"), "test", user, categoryFood, null);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            expenseService.delete(expenseId, hackerId);
        });

        assertEquals("Expense not found with id: " + expenseId, ex.getMessage());
        verify(expenseRepository, never()).delete(any());
    }
}