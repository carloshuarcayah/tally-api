package pe.com.carlosh.tallyapi.budget;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "user_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Budget(String name, String description, BigDecimal maxAmount, User user, Category category) {
        this.name = name;
        this.description = description;
        this.maxAmount = maxAmount;
        this.user = user;
        this.category = category;
        this.active = true;
    }

    public void update(String name, String description, BigDecimal maxAmount, Category category) {
        this.name = name;
        this.description = description;
        this.maxAmount = maxAmount;
        this.category = category;
    }

    public boolean nameChanged(String name) {
        return !this.name.equalsIgnoreCase(name);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}