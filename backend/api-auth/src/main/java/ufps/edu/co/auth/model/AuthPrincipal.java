package ufps.edu.co.auth.model;

import java.util.List;

public record AuthPrincipal(
        Integer userId,
        String username,
        List<String> roles) {

    public AuthPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}