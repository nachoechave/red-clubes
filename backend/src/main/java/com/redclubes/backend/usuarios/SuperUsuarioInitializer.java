package com.redclubes.backend.usuarios;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SuperUsuarioInitializer implements CommandLineRunner {

    private static final String DNI_SUPERUSUARIO = "41131131";

    private final UsuarioRepository usuarioRepository;
    private final PasswordService passwordService;

    public SuperUsuarioInitializer(UsuarioRepository usuarioRepository, PasswordService passwordService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByDni(DNI_SUPERUSUARIO)) {
            return;
        }

        Usuario superUsuario = new Usuario(
                DNI_SUPERUSUARIO,
                "Super",
                "Usuario",
                RolUsuario.SUPERUSUARIO,
                EstadoUsuario.ACTIVO,
                passwordService.generarHash(DNI_SUPERUSUARIO),
                true
        );

        usuarioRepository.save(superUsuario);
    }
}
