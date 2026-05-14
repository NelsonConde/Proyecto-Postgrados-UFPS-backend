package ufps.edu.co.auth.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ApiAuthModuleStartupCheck {

    @Autowired
    private AuthProperties properties;

    @Value("${app.module.name.api-auth}")
    private String moduleName;

    private static final Logger logger = LoggerFactory.getLogger(ApiAuthModuleStartupCheck.class);

    @PostConstruct
    void validateAndLogModuleLoad() {
        if (moduleName == null || moduleName.isBlank()) {
            throw new IllegalStateException("app.module.name.api-auth is required for api-auth module");
        }
        if (properties.getJwt().getSecret() == null || properties.getJwt().getSecret().isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }

        String banner = """
                ======================================================
                =                MODULE %s LOADED                    =
                ======================================================
                """.formatted(moduleName);

        logger.warn("\n" + banner);
    }
}