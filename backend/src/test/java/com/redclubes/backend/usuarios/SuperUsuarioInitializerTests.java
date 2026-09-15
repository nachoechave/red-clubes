package com.redclubes.backend.usuarios;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperUsuarioInitializerTests {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordService passwordService;

    @Test
    void creaSuperusuarioSoloConCredencialesConfiguradas() throws Exception {
        when(usuarioRepository.existsByDni("41131131")).thenReturn(false);
        when(passwordService.generarHash("password-inicial-seguro")).thenReturn("hash-seguro");

        SuperUsuarioInitializer initializer = new SuperUsuarioInitializer(
                usuarioRepository,
                passwordService,
                "41131131",
                "password-inicial-seguro",
                "Administrador",
                "Inicial"
        );
        initializer.run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario usuario = captor.getValue();
        assertEquals("41131131", usuario.getDni());
        assertEquals("hash-seguro", usuario.getPasswordHash());
        assertEquals(RolUsuario.SUPERUSUARIO, usuario.getRol());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
        assertTrue(usuario.isDebeCambiarPassword());
    }

    @Test
    void noModificaUnSuperusuarioExistente() throws Exception {
        when(usuarioRepository.existsByDni("41131131")).thenReturn(true);

        SuperUsuarioInitializer initializer = new SuperUsuarioInitializer(
                usuarioRepository,
                passwordService,
                "41131131",
                "password-inicial-seguro",
                "Administrador",
                "Inicial"
        );
        initializer.run();

        verify(passwordService, never()).generarHash(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaCredencialesDeBootstrapVacias() {
        SuperUsuarioInitializer initializer = new SuperUsuarioInitializer(
                usuarioRepository,
                passwordService,
                "",
                "",
                "Administrador",
                "Inicial"
        );

        assertThrows(IllegalStateException.class, initializer::run);
        verify(usuarioRepository, never()).save(any());
    }
}
