package com.redclubes.backend.portal;

import com.redclubes.backend.config.SecurityErrorWriter;
import com.redclubes.backend.usuarios.AutenticacionRequeridaException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Configuration
public class PortalSecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain portalChain(HttpSecurity http, PortalService service, SecurityErrorWriter errors) throws Exception {
        return http.securityMatcher("/api/portal-bomberos/**")
                .csrf(c -> c.disable()).cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/portal-bomberos/auth/solicitar-codigo",
                                "/api/portal-bomberos/auth/verificar-codigo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/portal-bomberos/me", "/api/portal-bomberos/cuotas").hasRole("PORTAL_SOCIO")
                        .requestMatchers(HttpMethod.POST, "/api/portal-bomberos/auth/cerrar-sesion").hasRole("PORTAL_SOCIO")
                        .requestMatchers(HttpMethod.POST, "/api/portal-bomberos/pagos", "/api/portal-bomberos/pagos/*/actualizar").hasRole("PORTAL_SOCIO")
                        .requestMatchers(HttpMethod.GET, "/api/portal-bomberos/pagos/*").hasRole("PORTAL_SOCIO")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> errors.unauthorized(res, req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) -> errors.forbidden(res, req.getRequestURI())))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                            throws ServletException, IOException {
                        res.setHeader("Cache-Control", "no-store");
                        String path = req.getServletPath().isEmpty() ? req.getRequestURI() : req.getServletPath();
                        boolean publicRoute = "OPTIONS".equals(req.getMethod()) || ("POST".equals(req.getMethod())
                                && (path.equals("/api/portal-bomberos/auth/solicitar-codigo")
                                || path.equals("/api/portal-bomberos/auth/verificar-codigo")));
                        if (!publicRoute) {
                            try {
                                var principal = service.authenticate(req.getHeader("Authorization"));
                                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_PORTAL_SOCIO"))));
                            } catch (AutenticacionRequeridaException e) {
                                errors.unauthorized(res, req.getRequestURI()); return;
                            }
                        }
                        chain.doFilter(req, res);
                    }
                }, UsernamePasswordAuthenticationFilter.class).build();
    }
}
