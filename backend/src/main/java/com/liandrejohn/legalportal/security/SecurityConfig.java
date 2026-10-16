package com.liandrejohn.legalportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailsAccessDeniedHandler accessDeniedHandler,
            ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint) {
        // Tokens live in the server-side session; the SPA reads one from GET /api/v1/auth/csrf
        HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
        csrfTokenRepository.setHeaderName(CSRF_HEADER_NAME);

        http.csrf((csrf) -> csrf.csrfTokenRepository(csrfTokenRepository))
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                // Public, but still CSRF-protected like every other state-changing request
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling((exceptions) -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            // Logout gets its own API endpoint later; the default POST /logout filter isn't wanted
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
