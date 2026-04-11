package pe.com.carlosh.tallyapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import pe.com.carlosh.tallyapi.core.exception.ResourceNotFoundException;
import pe.com.carlosh.tallyapi.user.UserRepository;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {
    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        // 👇 Ahora busca por ambos campos usando el mismo input
        return input -> userRepository.findByEmailOrUsername(input, input)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}