package com.redclubes.backend.socios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocioServiceSecurityTests {

    private SocioService service() {
        return new SocioService(socioRepository, clubRepository, auditoriaService);
    }

    @Mock
    private SocioRepository socioRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void importaSociosRespetandoNumeroYFechaAlta() {
        Club club = new Club();
        club.setId(1L);

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findByClubIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(socioRepository.save(any(Socio.class))).thenAnswer(invocation -> {
            Socio socio = invocation.getArgument(0);
            socio.setId(10L);
            return socio;
        });

        String csv = "nombre;apellido;dni;numero_socio;telefono;email;fecha_nacimiento;fecha_alta;direccion;estado\n"
                + "Ana;Perez;12345678;25;2215551234;ana@example.com;1980-05-10;2020-01-15;Calle 1 123;ACTIVO\n";

        ImportacionSociosResponse resultado = service().importarSocios(1L, csv, new Usuario());

        assertTrue(resultado.valida());
        assertEquals(1, resultado.importados());
        assertEquals(25, resultado.sociosImportados().getFirst().numeroSocio());

        org.mockito.ArgumentCaptor<Socio> captor = org.mockito.ArgumentCaptor.forClass(Socio.class);
        verify(socioRepository).save(captor.capture());
        Socio guardado = captor.getValue();
        assertEquals("12345678", guardado.getDni());
        assertEquals(25, guardado.getNumeroSocio());
        assertEquals(LocalDate.of(2020, 1, 15), guardado.getFechaAlta());
    }

    @Test
    void importacionSociosEsAtomicaSiHayDniDuplicado() {
        Club club = new Club();
        club.setId(1L);
        Socio existente = new Socio();
        existente.setId(5L);
        existente.setDni("99999999");
        existente.setNumeroSocio(3);

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(socioRepository.findByClubIdOrderByIdAsc(1L)).thenReturn(List.of(existente));

        String csv = "nombre;apellido;dni\n"
                + "Ana;Perez;12345678\n"
                + "Juan;Lopez;99999999\n";

        ImportacionSociosResponse resultado = service().importarSocios(1L, csv, new Usuario());

        assertFalse(resultado.valida());
        assertEquals(2, resultado.totalFilas());
        assertEquals(0, resultado.importados());
        assertEquals(1, resultado.errores().size());
        verify(socioRepository, never()).save(any());
    }

    @Test
    void administradorDelClubANoPuedeActualizarSocioDelClubB() {
        SocioService service = service();
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
