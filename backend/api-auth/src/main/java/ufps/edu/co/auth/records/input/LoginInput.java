package ufps.edu.co.auth.records.input;

import jakarta.validation.constraints.NotBlank;

public record LoginInput(
        @NotBlank String username,
        @NotBlank String password) {
}