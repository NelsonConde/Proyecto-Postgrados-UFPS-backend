package ufps.edu.co.auth.contract;

import ufps.edu.co.auth.model.AuthPrincipal;
import ufps.edu.co.auth.records.output.LoginOutput;

public interface TokenIssuer {

    LoginOutput issueTokens(AuthPrincipal principal);

    LoginOutput refreshTokens(String refreshToken);
}