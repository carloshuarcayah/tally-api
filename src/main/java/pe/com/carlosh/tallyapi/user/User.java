package pe.com.carlosh.tallyapi.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false, unique = true)
    @Getter
    private String email;

    @Column(length = 20)
    @Getter
    private String phone;

    @Column(nullable = false,unique = false)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(nullable = false,length = 50)
    @Getter
    private String firstName;

    @Getter
    private String lastName;

    @CreationTimestamp
    @Column(updatable = false,nullable = false)
    @Getter
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    @Setter @Getter
    private boolean onboardingCompleted;

    @Column(nullable = false)
    @Setter @Getter
    private boolean emailVerified;


    public User(String email, String phone, String username, String password, String firstName, String lastName) {
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = Role.USER;
        this.active = true;
        this.onboardingCompleted=false;
        this.emailVerified = false;
    }

    //usamos el email como identificador
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+this.role.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active;
    }

    public String getNickname(){
        return this.username;
    }
}
