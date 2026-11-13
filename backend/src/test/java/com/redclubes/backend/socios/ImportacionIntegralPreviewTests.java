package com.redclubes.backend.socios;

import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImportacionIntegralPreviewTests {
    SocioRepository socios;
    ClubRepository clubes;
    JdbcTemplate jdbc;
    AuditoriaService auditoria;
    ImportacionIntegralService service;

    @BeforeEach void setup() {
        socios=mock(SocioRepository.class);
        clubes=mock(ClubRepository.class);
        jdbc=mock(JdbcTemplate.class);
        auditoria=mock(AuditoriaService.class);
        service=new ImportacionIntegralService(socios,clubes,jdbc,auditoria);
        when(clubes.findByIdForUpdate(1L)).thenReturn(Optional.of(new Club()));
        when(socios.findByClubId(1L)).thenReturn(List.of());
    }

    @Test void vistaPreviaConPagosPendientesYVaciosNoEscribe() {
        String csv="nombre;apellido;dni;numero_socio;enero;febrero;marzo\n"+
                "Ana;Perez;12345678;25;Pagó;No pagó;\n";
        var result=service.procesar(1L,csv,2026,false,null);
        assertTrue(result.valida());
        assertFalse(result.ejecutada());
        assertEquals(1,result.sociosNuevos());
        assertEquals(1,result.mesesPagados());
        assertEquals(1,result.mesesPendientes());
        assertEquals(10,result.mesesVacios());
        verify(socios,never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }

    @Test void controlConNombreCompuestoNoInventaApellido() {
        String csv="Año 2026,,,,\nNombre y apellido,N° de socio,DNI,Enero\n"+
                "Nombre Largo de Prueba Compuesto,4,12345678,Pagó\n";
        var result=service.procesar(1L,csv,2026,false,null);
        assertFalse(result.valida());
        assertTrue(result.errores().stream().anyMatch(e -> e.mensaje().contains("ambiguo")));
        verify(socios,never()).saveAndFlush(any());
    }

    @Test void rechazoCuandoAnioDelArchivoDifiere() {
        String csv="Año 2025,,,,\nNombre y apellido,N° de socio,DNI,Enero\n"+
                "Ana Perez,4,12345678,Pagó\n";
        var result=service.procesar(1L,csv,2026,false,null);
        assertFalse(result.valida());
        assertTrue(result.errores().stream().anyMatch(e -> e.mensaje().contains("año")));
    }
}
