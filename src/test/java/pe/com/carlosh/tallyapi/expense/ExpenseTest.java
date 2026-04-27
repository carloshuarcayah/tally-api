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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseListResponseDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseRequestDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseResponseDTO;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;
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

    // ── findById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("FindById - Ok: returns expense when user is owner")
    void findById_Success() {
        Long expenseId = 500L;
        Expense expense = new Expense(new BigDecimal("20.00"), "Almuerzo", user, categoryFood, null);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        ExpenseResponseDTO response = expenseService.findById(expenseId, USER_ID);

        assertNotNull(response);
        assertEquals("Almuerzo", response.description());
    }

    @Test
    @DisplayName("FindById - Error: throws ResourceNotFoundException when expense belongs to another user")
    void findById_ThrowsResourceNotFoundException() {
        Long expenseId = 500L;
        Expense expense = new Expense(new BigDecimal("20.00"), "Almuerzo", user, categoryFood, null);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        assertThrows(ResourceNotFoundException.class,
                () -> expenseService.findById(expenseId, 99L));
    }

    // ── create (additional) ──────────────────────────────────────────────

    @Test
    @DisplayName("Create Expense - Error: throws ResourceNotFoundException when category not found")
    void create_ThrowsResourceNotFoundException_CategoryNotFound() {
        ExpenseRequestDTO req = new ExpenseRequestDTO(new BigDecimal("50.00"), "Test", 999L, null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(999L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> expenseService.create(req, USER_ID));

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    @DisplayName("Create Expense - Error: throws ResourceNotFoundException when budget not found")
    void create_ThrowsResourceNotFoundException_BudgetNotFound() {
        ExpenseRequestDTO req = new ExpenseRequestDTO(new BigDecimal("50.00"), "Test", CAT_FOOD_ID, 999L);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CAT_FOOD_ID, USER_ID)).thenReturn(Optional.of(categoryFood));
        when(budgetRepository.findByIdAndUserIdAndActiveTrue(999L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> expenseService.create(req, USER_ID));

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Update Expense - Ok: updates amount and description")
    void update_Success() {
        Long expenseId = 500L;
        Expense expense = new Expense(new BigDecimal("10.00"), "Viejo", user, categoryFood, null);
        ExpenseRequestDTO req = new ExpenseRequestDTO(new BigDecimal("75.00"), "Actualizado", CAT_FOOD_ID, null);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CAT_FOOD_ID, USER_ID)).thenReturn(Optional.of(categoryFood));

        ExpenseResponseDTO response = expenseService.update(expenseId, req, USER_ID);

        assertEquals(new BigDecimal("75.00"), response.amount());
        assertEquals("Actualizado", response.description());
    }

    @Test
    @DisplayName("Update Expense - Error: throws ResourceNotFoundException when expense not found")
    void update_ThrowsResourceNotFoundException() {
        Long expenseId = 500L;
        ExpenseRequestDTO req = new ExpenseRequestDTO(new BigDecimal("10.00"), "Test", CAT_FOOD_ID, null);

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> expenseService.update(expenseId, req, USER_ID));
    }

    // ── delete (additional) ──────────────────────────────────────────────

    @Test
    @DisplayName("Delete Expense - Ok: deletes when user is owner")
    void delete_Success() {
        Long expenseId = 500L;
        Expense expense = new Expense(new BigDecimal("10.00"), "Test", user, categoryFood, null);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        expenseService.delete(expenseId, USER_ID);

        verify(expenseRepository, times(1)).delete(expense);
    }

    // ── findByFilters ────────────────────────────────────────────────────

    @Test
    @DisplayName("FindByFilters - Ok: no filters returns all user expenses")
    void findByFilters_NoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of());

        when(expenseRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(page);
        when(expenseRepository.sumTotalByUserId(USER_ID)).thenReturn(new BigDecimal("100.00"));

        ExpenseListResponseDTO result = expenseService.findByFilters(USER_ID, null, null, pageable);

        assertEquals(new BigDecimal("100.00"), result.total());
    }

    @Test
    @DisplayName("FindByFilters - Ok: with budgetId filters by budget")
    void findByFilters_WithBudgetId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of());

        when(expenseRepository.findByUserIdAndActiveTrueAndBudgetId(USER_ID, BUDGET_FOOD_ID, pageable)).thenReturn(page);
        when(expenseRepository.sumTotalByBudgetIdAndUserId(BUDGET_FOOD_ID, USER_ID)).thenReturn(new BigDecimal("50.00"));

        ExpenseListResponseDTO result = expenseService.findByFilters(USER_ID, null, BUDGET_FOOD_ID, pageable);

        assertEquals(new BigDecimal("50.00"), result.total());
        verify(expenseRepository, never()).findByUserIdAndActiveTrue(any(), any());
    }

    @Test
    @DisplayName("FindByFilters - Ok: with categoryId filters by category")
    void findByFilters_WithCategoryId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of());

        when(expenseRepository.findByUserIdAndActiveTrueAndCategoryId(USER_ID, CAT_FOOD_ID, pageable)).thenReturn(page);
        when(expenseRepository.sumTotalByUserIdAndCategoryId(USER_ID, CAT_FOOD_ID)).thenReturn(new BigDecimal("30.00"));

        ExpenseListResponseDTO result = expenseService.findByFilters(USER_ID, CAT_FOOD_ID, null, pageable);

        assertEquals(new BigDecimal("30.00"), result.total());
    }

    // ── totals ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GetTotal - Ok: returns total spent by user when categoryId is null")
    void getTotal_NoCategory_Success() {
        when(expenseRepository.sumTotalByUserId(USER_ID)).thenReturn(new BigDecimal("1500.00"));

        assertEquals(new BigDecimal("1500.00"), expenseService.getTotal(USER_ID, null));
    }

    @Test
    @DisplayName("GetTotal - Ok: returns total spent by user and category")
    void getTotal_WithCategory_Success() {
        when(expenseRepository.sumTotalByUserIdAndCategoryId(USER_ID, CAT_FOOD_ID)).thenReturn(new BigDecimal("500.00"));

        assertEquals(new BigDecimal("500.00"), expenseService.getTotal(USER_ID, CAT_FOOD_ID));
    }
}