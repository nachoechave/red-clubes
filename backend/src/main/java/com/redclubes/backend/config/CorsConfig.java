package com.redclubes.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private final String[] allowedOrigins;
    private final String[] portalOrigins;

    public CorsConfig(@Value("${redclubes.cors.allowed-origins}") String allowedOrigins,
                      @Value("${portal.allowed-origins:http://localhost:4300}") String portalOrigins) {
        this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
        this.portalOrigins = portalOrigins.split("\\s*,\\s*");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/portal-bomberos/**")
                        .allowedOrigins(portalOrigins)
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type")
                        .allowCredentials(false);
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
