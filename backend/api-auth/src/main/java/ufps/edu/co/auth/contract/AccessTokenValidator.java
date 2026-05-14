package ufps.edu.co.auth.contract;

import ufps.edu.co.auth.model.AuthPrincipal;

public interface AccessTokenValidator {

    boolean isValid(String accessToken);

    AuthPrincipal parse(String accessToken);
}