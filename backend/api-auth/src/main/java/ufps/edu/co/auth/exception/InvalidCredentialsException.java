package ufps.edu.co.auth.exception;

public class InvalidCredentialsException extends AuthDomainException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}