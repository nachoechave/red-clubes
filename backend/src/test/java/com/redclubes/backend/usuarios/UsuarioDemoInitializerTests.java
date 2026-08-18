package com.redclubes.backend.usuarios;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDemoInitializerTests {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordService passwordService;

    @Test
    void creaSuperusuarioDemoConPasswordHasheada() throws Exception {
        when(usuarioRepository.existsByDni(UsuarioDemoInitializer.DNI)).thenReturn(false);
        when(passwordService.generarHash(UsuarioDemoInitializer.PASSWORD)).thenReturn("hash-seguro");

        UsuarioDemoInitializer initializer = new UsuarioDemoInitializer(usuarioRepository, passwordService);
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
    void noModificaUnUsuarioDemoExistente() throws Exception {
        when(usuarioRepository.existsByDni(UsuarioDemoInitializer.DNI)).thenReturn(true);

        UsuarioDemoInitializer initializer = new UsuarioDemoInitializer(usuarioRepository, passwordService);
        initializer.run();

        verify(passwordService, never()).generarHash(UsuarioDemoInitializer.PASSWORD);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
