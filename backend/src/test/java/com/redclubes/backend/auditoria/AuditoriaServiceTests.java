package com.redclubes.backend.auditoria;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTests {
    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private ClubRepository clubRepository;

    @Test
    void registraActorClubEntidadYNormalizaElDetalle() {
        Club club = new Club();
        club.setId(2L);
        Usuario actor = new Usuario();
        actor.setId(3L);
        when(clubRepository.findById(2L)).thenReturn(Optional.of(club));

        new AuditoriaService(auditoriaRepository, clubRepository)
                .registrar(actor, 2L, "MODIFICACION", "SOCIO", 4L, "linea 1\nlinea 2");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria registrada = captor.getValue();
        assertEquals(actor, registrada.getUsuario());
        assertEquals(club, registrada.getClub());
        assertEquals(4L, registrada.getEntidadId());
        assertFalse(registrada.getDetalle().contains("\n"));
    }
}
