package pe.com.carlosh.tallyapi.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user1;
    private Category category1;
    private final Long USER_ID = 1L;
    private final Long CATEGORY_ID = 99L;

    @BeforeEach
    void setUp() {
        user1 = new User("prueba@gmail.com", "968574659", "usuarioprueba", "usuario123", "usuario", "prueba");
        ReflectionTestUtils.setField(user1, "id", USER_ID);
        user1.setTier(new pe.com.carlosh.tallyapi.tier.Tier(pe.com.carlosh.tallyapi.tier.TierName.FREE, 5, 4));
    }

    @Test
    @DisplayName("Create category - Ok")
    void create_Success() {
        CategoryRequestDTO req = new CategoryRequestDTO("Nueva Cat", "Desc");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user1));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, "Nueva Cat")).thenReturn(false);

        Category savedCat = new Category(req.name(), req.description(), user1);
        ReflectionTestUtils.setField(savedCat, "id", 100L);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

        CategoryResponseDTO response = categoryService.create(req, USER_ID);

        assertNotNull(response);
        assertEquals("Nueva Cat", response.name());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Create Category - Error: throws InvalidOperationException when tier limit reached")
    void create_ThrowsInvalidOperationException_WhenTierLimitReached() {
        CategoryRequestDTO req = new CategoryRequestDTO("Sexta", "Desc");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user1));
        when(categoryRepository.countByUserIdAndActiveTrueAndPredefinedFalse(USER_ID)).thenReturn(5L);

        assertThrows(InvalidOperationException.class, () -> categoryService.create(req, USER_ID));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Create Category - Error: throws AlreadyExistsException")
    void create_ThrowsAlreadyExistsException() {
        CategoryRequestDTO req = new CategoryRequestDTO("Category 1", "Error Test");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user1));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(USER_ID, req.name())).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> {
            categoryService.create(req, USER_ID);
        });

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Update Category - Error: throws ResourceNotFoundException when User Is Not Owner")
    void update_ThrowsResourceNotFound() {
        CategoryRequestDTO req = new CategoryRequestDTO("Update Name", "Desc");
        Long wrongUserId = 2L;
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, wrongUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.update(CATEGORY_ID, wrongUserId, req);
        });
    }

    @Test
    @DisplayName("Delete Category - Ok: reassigns expenses to fallback, clears budgets, hard-deletes category")
    void delete_Success() {
        Category category = new Category("Cat A", "Desc", user1);
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);

        Category fallback = new Category(Category.DEFAULT_SYSTEM_NAME, null, user1);
        fallback.setPredefined(true);
        ReflectionTestUtils.setField(fallback, "id", 500L);

        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.findByUserIdAndPredefinedTrue(USER_ID)).thenReturn(Optional.of(fallback));

        categoryService.delete(CATEGORY_ID, USER_ID);

        verify(expenseRepository, times(1)).reassignCategory(category, fallback);
        verify(budgetRepository, times(1)).clearCategory(category);
        verify(categoryRepository, times(1)).delete(category);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Delete Category - Ok: creates fallback when predefined missing")
    void delete_CreatesFallbackWhenMissing() {
        Category category = new Category("Cat A", "Desc", user1);
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);

        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.findByUserIdAndPredefinedTrue(USER_ID)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.delete(CATEGORY_ID, USER_ID);

        verify(categoryRepository, times(1)).save(argThat(c ->
                c.isPredefined() && Category.DEFAULT_SYSTEM_NAME.equals(c.getName()) && c.getUser() == user1));
        verify(expenseRepository, times(1)).reassignCategory(eq(category), any(Category.class));
        verify(budgetRepository, times(1)).clearCategory(category);
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Update Category - Error: throws InvalidOperationException when category is system")
    void update_ThrowsInvalidOperationException_WhenSystemCategory() {
        Category systemCategory = new Category(Category.DEFAULT_SYSTEM_NAME, null, user1);
        systemCategory.setPredefined(true);
        ReflectionTestUtils.setField(systemCategory, "id", CATEGORY_ID);

        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(systemCategory));

        CategoryRequestDTO req = new CategoryRequestDTO("Nuevo Nombre", "Desc");

        assertThrows(InvalidOperationException.class,
                () -> categoryService.update(CATEGORY_ID, USER_ID, req));

        assertEquals(Category.DEFAULT_SYSTEM_NAME, systemCategory.getName());
        verify(categoryRepository, never()).existsByUserIdAndNameIgnoreCaseAndActiveTrue(anyLong(), anyString());
    }

    @Test
    @DisplayName("Delete Category - Error: throws InvalidOperationException when category is system")
    void delete_ThrowsInvalidOperationException_WhenSystemCategory() {
        Category systemCategory = new Category(Category.DEFAULT_SYSTEM_NAME, null, user1);
        systemCategory.setPredefined(true);
        ReflectionTestUtils.setField(systemCategory, "id", CATEGORY_ID);

        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(systemCategory));

        assertThrows(InvalidOperationException.class,
                () -> categoryService.delete(CATEGORY_ID, USER_ID));

        assertTrue(systemCategory.isActive());
        verify(categoryRepository, never()).delete(any(Category.class));
        verify(expenseRepository, never()).reassignCategory(any(), any());
        verify(budgetRepository, never()).clearCategory(any());
    }

    @Test
    @DisplayName("SetActive Category - Error: throws InvalidOperationException when deactivating system category")
    void setActive_ThrowsInvalidOperationException_WhenDeactivatingSystemCategory() {
        Category systemCategory = new Category(Category.DEFAULT_SYSTEM_NAME, null, user1);
        systemCategory.setPredefined(true);
        ReflectionTestUtils.setField(systemCategory, "id", CATEGORY_ID);

        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(systemCategory));

        assertThrows(InvalidOperationException.class,
                () -> categoryService.setActive(CATEGORY_ID, USER_ID, false));

        assertTrue(systemCategory.isActive());
    }

    @Test
    @DisplayName("SetActive Category - Error: throws InvalidOperationException when activating system category")
    void setActive_ThrowsInvalidOperationException_WhenActivatingSystemCategory() {
        Category systemCategory = new Category(Category.DEFAULT_SYSTEM_NAME, null, user1);
        systemCategory.setPredefined(true);
        ReflectionTestUtils.setField(systemCategory, "id", CATEGORY_ID);

        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(systemCategory));

        assertThrows(InvalidOperationException.class,
                () -> categoryService.setActive(CATEGORY_ID, USER_ID, true));
    }

    @Test
    @DisplayName("FindAllWithStats - Ok: maps spentAmount and expenseCount per category")
    void findAllWithStats_MapsStats() {
        Category catA = new Category("Comida", null, user1);
        ReflectionTestUtils.setField(catA, "id", 10L);
        Category catB = new Category("Transporte", null, user1);
        ReflectionTestUtils.setField(catB, "id", 20L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(catA, catB), pageable, 2);

        when(categoryRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(page);
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{10L, new BigDecimal("150.00"), 3L});
        when(expenseRepository.sumByCategoryIds(eq(USER_ID), anyCollection())).thenReturn(rows);

        Page<CategoryWithStatsResponseDTO> result = categoryService.findAllWithStats(USER_ID, null, pageable);

        assertEquals(2, result.getContent().size());
        CategoryWithStatsResponseDTO a = result.getContent().get(0);
        CategoryWithStatsResponseDTO b = result.getContent().get(1);
        assertEquals(10L, a.id());
        assertEquals(new BigDecimal("150.00"), a.spentAmount());
        assertEquals(3L, a.expenseCount());
        assertEquals(20L, b.id());
        assertEquals(BigDecimal.ZERO, b.spentAmount());
        assertEquals(0L, b.expenseCount());
    }

    @Test
    @DisplayName("FindAllWithStats - Ok: empty page returns empty page without querying expenses")
    void findAllWithStats_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> empty = new PageImpl<>(List.of(), pageable, 0);

        when(categoryRepository.findByUserIdAndActiveTrue(USER_ID, pageable)).thenReturn(empty);

        Page<CategoryWithStatsResponseDTO> result = categoryService.findAllWithStats(USER_ID, null, pageable);

        assertTrue(result.isEmpty());
        verify(expenseRepository, never()).sumByCategoryIds(any(), any(Collection.class));
    }
}