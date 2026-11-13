package com.redclubes.backend.socios;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportacionIntegralServiceTests {
    @Test void csvConTituloYComasPermiteEncontrarLaCabecera() {
        String csv = "CONTROL DE CUOTAS,,,,\nAño 2026,,,,\n" +
                "Nombre y apellido,N° de socio,DNI,Enero,Febrero\n" +
                "Ana Perez,2,12.345.678,Pagó,No pagó\n";
        List<List<String>> rows=ImportacionIntegralService.parsear(csv);
        assertEquals("Nombre y apellido",rows.get(2).get(0));
        assertEquals("Pagó",rows.get(3).get(3));
    }

    @Test void comillasYSeparadorPuntoYComaSonCompatibles() {
        String csv="nombre;apellido;dni;enero\n\"Ana; María\";Pérez;12345678;Pagó\n";
        var rows=ImportacionIntegralService.parsear(csv);
        assertEquals("Ana; María",rows.get(1).get(0));
        assertEquals("Pagó",rows.get(1).get(3));
    }

    @Test void noAceptaCsvConComillasSinCerrar() {
        assertThrows(IllegalArgumentException.class,
                ()->ImportacionIntegralService.parsear("nombre,apellido\n\"sin cerrar"));
    }
}
