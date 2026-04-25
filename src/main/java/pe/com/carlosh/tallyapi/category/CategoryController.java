package pe.com.carlosh.tallyapi.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.com.carlosh.tallyapi.category.dto.CategoryRequestDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryResponseDTO;
import pe.com.carlosh.tallyapi.category.dto.CategoryWithStatsResponseDTO;
import pe.com.carlosh.tallyapi.core.dto.SetActiveRequestDTO;
import pe.com.carlosh.tallyapi.user.User;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<CategoryResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        return ResponseEntity.ok(categoryService.findAll(user.getId(), name, pageable));
    }

    @GetMapping("/with-stats")
    public ResponseEntity<Page<CategoryWithStatsResponseDTO>> findAllWithStats(
            @RequestParam(required = false) String name,
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        return ResponseEntity.ok(categoryService.findAllWithStats(user.getId(), name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> findById(@PathVariable Long id,@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.findById(id,user.getId()));
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO req, @AuthenticationPrincipal User user) {
        CategoryResponseDTO response = categoryService.create(req, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable Long id,@AuthenticationPrincipal User user,
            @Valid @RequestBody CategoryRequestDTO req) {
        return ResponseEntity.ok(categoryService.update(id,user.getId(),  req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        categoryService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> setActive(
            @PathVariable Long id,
            @Valid @RequestBody SetActiveRequestDTO req,
            @AuthenticationPrincipal User user) {
        categoryService.setActive(id, user.getId(), req.active());
        return ResponseEntity.noContent().build();
    }
}