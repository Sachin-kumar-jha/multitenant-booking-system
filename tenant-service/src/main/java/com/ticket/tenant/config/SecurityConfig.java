package com.ticket.tenant.config;

import com.ticket.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Tenant Service.
 * 
 * Public endpoints:
 * - POST /api/tenants (create new tenant)
 * - GET /api/tenants (list all tenants)
 * - GET /api/tenants/public/** (public tenant info by subdomain)
 * 
 * Admin protected endpoints:
 * - GET /api/tenants/{id}
 * - PUT /api/tenants/{id}
 * - POST /api/tenants/{id}/suspend
 * - POST /api/tenants/{id}/activate
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - tenant onboarding
                .requestMatchers(HttpMethod.POST, "/api/tenants").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tenants").permitAll()
                .requestMatchers("/api/tenants/public/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Admin protected endpoints
                .requestMatchers(HttpMethod.GET, "/api/tenants/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/tenants/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tenants/*/suspend").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tenants/*/activate").hasRole("ADMIN")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
