package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.gestion.ActividadRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceSecurityTests {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioClubRepository usuarioClubRepository;
    @Mock private UsuarioActividadRepository usuarioActividadRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private PasswordService passwordService;
    @Mock private AuditoriaService auditoriaService;

    @Test
    void administradorDelClubANoPuedeCrearUsuarioEnClubB() {
        Usuario administrador = new Usuario("12345678", "Admin", "Local", RolUsuario.ADMINISTRADOR,
                EstadoUsuario.ACTIVO, "hash", false);
        administrador.setId(10L);
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "87654321", "Usuario", "Ajeno", RolUsuario.PROFESOR,
                null, null, "password-segura",
                List.of(new AsignacionUsuarioRequest(2L, RolClub.PROFESOR, List.of()))
        );
        when(usuarioRepository.existsByDni("87654321")).thenReturn(false);
        when(usuarioClubRepository.findByUsuarioIdAndClubId(10L, 2L)).thenReturn(Optional.empty());

        UsuarioService service = new UsuarioService(usuarioRepository, usuarioClubRepository,
                usuarioActividadRepository, clubRepository, actividadRepository, passwordService, auditoriaService);

        assertThrows(AccesoDenegadoException.class, () -> service.crearUsuario(request, administrador));
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
