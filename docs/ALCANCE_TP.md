# Alcance académico del trabajo grupal

Estado: propuesta inicial del equipo. Debe ajustarse si la cátedra exige un alcance específico.

## Objetivo

Evolucionar Red Clubes a partir de un sistema base ya existente, trabajando de manera colaborativa sobre funcionalidades, mantenibilidad, calidad y trazabilidad.

## Líneas de trabajo propuestas

El trabajo grupal puede concentrarse en cuatro frentes con responsabilidad principal diferenciada:

1. **Gestión y experiencia de socios**
   - mejoras de altas, edición, búsqueda y estados;
   - validaciones y manejo de errores;
   - mejoras de interfaz y pruebas.

2. **Grupos, actividades e inscripciones**
   - evolución del modelo de grupos/actividades;
   - inscripciones y reglas de cupo;
   - configuración contextual por club;
   - pruebas de reglas de negocio.

3. **Asistencia y operación diaria**
   - toma y consulta de asistencia;
   - permisos de profesores y operadores;
   - filtros por fecha/grupo;
   - validaciones y pruebas.

4. **Reportes, dashboard y calidad**
   - indicadores y reportes;
   - mejoras de dashboard;
   - refactor de componentes o servicios;
   - pruebas, documentación e integración.

## Qué no se cuenta automáticamente como desarrollo grupal nuevo

Las funcionalidades que ya estaban presentes en la rama `facultad` forman parte del baseline. Pueden ser refactorizadas, ampliadas o corregidas, pero la contribución nueva debe quedar demostrada por el diff y los Pull Requests posteriores al baseline.

## Criterios de aceptación

Una tarea se considera completada cuando:

- tiene alcance verificable;
- compila y pasa las pruebas aplicables;
- no rompe aislamiento por club ni permisos;
- incluye migración Flyway si modifica esquema;
- incluye pruebas nuevas o actualizadas cuando corresponde;
- tiene Pull Request con descripción y evidencia de validación;
- queda documentada la autoría real.

## Flujo de ramas

```text
main
└── versión estable

facultad
└── snapshot del punto de partida académico

develop
└── integración del trabajo del equipo

feature/*
fix/*
docs/*
└── cambios individuales o por tarea
```

Las ramas de trabajo deben salir de `develop` y volver mediante Pull Request.
