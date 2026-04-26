package pe.com.carlosh.tallyapi.tier;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tiers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Tier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private TierName name;

    @Column(name = "max_categories", nullable = false)
    private int maxCategories;

    @Column(name = "max_budgets", nullable = false)
    private int maxBudgets;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Tier(TierName name, int maxCategories, int maxBudgets) {
        this.name = name;
        this.maxCategories = maxCategories;
        this.maxBudgets = maxBudgets;
    }
}
