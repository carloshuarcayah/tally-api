package pe.com.carlosh.tallyapi.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotBlank
        @Email
        String email,

        @Size(max = 20)
        String phone,

        @NotBlank
        String username,

        @NotBlank
        @Size(min = 8)
        String password1,

        @NotBlank
        @Size(min = 8)
        String password2,

        @NotBlank
        @Size(max = 50)
        String firstName,

        String lastName
) {
}