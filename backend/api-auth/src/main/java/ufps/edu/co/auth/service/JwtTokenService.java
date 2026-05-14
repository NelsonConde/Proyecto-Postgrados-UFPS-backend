package ufps.edu.co.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ufps.edu.co.auth.config.AuthProperties;
import ufps.edu.co.auth.contract.AccessTokenValidator;
import ufps.edu.co.auth.contract.TokenIssuer;
import ufps.edu.co.auth.exception.InvalidTokenException;
import ufps.edu.co.auth.model.AuthPrincipal;
import ufps.edu.co.auth.model.TokenType;
import ufps.edu.co.auth.records.output.LoginOutput;

@Service
public class JwtTokenService implements TokenIssuer, AccessTokenValidator {

    private final AuthProperties properties;
    private final SecretKey key;

    public JwtTokenService(AuthProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public LoginOutput issueTokens(AuthPrincipal principal) {
        String accessToken = createToken(principal, TokenType.ACCESS, properties.getJwt().getAccessExpirationMinutes());
        String refreshToken = createToken(principal, TokenType.REFRESH,
                properties.getJwt().getRefreshExpirationMinutes());
        return new LoginOutput(accessToken, refreshToken, principal.userId(), principal.username(), principal.roles());
    }

    @Override
    public LoginOutput refreshTokens(String refreshToken) {
        AuthPrincipal principal = parseToken(refreshToken, TokenType.REFRESH);
        return issueTokens(principal);
    }

    @Override
    public boolean isValid(String accessToken) {
        try {
            parseToken(accessToken, TokenType.ACCESS);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public AuthPrincipal parse(String accessToken) {
        return parseToken(accessToken, TokenType.ACCESS);
    }

    private String createToken(AuthPrincipal principal, TokenType type, long expirationMinutes) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (expirationMinutes * 60_000L));

        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(principal.username())
                .issuedAt(now)
                .expiration(expiration)
                .claim("uid", principal.userId())
                .claim("roles", principal.roles())
                .claim("type", type.name())
                .signWith(key)
                .compact();
    }

    private AuthPrincipal parseToken(String token, TokenType expectedType) {
        try {
            Jws<Claims> jws = parser().parseSignedClaims(token);
            Claims claims = jws.getPayload();

            String type = claims.get("type", String.class);
            if (type == null || !expectedType.name().equals(type)) {
                throw new InvalidTokenException("Invalid token type");
            }

            Integer userId = claims.get("uid", Integer.class);
            String username = claims.getSubject();
            Object rolesObj = claims.get("roles");
            List<String> roles = rolesObj == null ? List.of()
                    : ((List<?>) rolesObj).stream().map(Object::toString).collect(Collectors.toList());
            return new AuthPrincipal(userId, username, List.copyOf(roles));
        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("Token expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    private io.jsonwebtoken.JwtParser parser() {
        io.jsonwebtoken.JwtParserBuilder builder = Jwts.parser().verifyWith(key);
        String issuer = properties.getJwt().getIssuer();
        if (issuer != null && !issuer.isBlank()) {
            builder.requireIssuer(issuer);
        }
        return builder.build();
    }
}