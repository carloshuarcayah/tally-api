package pe.com.carlosh.tallyapi.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.com.carlosh.tallyapi.category.Category;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByUserId(Long userId, Pageable pageable);
    Page<Expense> findByUserIdAndActiveTrue(Long userId, Pageable pageable);
    Page<Expense> findByUserIdAndActiveTrueAndCategoryId(Long userId, Long categoryId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.active = true")
    BigDecimal sumTotalByUserId(@Param("userId")Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.active = true AND e.category.id=:categoryId")
    BigDecimal sumTotalByUserIdAndCategoryId(@Param("userId")Long userId, @Param("categoryId") Long categoryId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.budget.id = :budgetId AND e.active = true")
    BigDecimal sumTotalByBudgetId(@Param("budgetId") Long budgetId);

    Page<Expense> findByUserIdAndActiveTrueAndBudgetId(Long userId, Long budgetId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.user.id = :userId AND e.active = true")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.active = true AND MONTH(e.expenseDate) = MONTH(CURRENT_DATE) AND YEAR(e.expenseDate) = YEAR(CURRENT_DATE)")
    BigDecimal sumTotalByUserIdThisMonth(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Expense e SET e.category = :newCategory WHERE e.category = :oldCategory")
    int reassignCategory(@Param("oldCategory") Category oldCategory, @Param("newCategory") Category newCategory);

    @Query("""
            SELECT e.category.id, COALESCE(SUM(e.amount), 0), COUNT(e.id)
            FROM Expense e
            WHERE e.user.id = :userId AND e.active = true AND e.category.id IN :categoryIds
            GROUP BY e.category.id
            """)
    List<Object[]> sumByCategoryIds(@Param("userId") Long userId, @Param("categoryIds") Collection<Long> categoryIds);
}