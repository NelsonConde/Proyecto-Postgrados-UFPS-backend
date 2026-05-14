package ufps.edu.co.auth.records.input;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenInput(
        @NotBlank String refreshToken) {
}