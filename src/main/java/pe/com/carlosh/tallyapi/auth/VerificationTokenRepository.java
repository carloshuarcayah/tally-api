package pe.com.carlosh.tallyapi.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.carlosh.tallyapi.user.User;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser(User user);
}