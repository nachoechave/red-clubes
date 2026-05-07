# Red Clubes - Alcance del MVP

## 1. Descripción del proyecto

Red Clubes es un sistema web pensado para la administración de clubes de abuelos, centros de jubilados y clubes barriales.

El objetivo principal es ayudar a organizar la información diaria del club en un solo lugar, evitando depender exclusivamente de cuadernos, planillas sueltas o registros desordenados.

El sistema permitirá administrar socios, actividades, cuotas, asistencias, eventos y reportes básicos.

---

## 2. Problema que busca resolver

Muchos clubes y centros de jubilados manejan su información de forma manual o dispersa.

Algunos problemas comunes son:

- Dificultad para encontrar rápidamente los datos de un socio.
- Falta de control claro sobre cuotas pagadas y pendientes.
- Listas de actividades o talleres desactualizadas.
- Problemas para saber cuántas personas están inscriptas en cada actividad.
- Falta de historial de asistencia.
- Poca información disponible para tomar decisiones.
- Dificultad para generar reportes simples.

Red Clubes busca centralizar esa información y facilitar el trabajo de administradores, encargados y profesores.

---

## 3. Objetivo del MVP

El MVP, o Producto Mínimo Viable, será la primera versión funcional del sistema.

No tendrá todas las funcionalidades posibles, pero sí las necesarias para que el sistema ya sea útil.

El objetivo del MVP es permitir que un club pueda:

- Registrar socios.
- Consultar y editar la ficha de un socio.
- Crear actividades.
- Inscribir socios a actividades.
- Registrar cuotas.
- Identificar socios con cuotas pendientes.
- Marcar asistencias básicas.
- Ver un resumen general desde un dashboard.

---

## 4. Usuarios iniciales

### Administrador general

Usuario con permisos completos sobre el sistema.

Puede administrar clubes, usuarios, socios, actividades, cuotas, asistencias y reportes.

### Encargado del club

Usuario que trabaja en la administración diaria del club.

Puede cargar socios, modificar datos, crear actividades, registrar pagos y consultar reportes básicos.

### Profesor o tallerista

Usuario encargado de una o varias actividades.

Puede consultar sus actividades, ver los socios inscriptos y marcar asistencia.

### Usuario de consulta

Usuario con permisos de solo lectura.

Puede ver información general y reportes, pero no modificar datos.

---

## 5. Módulos incluidos en el MVP

### Login básico

Permite que los usuarios ingresen al sistema con email y contraseña.

### Dashboard

Pantalla principal con indicadores generales del club.

Ejemplos:

- Cantidad de socios activos.
- Cuotas pendientes.
- Actividades activas.
- Próximos eventos.
- Cumpleaños del mes.

### Socios

Permite administrar los datos de los socios.

Funciones iniciales:

- Crear socio.
- Editar socio.
- Buscar socio por nombre o DNI.
- Ver ficha de socio.
- Cambiar estado del socio.

### Actividades

Permite administrar talleres y actividades del club.

Funciones iniciales:

- Crear actividad.
- Editar actividad.
- Ver actividades activas.
- Consultar cupo disponible.
- Consultar socios inscriptos.

### Inscripciones

Permite relacionar socios con actividades.

Funciones iniciales:

- Inscribir un socio a una actividad.
- Evitar inscripciones duplicadas.
- Validar cupo disponible.
- Dar de baja una inscripción.

### Cuotas

Permite registrar el estado de pago de los socios.

Funciones iniciales:

- Crear cuota.
- Registrar pago.
- Ver cuotas pendientes.
- Ver socios morosos.
- Consultar historial de pagos de un socio.

### Asistencias básicas

Permite marcar la presencia de los socios en actividades.

Funciones iniciales:

- Seleccionar actividad.
- Seleccionar fecha.
- Marcar presentes y ausentes.
- Consultar asistencia de una actividad.

### Eventos básicos

Permite registrar eventos importantes del club.

Funciones iniciales:

- Crear evento.
- Editar evento.
- Ver próximos eventos.

### Reportes básicos

Permite consultar información útil para la administración.

Reportes iniciales:

- Socios activos.
- Socios morosos.
- Actividades con más inscriptos.
- Cuotas cobradas.
- Cumpleaños del mes.

---

## 6. Módulos no incluidos en la primera versión

Estas funcionalidades son interesantes, pero quedan para futuras versiones:

- Integración automática con WhatsApp.
- Carnet digital con QR.
- Pagos online.
- Aplicación móvil.
- Notificaciones automáticas.
- Multi-club avanzado.
- Exportaciones avanzadas.
- Firma digital.
- Integración con sistemas municipales.
- Gestión de inventario.
- Gestión contable completa.

---

## 7. Criterios para considerar completo el MVP

El MVP se considerará completo cuando se pueda realizar el siguiente flujo:

1. Un usuario inicia sesión.
2. El usuario accede al dashboard.
3. El usuario registra un socio.
4. El usuario crea una actividad.
5. El usuario inscribe al socio en una actividad.
6. El usuario registra una cuota.
7. El usuario marca una asistencia.
8. El usuario consulta un reporte básico.

---

## 8. Tecnologías previstas

### Frontend

Angular.

Se encargará de las pantallas, formularios, navegación e interacción con el usuario.

### Backend

Java con Spring Boot.

Se encargará de la lógica del sistema, validaciones, reglas de negocio y comunicación con la base de datos.

### Base de datos

MySQL.

Se encargará de guardar socios, actividades, cuotas, asistencias, eventos y usuarios.

### Control de versiones

Git y GitHub.

Se usará un flujo de trabajo con issues, ramas, commits y pull requests.

---

## 9. Primera versión esperada

La primera versión de Red Clubes no busca ser perfecta ni tener todas las funciones posibles.

Busca ser una base sólida para aprender, practicar y construir un sistema real de forma ordenada.

El foco principal será:

- Aprender Angular.
- Aprender Java con Spring Boot.
- Aprender MySQL.
- Practicar Git y GitHub.
- Construir un proyecto con lógica real.