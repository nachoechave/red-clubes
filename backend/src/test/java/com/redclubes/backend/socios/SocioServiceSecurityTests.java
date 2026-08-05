package com.redclubes.backend.socios;

import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocioServiceSecurityTests {

    @Mock
    private SocioRepository socioRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void administradorDelClubANoPuedeActualizarSocioDelClubB() {
        SocioService service = new SocioService(socioRepository, clubRepository, auditoriaService);
        ActualizarSocioRequest cambios = new ActualizarSocioRequest(
                "Nombre", "Apellido", "12345678", "ACTIVO",
                null, null, null, null, null, null, null
        );
        when(socioRepository.findByIdAndClubId(20L, 1L)).thenReturn(Optional.empty());

        assertThrows(SocioNoEncontradoException.class,
                () -> service.actualizarSocioEnClub(1L, 20L, cambios, new Usuario()));

        verify(socioRepository).findByIdAndClubId(20L, 1L);
        verify(socioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
