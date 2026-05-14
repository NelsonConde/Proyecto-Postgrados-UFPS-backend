package ufps.edu.co.auth.service;

import java.text.Normalizer;

import org.springframework.stereotype.Service;

@Service
public class RoleNameNormalizer {

    public String normalize(String roleName) {
        if (roleName == null) {
            return "";
        }

        String normalized = Normalizer.normalize(roleName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[\\s-]+", "_")
                .replaceAll("[^A-Za-z0-9_]+", "")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase();

        return normalized;
    }
}