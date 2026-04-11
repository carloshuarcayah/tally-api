package pe.com.carlosh.tallyapi.category;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.core.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

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
}