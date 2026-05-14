package ufps.edu.co.auth.model;

import java.util.List;

public record AuthenticatedUser(
        Integer userId,
        String username,
        String passwordHash,
        List<String> roles) {

    public AuthenticatedUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}