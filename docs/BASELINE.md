# Baseline académico — Red Clubes

Fecha de corte: 22 de septiembre de 2026.

## Punto de partida

El trabajo grupal de facultad toma como base el estado existente de Red Clubes conservado en la rama `facultad`.

- Rama baseline: `facultad`
- Commit baseline: `994134d04582ba581ec6fe73fa6f516c24b336c8`
- Rama de integración del equipo: `develop`
- Rama estable general del repositorio: `main`

La rama `facultad` funciona como una fotografía del código previo al trabajo grupal. No debe reescribirse ni utilizarse para desarrollo cotidiano.

## Funcionalidad preexistente

Antes del inicio formal del trabajo grupal ya existían, entre otros componentes:

- backend Spring Boot y frontend Angular;
- persistencia MySQL y migraciones Flyway;
- autenticación, sesiones, roles y permisos;
- soporte multi-club;
- socios;
- actividades;
- inscripciones;
- asistencia;
- cuotas y pagos;
- usuarios;
- auditoría;
- reportes;
- Docker Compose;
- pruebas automatizadas y CI;
- controles de seguridad, backup y restore.

Estas partes constituyen el punto de partida técnico y no deben presentarse como trabajo nuevo del grupo cuando no lo sean.

## Criterio de trazabilidad

A partir de este baseline, toda contribución académica debe poder identificarse mediante:

1. un Issue o tarea concreta;
2. una rama de trabajo creada desde `develop`;
3. commits descriptivos;
4. un Pull Request hacia `develop`;
5. revisión de al menos otro integrante cuando corresponda;
6. CI en verde antes de integrar.

No se reescribe el historial previo para atribuir trabajo anterior a integrantes que se incorporan ahora.
