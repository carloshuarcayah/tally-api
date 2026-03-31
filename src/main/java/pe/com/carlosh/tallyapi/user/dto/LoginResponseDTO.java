package pe.com.carlosh.tallyapi.user.dto;

public record LoginResponseDTO (String token,
                                String username,
                                boolean onboardingCompleted){
}
