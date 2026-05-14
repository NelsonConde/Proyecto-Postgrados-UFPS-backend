package ufps.edu.co.auth.contract;

import ufps.edu.co.auth.records.input.LoginInput;
import ufps.edu.co.auth.records.output.LoginOutput;

public interface AuthenticationUseCase {

    LoginOutput login(LoginInput input);
}