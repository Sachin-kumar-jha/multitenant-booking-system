package com.ticket.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Allow these origins (configure for production)
        corsConfig.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",        // React dev
            "http://localhost:4200",        // Angular dev
            "http://localhost:8081"         // Vue dev
        ));
        
        // Allow all common headers
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Tenant-Id",
            "X-Request-Id",
            "Accept",
            "Origin"
        ));
        
        // Allow common HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Expose these headers to the client
        corsConfig.setExposedHeaders(Arrays.asList(
            "X-Request-Id",
            "X-Rate-Limit-Remaining"
        ));
        
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
