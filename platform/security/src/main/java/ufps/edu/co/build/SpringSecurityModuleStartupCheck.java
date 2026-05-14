package ufps.edu.co.build;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityModuleStartupCheck {

    @Value("${app.module.name.security}")
    private String moduleName;

    @PostConstruct
    void validate() {
        if (moduleName == null || moduleName.isBlank()) {
            throw new IllegalStateException("app.module.name.security is required for security module");
        }
    }
}