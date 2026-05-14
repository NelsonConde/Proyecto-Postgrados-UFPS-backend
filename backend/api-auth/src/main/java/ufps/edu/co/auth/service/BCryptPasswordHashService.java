package ufps.edu.co.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import ufps.edu.co.auth.config.AuthProperties;
import ufps.edu.co.auth.contract.PasswordHashService;

@Service
public class BCryptPasswordHashService implements PasswordHashService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthProperties properties;

    public BCryptPasswordHashService(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (isBcrypt(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }

        return properties.getAuth().getPassword().isAllowPlainTextLegacy() && rawPassword.equals(storedPassword);
    }

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean needsRehash(String storedPassword) {
        return !isBcrypt(storedPassword);
    }

    private boolean isBcrypt(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}