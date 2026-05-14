package ufps.edu.co.auth.exception;

public class MissingCredentialsException extends AuthDomainException {

    public MissingCredentialsException() {
        super("Missing credentials");
    }

    public MissingCredentialsException(String message) {
        super(message);
    }
}