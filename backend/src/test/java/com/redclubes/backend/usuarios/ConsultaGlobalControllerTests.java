package com.redclubes.backend.usuarios;

import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.socios.SocioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultaGlobalControllerTests {
    private final AuthService auth=mock(AuthService.class);
    private final ClubRepository clubes=mock(ClubRepository.class);
    private final SocioRepository socios=mock(SocioRepository.class);
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final ConsultaGlobalController controller=new ConsultaGlobalController(auth,clubes,socios,jdbc);

    private void rol(RolUsuario rol) {
        Usuario actor=new Usuario();
        actor.setRol(rol);
        when(auth.obtenerUsuarioAutenticado("Bearer synthetic")).thenReturn(actor);
    }

    @Test void operadorNoAccedeADirectorioGlobal() {
        rol(RolUsuario.OPERADOR);
        assertThrows(AccesoDenegadoException.class,()->controller.consultarSocios("Bearer synthetic",null));
        verifyNoInteractions(socios,jdbc,clubes);
    }

    @Test void profesorNoPuedeConsultarResumenGlobal() {
        rol(RolUsuario.PROFESOR);
        assertThrows(AccesoDenegadoException.class,()->controller.resumen("Bearer synthetic"));
        verifyNoInteractions(clubes,socios,jdbc);
    }

    @Test void consultorPuedeConsultarSinModificarRegistros() {
        rol(RolUsuario.CONSULTOR_GLOBAL);
        when(clubes.findAll()).thenReturn(List.of());
        when(socios.findAll()).thenReturn(List.of());
        assertTrue(controller.resumen("Bearer synthetic").isEmpty());
        assertTrue(controller.consultarSocios("Bearer synthetic",null).isEmpty());
        verify(socios,never()).save(any());
        verify(clubes,never()).save(any());
    }
}
