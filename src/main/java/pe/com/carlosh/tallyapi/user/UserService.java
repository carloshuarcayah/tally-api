package pe.com.carlosh.tallyapi.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.carlosh.tallyapi.exception.AlreadyExistsException;
import pe.com.carlosh.tallyapi.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.security.JwtService;
import pe.com.carlosh.tallyapi.user.dto.LoginRequestDTO;
import pe.com.carlosh.tallyapi.user.dto.LoginResponseDTO;
import pe.com.carlosh.tallyapi.user.dto.UserRequestDTO;
import pe.com.carlosh.tallyapi.user.dto.UserResponseDTO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponseDTO register(UserRequestDTO req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new AlreadyExistsException("Email already register");
        }

        User user = UserMapper.toEntity(req,passwordEncoder.encode(req.password()));

        String jwtToken = jwtService.generateToken(userRepository.save(user));
        return new LoginResponseDTO(jwtToken);
    }

    public UserResponseDTO findById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public LoginResponseDTO login(LoginRequestDTO req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User does not exists"));

        String jwtToken = jwtService.generateToken(user);
        return new LoginResponseDTO(jwtToken);
    }

}