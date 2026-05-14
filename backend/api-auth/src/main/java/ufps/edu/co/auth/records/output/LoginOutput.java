package ufps.edu.co.auth.records.output;

import java.util.List;

public record LoginOutput(
        String accessToken,
        String refreshToken,
        Integer userId,
        String username,
        List<String> roles) {

    public LoginOutput {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}