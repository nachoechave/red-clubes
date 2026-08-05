package com.redclubes.backend.config;

import com.redclubes.backend.usuarios.AuthService;
import com.redclubes.backend.usuarios.AutenticacionRequeridaException;
import com.redclubes.backend.usuarios.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SesionAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final SecurityErrorWriter errorWriter;

    public SesionAuthenticationFilter(AuthService authService, SecurityErrorWriter errorWriter) {
        this.authService = authService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || "/api/health".equals(path)
                || "/api/auth/login".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Usuario usuario = authService.obtenerUsuarioAutenticado(request.getHeader(HttpHeaders.AUTHORIZATION));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    usuario,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AutenticacionRequeridaException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.unauthorized(response, request.getRequestURI());
        }
    }
}
