package com.redclubes.backend.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GruposMigrationTests {
    @Test void migrarV6ConservaIdentidadesYRelaciones() throws Exception {
        String url = "jdbc:h2:mem:migracion_grupos;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway.configure().dataSource(url, "sa", "").placeholders(Map.of("logo_type", "CLOB"))
                .target("6").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("INSERT INTO club(id,nombre,direccion,estado) VALUES(1,'Club historico','Calle 1','ACTIVO')");
            sql.execute("INSERT INTO usuario(id,dni,nombre,apellido,rol,estado,password_hash,debe_cambiar_password) VALUES(1,'12345678','Ana','Perez','PROFESOR','ACTIVO','hash',FALSE)");
            sql.execute("INSERT INTO socio(id,nombre,apellido,dni,estado,club_id) VALUES(1,'Juan','Perez','23456789','ACTIVO',1)");
            sql.execute("INSERT INTO actividad(id,nombre,profesor,profesor_usuario_id,dias,categoria,icono,cupo,inscriptos,estado,club_id) VALUES(1,'Yoga','Ana Perez',1,'Lunes','Salud','check',32,1,'ACTIVA',1)");
            sql.execute("INSERT INTO inscripcion_actividad(id,club_id,socio_id,actividad_id,fecha_inscripcion,estado) VALUES(1,1,1,1,CURRENT_DATE,'ACTIVA')");
            sql.execute("INSERT INTO asistencia(id,fecha,presente,club_id,actividad_id,socio_id,estado) VALUES(1,CURRENT_DATE,TRUE,1,1,1,'PRESENTE')");
            sql.execute("INSERT INTO usuario_actividad(id,usuario_id,club_id,actividad_id) VALUES(1,1,1,1)");
        }
        Flyway.configure().dataSource(url, "sa", "").placeholders(Map.of("logo_type", "CLOB")).load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            try (var row = sql.executeQuery("SELECT a.tipo,a.asistencia_habilitada,a.cupo,a.nombre,a.profesor_usuario_id FROM actividad a JOIN inscripcion_actividad i ON i.actividad_id=a.id JOIN asistencia s ON s.actividad_id=a.id JOIN usuario_actividad u ON u.actividad_id=a.id WHERE a.id=1 AND i.socio_id=s.socio_id AND a.club_id=i.club_id AND a.club_id=s.club_id")) {
                assertTrue(row.next());
                assertEquals("ACTIVIDAD", row.getString(1));
                assertTrue(row.getBoolean(2));
                assertEquals(32, row.getInt(3));
                assertEquals("Yoga", row.getString(4));
                assertEquals(1, row.getLong(5));
                assertFalse(row.next());
            }
            try (var row = sql.executeQuery("SELECT COUNT(*) FROM club_modulo WHERE club_id=1")) {
                assertTrue(row.next()); assertEquals(5, row.getInt(1));
            }
        }
    }
}
