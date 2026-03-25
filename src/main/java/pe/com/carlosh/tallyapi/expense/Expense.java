package pe.com.carlosh.tallyapi.expense;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean active;

    public Expense(BigDecimal amount, String description, LocalDate expenseDate, User user, Category category) {
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.user = user;
        this.category = category;
        this.active=true;
    }

    public void update(BigDecimal amount, String description, LocalDate expenseDate, Category category) {
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.category = category;
    }

    public void activate(){
        this.active=true;
    }

    public void deactivate(){
        this.active=false;
    }
}
