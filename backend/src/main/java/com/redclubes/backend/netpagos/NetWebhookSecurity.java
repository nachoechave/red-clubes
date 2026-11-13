package com.redclubes.backend.netpagos;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class NetWebhookSecurity {
    @Bean @Order(0)
    SecurityFilterChain netWebhookChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/integraciones/netpagos/webhook")
                .csrf(c -> c.disable()).cors(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.POST, "/api/integraciones/netpagos/webhook").permitAll()
                        .anyRequest().denyAll()).build();
    }
}
