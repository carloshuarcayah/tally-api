package pe.com.carlosh.tallyapi.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pe.com.carlosh.tallyapi.auth.VerificationToken;
import pe.com.carlosh.tallyapi.auth.VerificationTokenRepository;
import pe.com.carlosh.tallyapi.budget.BudgetRepository;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.core.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.core.exception.InvalidOperationException;
import pe.com.carlosh.tallyapi.core.exception.PasswordMismatchException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.ExpenseRepository;
import pe.com.carlosh.tallyapi.notification.EmailService;
import pe.com.carlosh.tallyapi.security.JwtService;
import pe.com.carlosh.tallyapi.user.dto.LoginRequestDTO;
import pe.com.carlosh.tallyapi.user.dto.LoginResponseDTO;
import pe.com.carlosh.tallyapi.user.dto.UserRequestDTO;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private pe.com.carlosh.tallyapi.tier.TierRepository tierRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        user = new User("test@mail.com", "123456789", "tester", "encodedPass", "Carlos", "Test");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Register - Ok: creates user, token and sends email")
    void register_Success() {
        UserRequestDTO req = new UserRequestDTO("new@mail.com", "999", "newuser", "password1", "password1", "John", "Doe");

        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        pe.com.carlosh.tallyapi.tier.Tier freeTier = new pe.com.carlosh.tallyapi.tier.Tier(pe.com.carlosh.tallyapi.tier.TierName.FREE, 5, 4);
        when(tierRepository.findByName(pe.com.carlosh.tallyapi.tier.TierName.FREE)).thenReturn(java.util.Optional.of(freeTier));

        userService.register(req);

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendVerificationEmail(eq("new@mail.com"), anyString());
    }

    @Test
    @DisplayName("Register - Error: throws AlreadyExistsException when email exists")
    void register_ThrowsAlreadyExistsException() {
        UserRequestDTO req = new UserRequestDTO("test@mail.com", "999", "user", "pass1234", "pass1234", "John", "Doe");

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> userService.register(req));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Register - Error: throws PasswordMismatchException when passwords differ")
    void register_ThrowsPasswordMismatchException() {
        UserRequestDTO req = new UserRequestDTO("new@mail.com", "999", "user", "pass1234", "otherpass", "John", "Doe");

        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);

        assertThrows(PasswordMismatchException.class, () -> userService.register(req));

        verify(userRepository, never()).save(any(User.class));
    }

    // ── login ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login - Ok: returns token when user is verified")
    void login_Success() {
        LoginRequestDTO req = new LoginRequestDTO("test@mail.com", "password");
        user.setEmailVerified(true);

        when(userRepository.findByEmailOrUsername("test@mail.com", "test@mail.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");

        LoginResponseDTO response = userService.login(req);

        assertEquals("fake-jwt-token", response.token());
        assertEquals("test@mail.com", response.username());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    @DisplayName("Login - Error: throws ResourceNotFoundException when user not found")
    void login_ThrowsResourceNotFoundException() {
        LoginRequestDTO req = new LoginRequestDTO("ghost@mail.com", "password");

        when(userRepository.findByEmailOrUsername("ghost@mail.com", "ghost@mail.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.login(req));
    }

    @Test
    @DisplayName("Login - Error: throws InvalidOperationException when email not verified")
    void login_ThrowsInvalidOperationException_WhenNotVerified() {
        LoginRequestDTO req = new LoginRequestDTO("test@mail.com", "password");
        // user.emailVerified is false by default

        when(userRepository.findByEmailOrUsername("test@mail.com", "test@mail.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidOperationException.class, () -> userService.login(req));

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    // ── completeOnboarding ───────────────────────────────────────────────

    @Test
    @DisplayName("CompleteOnboarding - Ok: marks onboarding as completed")
    void completeOnboarding_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.completeOnboarding(USER_ID);

        assertTrue(user.isOnboardingCompleted());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("CompleteOnboarding - Error: throws ResourceNotFoundException when user not found")
    void completeOnboarding_ThrowsResourceNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.completeOnboarding(USER_ID));
    }

    // ── verifyEmail ──────────────────────────────────────────────────────

    @Test
    @DisplayName("VerifyEmail - Ok: marks email verified and deletes token")
    void verifyEmail_Success() {
        VerificationToken token = new VerificationToken(user);

        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        userService.verifyEmail(token.getToken());

        assertTrue(user.isEmailVerified());
        verify(userRepository, times(1)).save(user);
        verify(tokenRepository, times(1)).delete(token);
    }

    @Test
    @DisplayName("VerifyEmail - Error: throws ResourceNotFoundException when token not found")
    void verifyEmail_ThrowsResourceNotFoundException() {
        when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.verifyEmail("invalid"));
    }

    @Test
    @DisplayName("VerifyEmail - Error: throws InvalidOperationException when token expired")
    void verifyEmail_ThrowsInvalidOperationException_WhenExpired() {
        VerificationToken token = new VerificationToken(user);
        ReflectionTestUtils.setField(token, "expiryDate", LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThrows(InvalidOperationException.class, () -> userService.verifyEmail(token.getToken()));

        assertFalse(user.isEmailVerified());
        verify(tokenRepository, times(1)).delete(token);
        verify(userRepository, never()).save(any(User.class));
    }

    // ── findById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("FindById - Ok: returns user")
    void findById_Success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertNotNull(userService.findById(USER_ID));
    }

    @Test
    @DisplayName("FindById - Error: throws ResourceNotFoundException when not found")
    void findById_ThrowsResourceNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(USER_ID));
    }
}
