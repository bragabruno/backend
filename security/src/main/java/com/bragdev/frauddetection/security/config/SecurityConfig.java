package com.bragdev.frauddetection.security.config;

import com.bragdev.frauddetection.security.filter.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator").permitAll()

                // Transaction ingest: ADMIN and SYSTEM_ACCOUNT only
                .requestMatchers(HttpMethod.POST, "/api/v1/transactions").hasAnyRole("ADMIN", "SYSTEM_ACCOUNT")
                // Transaction reads: all analyst roles
                .requestMatchers(HttpMethod.GET, "/api/v1/transactions", "/api/v1/transactions/**")
                    .hasAnyRole("ADMIN", "FRAUD_ANALYST", "INVESTIGATOR", "AUDITOR")

                // Case management reads: all analyst roles
                .requestMatchers(HttpMethod.GET, "/api/v1/fraud-cases/**")
                    .hasAnyRole("ADMIN", "FRAUD_ANALYST", "INVESTIGATOR", "AUDITOR")
                // Case management writes: ADMIN, FRAUD_ANALYST, INVESTIGATOR
                .requestMatchers(HttpMethod.PUT, "/api/v1/fraud-cases/**")
                    .hasAnyRole("ADMIN", "FRAUD_ANALYST", "INVESTIGATOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/fraud-cases/**")
                    .hasAnyRole("ADMIN", "FRAUD_ANALYST", "INVESTIGATOR")

                // Risk scores: all analyst roles
                .requestMatchers(HttpMethod.GET, "/api/v1/risk-scores/**")
                    .hasAnyRole("ADMIN", "FRAUD_ANALYST", "INVESTIGATOR", "AUDITOR")

                // Admin: ADMIN only
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
