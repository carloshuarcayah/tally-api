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
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.user.User;
import pe.com.carlosh.tallyapi.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository repository;

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

        category1 = new Category("Categoria 1", "Categoria de prueba", user1);
        ReflectionTestUtils.setField(category1, "id", CATEGORY_ID);
    }

    @Test
    @DisplayName("Should return a page with just one category with id: 99")
    void findByActiveTrue() {

        Page<Category> page = new PageImpl<>(List.of(category1));
        Pageable pageable = PageRequest.of(0,10);
        when(categoryRepository.findByUserIdAndActiveTrue(USER_ID,pageable)).thenReturn(page);

        Page<CategoryResponseDTO> result = categoryService.findByActiveTrue(USER_ID,pageable);

        assertNotNull(result);
        assertEquals(1,result.getTotalElements());
        assertEquals(USER_ID,result.getContent().getFirst().userId());
        assertEquals(CATEGORY_ID,result.getContent().getFirst().id());
        verify(categoryRepository,times(1)).findByUserIdAndActiveTrue(USER_ID,pageable);
        assertEquals("Categoria 1", result.getContent().getFirst().name());
    }

    @Test
    @DisplayName("Should return an CategoryResponse with id: 99")
    void findById() {
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(CATEGORY_ID,USER_ID)).thenReturn(Optional.of(category1));

        CategoryResponseDTO result = categoryService.findById(CATEGORY_ID,USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID,result.userId());
        assertEquals(CATEGORY_ID,result.id());
        verify(categoryRepository,times(1)).findByIdAndUserIdAndActiveTrue(CATEGORY_ID,USER_ID);
        assertEquals("Categoria 1", result.name());

    }

    @Test
    void findByName() {
    }

    @Test
    void create() {
    }

    @Test
    void update() {
    }

    @Test
    void delete() {
    }

    @Test
    void enable() {
    }
}