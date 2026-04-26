package pe.com.carlosh.tallyapi.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.auth.VerificationToken;
import pe.com.carlosh.tallyapi.auth.VerificationTokenRepository;
import pe.com.carlosh.tallyapi.budget.BudgetRepository;
import pe.com.carlosh.tallyapi.budget.dto.BudgetStatsDTO;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.category.dto.CategoryStatsDTO;
import pe.com.carlosh.tallyapi.core.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.core.exception.InvalidOperationException;
import pe.com.carlosh.tallyapi.core.exception.PasswordMismatchException;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.ExpenseRepository;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseStatsDTO;
import pe.com.carlosh.tallyapi.notification.EmailService;
import pe.com.carlosh.tallyapi.security.JwtService;
import pe.com.carlosh.tallyapi.tier.Tier;
import pe.com.carlosh.tallyapi.tier.TierName;
import pe.com.carlosh.tallyapi.tier.TierRepository;
import pe.com.carlosh.tallyapi.user.dto.TierInfoDTO;
import pe.com.carlosh.tallyapi.user.dto.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final TierRepository tierRepository;

    @Transactional
    public void register(UserRequestDTO req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new AlreadyExistsException("El email ya está registrado");
        }

        if(!req.password1().equals(req.password2())){
            throw new PasswordMismatchException("Las contraseñas no coinciden");
        }

        Tier freeTier = tierRepository.findByName(TierName.FREE)
                .orElseThrow(() -> new ResourceNotFoundException("Default tier not found"));

        User user = UserMapper.toEntity(req, passwordEncoder.encode(req.password1()));
        user.setTier(freeTier);
        userRepository.save(user);

        VerificationToken verificationToken = new VerificationToken(user);
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
    }

    public UserResponseDTO findById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public LoginResponseDTO login(LoginRequestDTO req) {
        User user = userRepository.findByEmailOrUsername(req.identifier(), req.identifier())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no existe"));

        // Si no ha verificado su correo, no puede ingresar.
        if (!user.isEmailVerified()) {
            throw new InvalidOperationException("Debes verificar tu correo electrónico antes de poder iniciar sesión.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.identifier(), req.password())
        );

        String jwtToken = jwtService.generateToken(user);
        return new LoginResponseDTO(jwtToken, user.getUsername(), user.isOnboardingCompleted());
    }

    @Transactional
    public void completeOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }

    public UserStatsDTO getStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Tier tier = user.getTier();

        BigDecimal totalSpent = expenseRepository.sumTotalByUserId(userId);
        BigDecimal thisMonth = expenseRepository.sumTotalByUserIdThisMonth(userId);
        long expenseCount = expenseRepository.countByUserId(userId);


        long budgetCount = budgetRepository.countByUserIdAndActiveTrue(userId);
        long exceededCount = budgetRepository.countExceededByUserId(userId);

        long categoryCount = categoryRepository.countByUserIdAndActiveTrueAndPredefinedFalse(userId);

        String topName = null;
        BigDecimal topSpent = BigDecimal.ZERO;

        List<Category> categories = categoryRepository.findByUserIdAndActiveTrue(userId);
        for (Category cat : categories) {
            BigDecimal spent = expenseRepository.sumTotalByUserIdAndCategoryId(userId, cat.getId());
            if (spent.compareTo(topSpent) > 0) {
                topSpent = spent;
                topName = cat.getName();
            }
        }

        TierInfoDTO tierInfo = new TierInfoDTO(tier.getName().name(), tier.getMaxCategories(), tier.getMaxBudgets());

        return new UserStatsDTO(
                tierInfo,
                new BudgetStatsDTO(budgetCount, tier.getMaxBudgets(), exceededCount),
                new ExpenseStatsDTO(totalSpent, thisMonth, expenseCount),
                new CategoryStatsDTO(categoryCount, tier.getMaxCategories(), topName, topSpent)
        );
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Enlace de verificación inválido o inexistente."));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new InvalidOperationException("El enlace de verificación ha expirado. Por favor, regístrate nuevamente.");
        }

        // Activamos el correo del usuario
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        //creamos la categoria sin categoria por defecto
        Category category = new Category(Category.DEFAULT_SYSTEM_NAME, null, user);
        category.setPredefined(true);
        categoryRepository.save(category);

        // Limpiamos el token de la BD porque ya se usó
        tokenRepository.delete(verificationToken);
    }

}