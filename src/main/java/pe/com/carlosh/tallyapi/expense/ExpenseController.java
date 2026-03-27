package pe.com.carlosh.tallyapi.expense;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseRequestDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseResponseDTO;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseListResponseDTO;
import pe.com.carlosh.tallyapi.user.User;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;


    //FIND ALL AND FIND ALL BY CATEGORY
    @GetMapping
    public ResponseEntity<ExpenseListResponseDTO> findAll(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal User user,
            Pageable pageable) {

        return ResponseEntity.ok(expenseService.findByUserIdAndCategoryId(user.getId(),categoryId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.findById(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponseDTO> create(
            @Valid @RequestBody ExpenseRequestDTO req,
            @AuthenticationPrincipal User user) {

        ExpenseResponseDTO response = expenseService.create(req, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequestDTO req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.update(id, req, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(expenseService.delete(id, user.getId()));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<ExpenseResponseDTO> enable(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(expenseService.enable(id, user.getId()));
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotal(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expenseService.getTotalByUser(user.getId()));
    }

    @GetMapping("/total/{categoryId}")
    public ResponseEntity<BigDecimal> getTotalByCategory(@AuthenticationPrincipal User user, @PathVariable Long categoryId) {
        return ResponseEntity.ok(expenseService.getTotalByCategory(user.getId(),categoryId));
    }
}