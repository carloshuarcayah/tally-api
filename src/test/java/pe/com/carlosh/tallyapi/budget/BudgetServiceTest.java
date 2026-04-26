package pe.com.carlosh.tallyapi.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private Category category;
    private Budget budget;
    private final Long USER_ID = 1L;
    private final Long CATEGORY_ID = 10L;
    private final Long BUDGET_ID = 100L;

    @BeforeEach
    void setUp() {
        user = new User("test@mail.com", "123456789", "tester", "pass", "Carlos", "Test");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setTier(new pe.com.carlosh.tallyapi.tier.Tier(pe.com.carlosh.tallyapi.tier.TierName.FREE, 5, 4));

        category = new Category("Comida", "Gastos de comida", user);
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);

        budget = new Budget("Presupuesto Mensual", "Descripcion", new BigDecimal("1000.00"), user, category);
        ReflectionTestUtils.setField(budget, "id", BUDGET_ID);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Ok: returns page of budgets with spent amounts")
        void success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Budget> page = new PageImpl<>(List.of(budget));

            when(budgetRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(page);
            when(expenseRepository.sumTotalByBudgetId(BUDGET_ID)).thenReturn(new BigDecimal("250.00"));

            Page<BudgetResponseDTO> result = budgetService.findAll(USER_ID, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(new BigDecimal("250.00"), result.getContent().getFirst().spentAmount());
            assertEquals(new BigDecimal("750.00"), result.getContent().getFirst().remainingAmount());
        }

        @Test
        @DisplayName("Ok: returns empty page when user has no budgets")
        void noBudgets() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Budget> page = new PageImpl<>(List.of());

            when(budgetRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(page);

            Page<BudgetResponseDTO> result = budgetService.findAll(USER_ID, pageable);

            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
            verify(expenseRepository, never()).sumTotalByBudgetId(any());
        }

        @Test
        @DisplayName("Ok: overspent budget shows negative remaining amount")
        void overspent() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Budget> page = new PageImpl<>(List.of(budget));

            when(budgetRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(page);
            when(expenseRepository.sumTotalByBudgetId(BUDGET_ID)).thenReturn(new BigDecimal("1200.00"));

            Page<BudgetResponseDTO> result = budgetService.findAll(USER_ID, pageable);

            BudgetResponseDTO dto = result.getContent().getFirst();
            assertEquals(new BigDecimal("1200.00"), dto.spentAmount());
            assertEquals(new BigDecimal("-200.00"), dto.remainingAmount());
        }

        @Test
        @DisplayName("Ok: each budget gets its own spent amount")
        void multipleBudgets() {
            Pageable pageable = PageRequest.of(0, 10);
            Budget budget2 = new Budget("Otro", "Desc", new BigDecimal("500.00"), user, category);
            ReflectionTestUtils.setField(budget2, "id", 101L);

            Page<Budget> page = new PageImpl<>(List.of(budget, budget2));

            when(budgetRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(page);
            when(expenseRepository.sumTotalByBudgetId(BUDGET_ID)).thenReturn(new BigDecimal("250.00"));
            when(expenseRepository.sumTotalByBudgetId(101L)).thenReturn(new BigDecimal("400.00"));

            Page<BudgetResponseDTO> result = budgetService.findAll(USER_ID, pageable);

            assertEquals(2, result.getTotalElements());
            assertEquals(new BigDecimal("250.00"), result.getContent().get(0).spentAmount());
            assertEquals(new BigDecimal("400.00"), result.getContent().get(1).spentAmount());
            assertEquals(new BigDecimal("750.00"), result.getContent().get(0).remainingAmount());
            assertEquals(new BigDecimal("100.00"), result.getContent().get(1).remainingAmount());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Ok: returns budget with spent amount")
        void success() {
            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));
            when(expenseRepository.sumTotalByBudgetId(BUDGET_ID)).thenReturn(new BigDecimal("300.00"));

            BudgetResponseDTO response = budgetService.findById(BUDGET_ID, USER_ID);

            assertNotNull(response);
            assertEquals("Presupuesto Mensual", response.name());
            assertEquals(new BigDecimal("300.00"), response.spentAmount());
            assertEquals(new BigDecimal("700.00"), response.remainingAmount());
        }

        @Test
        @DisplayName("Error: throws ResourceNotFoundException when not found")
        void throwsResourceNotFound() {
            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> budgetService.findById(BUDGET_ID, USER_ID));
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Ok: with category")
        void withCategory() {
            BudgetRequestDTO req = new BudgetRequestDTO("Nuevo Budget", "Desc", new BigDecimal("500.00"), CATEGORY_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Nuevo Budget")).thenReturn(false);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 200L);
                return b;
            });

            BudgetResponseDTO response = budgetService.create(req, USER_ID);

            assertNotNull(response);
            assertEquals("Nuevo Budget", response.name());
            assertEquals(BigDecimal.ZERO, response.spentAmount());
            assertEquals("Comida", response.categoryName());
            verify(budgetRepository, times(1)).save(any(Budget.class));
        }

        @Test
        @DisplayName("Ok: without category")
        void withoutCategory() {
            BudgetRequestDTO req = new BudgetRequestDTO("Budget Sin Cat", "Desc", new BigDecimal("200.00"), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Budget Sin Cat")).thenReturn(false);
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 201L);
                return b;
            });

            BudgetResponseDTO response = budgetService.create(req, USER_ID);

            assertNotNull(response);
            assertNull(response.categoryId());
            assertNull(response.categoryName());
            verify(categoryRepository, never()).findByIdAndUserIdAndActiveTrue(any(), any());
        }

        @Test
        @DisplayName("Error: throws AlreadyExistsException when name duplicated")
        void throwsAlreadyExists() {
            BudgetRequestDTO req = new BudgetRequestDTO("Presupuesto Mensual", "Desc", new BigDecimal("500.00"), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Presupuesto Mensual")).thenReturn(true);

            AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                    () -> budgetService.create(req, USER_ID));

            assertTrue(ex.getMessage().contains("Presupuesto Mensual"));
            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("Error: throws ResourceNotFoundException when category not found")
        void throwsResourceNotFoundCategory() {
            BudgetRequestDTO req = new BudgetRequestDTO("Budget", "Desc", new BigDecimal("500.00"), 999L);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Budget")).thenReturn(false);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(999L, USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> budgetService.create(req, USER_ID));

            assertEquals("Category not found", ex.getMessage());
            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("Error: throws InvalidOperationException when tier limit reached")
        void throwsInvalidOperationException_WhenTierLimitReached() {
            BudgetRequestDTO req = new BudgetRequestDTO("Quinto", "Desc", new BigDecimal("500.00"), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(budgetRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(4L);

            assertThrows(pe.com.carlosh.tallyapi.core.exception.InvalidOperationException.class,
                    () -> budgetService.create(req, USER_ID));

            verify(budgetRepository, never()).save(any(Budget.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Ok: updates name and category")
        void success() {
            BudgetRequestDTO req = new BudgetRequestDTO("Nombre Actualizado", "Nueva desc", new BigDecimal("800.00"), CATEGORY_ID);

            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));
            when(budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Nombre Actualizado")).thenReturn(false);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
            when(expenseRepository.sumTotalByBudgetId(BUDGET_ID)).thenReturn(new BigDecimal("100.00"));

            BudgetResponseDTO response = budgetService.update(BUDGET_ID, USER_ID, req);

            assertEquals("Nombre Actualizado", response.name());
            assertEquals(new BigDecimal("800.00"), response.maxAmount());
            assertEquals(new BigDecimal("100.00"), response.spentAmount());
        }

        @Test
        @DisplayName("Ok: same name does not trigger duplicate check")
        void sameNameSkipsDuplicateCheck() {
            BudgetRequestDTO req = new BudgetRequestDTO("Presupuesto Mensual", "Otra desc", new BigDecimal("1500.00"), null);

            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));
            when(expenseRepository.sumTotalByBudgetId(BUDGET_ID)).thenReturn(BigDecimal.ZERO);

            BudgetResponseDTO response = budgetService.update(BUDGET_ID, USER_ID, req);

            assertEquals("Presupuesto Mensual", response.name());
            verify(budgetRepository, never()).existsByUserIdAndNameIgnoreCaseAndActiveTrue(any(), any());
        }

        @Test
        @DisplayName("Error: throws AlreadyExistsException when new name already taken")
        void throwsAlreadyExists() {
            BudgetRequestDTO req = new BudgetRequestDTO("Nombre Ocupado", "Desc", new BigDecimal("500.00"), null);

            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));
            when(budgetRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Nombre Ocupado")).thenReturn(true);

            assertThrows(AlreadyExistsException.class,
                    () -> budgetService.update(BUDGET_ID, USER_ID, req));
        }

        @Test
        @DisplayName("Error: throws ResourceNotFoundException when budget not found")
        void throwsResourceNotFound() {
            BudgetRequestDTO req = new BudgetRequestDTO("Name", "Desc", new BigDecimal("500.00"), null);

            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> budgetService.update(BUDGET_ID, USER_ID, req));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Ok: deactivates budget")
        void success() {
            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));

            budgetService.delete(BUDGET_ID, USER_ID);

            assertFalse(budget.isActive());
        }

        @Test
        @DisplayName("Error: throws ResourceNotFoundException when budget not found")
        void throwsResourceNotFound() {
            when(budgetRepository.findByIdAndUserIdAndActiveTrue(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> budgetService.delete(BUDGET_ID, USER_ID));
        }
    }

    @Nested
    @DisplayName("setActive")
    class SetActive {

        @Test
        @DisplayName("Ok: activates budget when active=true")
        void activates() {
            budget.deactivate();
            when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));

            budgetService.setActive(BUDGET_ID, USER_ID, true);

            assertTrue(budget.isActive());
        }

        @Test
        @DisplayName("Ok: deactivates budget when active=false")
        void deactivates() {
            when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.of(budget));

            budgetService.setActive(BUDGET_ID, USER_ID, false);

            assertFalse(budget.isActive());
        }

        @Test
        @DisplayName("Error: throws ResourceNotFoundException when budget not found")
        void throwsResourceNotFound() {
            when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> budgetService.setActive(BUDGET_ID, USER_ID, true));
        }
    }
}