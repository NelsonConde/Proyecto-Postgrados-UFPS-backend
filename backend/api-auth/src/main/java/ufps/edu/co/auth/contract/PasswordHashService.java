package ufps.edu.co.auth.contract;

public interface PasswordHashService {

    boolean matches(String rawPassword, String storedPassword);

    String encode(String rawPassword);

    boolean needsRehash(String storedPassword);
}