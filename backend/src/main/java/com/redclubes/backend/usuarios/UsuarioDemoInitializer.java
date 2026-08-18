package com.redclubes.backend.usuarios;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "redclubes.demo.enabled", havingValue = "true")
@Order(3)
public class UsuarioDemoInitializer implements CommandLineRunner {

    static final String DNI = "41131131";
    static final String PASSWORD = "12345678";

    private final UsuarioRepository usuarioRepository;
    private final PasswordService passwordService;

    public UsuarioDemoInitializer(UsuarioRepository usuarioRepository, PasswordService passwordService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByDni(DNI)) {
            return;
        }

        Usuario superusuario = new Usuario(
                DNI,
                "Superusuario",
                "Demo",
                RolUsuario.SUPERUSUARIO,
                EstadoUsuario.ACTIVO,
                passwordService.generarHash(PASSWORD),
                true
        );

        usuarioRepository.save(superusuario);
    }
}
