package com.redclubes.backend.usuarios;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "redclubes.bootstrap.superuser.enabled", havingValue = "true")
public class SuperUsuarioInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordService passwordService;
    private final String dni;
    private final String password;
    private final String nombre;
    private final String apellido;

    public SuperUsuarioInitializer(
            UsuarioRepository usuarioRepository,
            PasswordService passwordService,
            @Value("${BOOTSTRAP_SUPERUSER_DNI}") String dni,
            @Value("${BOOTSTRAP_SUPERUSER_PASSWORD}") String password,
            @Value("${BOOTSTRAP_SUPERUSER_NAME:Administrador}") String nombre,
            @Value("${BOOTSTRAP_SUPERUSER_LASTNAME:Inicial}") String apellido
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordService = passwordService;
        this.dni = dni;
        this.password = password;
        this.nombre = nombre;
        this.apellido = apellido;
    }

    @Override
    public void run(String... args) {
        if (dni.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Las credenciales de bootstrap del superusuario son obligatorias");
        }
        if (usuarioRepository.existsByDni(dni)) {
            return;
        }

        Usuario superUsuario = new Usuario(
                dni,
                nombre,
                apellido,
                RolUsuario.SUPERUSUARIO,
                EstadoUsuario.ACTIVO,
                passwordService.generarHash(password),
                true
        );

        usuarioRepository.save(superUsuario);
    }
}
