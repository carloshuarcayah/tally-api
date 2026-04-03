package pe.com.carlosh.tallyapi.budget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Page<Budget> findByUserIdAndActiveTrue(Long userId, Pageable pageable);
    Optional<Budget> findByIdAndUserIdAndActiveTrue(Long id, Long userId);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(Long userId, String name);
    long countByUserIdAndActiveTrue(Long userId);

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.user.id = :userId AND b.active = true AND (SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.budget = b AND e.active = true) > b.maxAmount")
    long countExceededByUserId(@Param("userId") Long userId);
}