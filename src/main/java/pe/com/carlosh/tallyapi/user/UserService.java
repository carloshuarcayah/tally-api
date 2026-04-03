package pe.com.carlosh.tallyapi.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.budget.BudgetRepository;
import pe.com.carlosh.tallyapi.budget.dto.BudgetStatsDTO;
import pe.com.carlosh.tallyapi.category.Category;
import pe.com.carlosh.tallyapi.category.CategoryRepository;
import pe.com.carlosh.tallyapi.category.dto.CategoryStatsDTO;
import pe.com.carlosh.tallyapi.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.expense.ExpenseRepository;
import pe.com.carlosh.tallyapi.expense.dto.ExpenseStatsDTO;
import pe.com.carlosh.tallyapi.security.JwtService;
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

    @Transactional
    public LoginResponseDTO register(UserRequestDTO req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new AlreadyExistsException("Email already register");
        }

        User user = UserMapper.toEntity(req,passwordEncoder.encode(req.password()));

        String jwtToken = jwtService.generateToken(userRepository.save(user));
        return new LoginResponseDTO(jwtToken,user.getUsername(),user.isOnboardingCompleted());
    }

    public UserResponseDTO findById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public LoginResponseDTO login(LoginRequestDTO req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.identifier(), req.password())
        );

        User user = userRepository.findByEmailOrUsername(req.identifier(), req.identifier())
                .orElseThrow(() -> new ResourceNotFoundException("User does not exists"));

        String jwtToken = jwtService.generateToken(user);
        return new LoginResponseDTO(jwtToken,user.getUsername(),user.isOnboardingCompleted());
    }

    @Transactional
    public void completeOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }

    public UserStatsDTO getStats(Long userId) {
        BigDecimal totalSpent = expenseRepository.sumTotalByUserId(userId);
        BigDecimal thisMonth = expenseRepository.sumTotalByUserIdThisMonth(userId);
        long expenseCount = expenseRepository.countByUserId(userId);


        long budgetCount = budgetRepository.countByUserIdAndActiveTrue(userId);
        long exceededCount = budgetRepository.countExceededByUserId(userId);

        long categoryCount = categoryRepository.countByUserIdAndActiveTrue(userId);

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

        return new UserStatsDTO(
                new BudgetStatsDTO(budgetCount, exceededCount),
                new ExpenseStatsDTO(totalSpent, thisMonth, expenseCount),
                new CategoryStatsDTO(categoryCount, topName, topSpent)
        );
    }

}