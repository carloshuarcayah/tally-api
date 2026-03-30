package pe.com.carlosh.tallyapi.budget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Page<Budget> findByUserIdAndActiveTrue(Long userId, Pageable pageable);
    Optional<Budget> findByIdAndUserIdAndActiveTrue(Long id, Long userId);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(Long userId, String name);
}