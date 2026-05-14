package ufps.edu.co.auth.contract;

import ufps.edu.co.auth.records.output.LoginOutput;

public interface RefreshTokenUseCase {

    LoginOutput refresh(String refreshToken);
}