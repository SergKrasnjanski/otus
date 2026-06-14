package otus.krasnyanskysa.jwtapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank @Email(message = "Invalid email") String email,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password
) {}
