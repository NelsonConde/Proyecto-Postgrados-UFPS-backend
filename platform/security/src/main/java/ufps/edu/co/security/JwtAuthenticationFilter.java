package ufps.edu.co.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ufps.edu.co.auth.contract.AccessTokenValidator;
import ufps.edu.co.auth.model.AuthPrincipal;
import ufps.edu.co.auth.util.BearerTokenConstants;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenValidator accessTokenValidator;
    private final SpringSecurityPrincipalAdapter principalAdapter;

    public JwtAuthenticationFilter(AccessTokenValidator accessTokenValidator,
            SpringSecurityPrincipalAdapter principalAdapter) {
        this.accessTokenValidator = accessTokenValidator;
        this.principalAdapter = principalAdapter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs")
                || "/swagger-ui.html".equals(path)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = BearerTokenConstants.extractToken(authorization);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!accessTokenValidator.isValid(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        AuthPrincipal principal = accessTokenValidator.parse(token);
        UsernamePasswordAuthenticationToken authentication = principalAdapter.toAuthentication(principal, request);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}