package pe.com.carlosh.tallyapi.category;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pe.com.carlosh.tallyapi.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false,length = 100)
    @Getter
    private String name;

    @Getter
    private String description;

    @Column(nullable = false,updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    @Getter
    private User user;

    @Column(nullable = false)
    @Getter
    private boolean active;

    public Category(String name, String description,User user) {
        this.name = name;
        this.description = description;
        this.user= user;
        this.active = true;
    }

    public void update(String name, String description){
        this.name=name;
        this.description=description;
    }

    public boolean nameChanged(String name){
        return !this.name.equalsIgnoreCase(name);
    }

    public void activate(){
        this.active=true;
    }

    public void deactivate(){
        this.active=false;
    }
}