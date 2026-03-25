package pe.com.carlosh.tallyapi.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByUserId(Long userId, Pageable pageable);
    Page<Expense> findByUserIdAndActiveTrue(Long userId, Pageable pageable);
    Page<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);
    Page<Expense> findByUserIdAndActiveTrueAndCategoryId(Long userId, Long categoryId, Pageable pageable);
}