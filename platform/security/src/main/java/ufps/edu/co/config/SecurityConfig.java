package ufps.edu.co.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import ufps.edu.co.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

        private static final String[] SUPER_ADMIN_PATHS = {
                        "/**"
        };

        private static final String[] PUBLIC_PATHS = {
                        "/auth/login",
                        "/auth/refresh",
                        "/api/application/case/login/recoveryPassword",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/error"
        };

        private static final String[] DIRECTOR_FACULTAD_PATHS = {
                        "/facultades/**",
                        "/programas/**",
                        "/profesores/**",
                        "/estudiantes/**",
                        "/proyectos/**"
        };

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
                        JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth

                                                // Permite todas las solicitudes OPTIONS para CORS preflight
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // Rutas públicas sin autenticación
                                                .requestMatchers(PUBLIC_PATHS)
                                                .permitAll()

                                                // Rutas protegidas para el rol SUPER_ADMINISTRADOR
                                                .requestMatchers(SUPER_ADMIN_PATHS)
                                                .hasRole("SUPER_ADMINISTRADOR")

                                                .requestMatchers(DIRECTOR_FACULTAD_PATHS)
                                                .hasRole("DIRECTOR_FACULTAD")

                                                // Denegar cualquier otra solicitud no especificada
                                                .anyRequest().denyAll())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
