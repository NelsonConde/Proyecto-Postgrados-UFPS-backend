package ufps.edu.co.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Auth auth = new Auth();

    public Jwt getJwt() {
        return jwt;
    }

    public Auth getAuth() {
        return auth;
    }

    public static class Jwt {
        private String secret = "dev-secret-change-me-must-be-long";
        private long accessExpirationMinutes = 120L;
        private long refreshExpirationMinutes = 10080L;
        private String issuer = "ufps-posgrados";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessExpirationMinutes() {
            return accessExpirationMinutes;
        }

        public void setAccessExpirationMinutes(long accessExpirationMinutes) {
            this.accessExpirationMinutes = accessExpirationMinutes;
        }

        public long getRefreshExpirationMinutes() {
            return refreshExpirationMinutes;
        }

        public void setRefreshExpirationMinutes(long refreshExpirationMinutes) {
            this.refreshExpirationMinutes = refreshExpirationMinutes;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Auth {
        private final Password password = new Password();

        public Password getPassword() {
            return password;
        }
    }

    public static class Password {
        private boolean allowPlainTextLegacy = true;

        public boolean isAllowPlainTextLegacy() {
            return allowPlainTextLegacy;
        }

        public void setAllowPlainTextLegacy(boolean allowPlainTextLegacy) {
            this.allowPlainTextLegacy = allowPlainTextLegacy;
        }
    }
}