# Red Clubes - Modelo de datos inicial

## 1. Objetivo del documento

Este documento define las entidades principales del sistema Red Clubes.

Una entidad representa un elemento importante del sistema que necesita ser guardado, consultado o relacionado con otros datos.

Este modelo inicial servirá como base para crear más adelante:

- Tablas en MySQL.
- Entidades en Java.
- Endpoints en Spring Boot.
- Pantallas y formularios en Angular.

---

## 2. Entidades principales

Las entidades iniciales del sistema serán:

- Club
- Usuario
- Rol
- Socio
- Actividad
- Inscripcion
- Cuota
- Asistencia
- Evento

---

## 3. Club

Representa una institución administrada dentro del sistema.

Puede ser un club de abuelos, centro de jubilados o club barrial.

### Campos iniciales

| Campo | Tipo conceptual | Descripción |

| id | Número | Identificador único del club |
| nombre | Texto | Nombre del club |
| direccion | Texto | Dirección física |
| telefono | Texto | Teléfono de contacto |
| email | Texto | Email institucional |
| responsable | Texto | Persona responsable del club |
| estado | Texto | Estado del club: ACTIVO o INACTIVO |

### Reglas iniciales

- Un club puede tener muchos socios.
- Un club puede tener muchas actividades.
- Un club puede tener muchos eventos.
- Un club puede tener varios usuarios asociados.

---

## 4. Rol

Representa el nivel de permiso de un usuario dentro del sistema.

### Roles iniciales

| Rol | Descripción |
|---|---|
| ADMIN_GENERAL | Puede administrar todo el sistema |
| ENCARGADO_CLUB | Puede gestionar la información diaria de un club |
| PROFESOR | Puede consultar sus actividades y marcar asistencia |
| CONSULTA | Solo puede ver información y reportes |

### Reglas iniciales

- Un usuario tendrá un rol.
- El rol define qué acciones puede realizar el usuario.

---

## 5. Usuario

Representa a una persona que puede ingresar al sistema.

No todos los socios son usuarios del sistema. Un usuario es alguien que administra o consulta información dentro de Red Clubes.

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único |
| nombre | Texto | Nombre de la persona |
| apellido | Texto | Apellido de la persona |
| email | Texto | Email usado para iniciar sesión |
| password | Texto | Contraseña cifrada |
| rol | Relación | Rol del usuario |
| club | Relación | Club al que pertenece, si corresponde |
| estado | Texto | ACTIVO o INACTIVO |

### Reglas iniciales

- El email debe ser único.
- La contraseña no debe guardarse en texto plano.
- Un usuario puede estar asociado a un club.
- Un administrador general podría no estar limitado a un solo club.

---

## 6. Socio

Representa a una persona asociada al club.

Es una de las entidades más importantes del sistema.

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único interno |
| numero_socio | Número | Número visible de socio |
| nombre | Texto | Nombre del socio |
| apellido | Texto | Apellido del socio |
| dni | Texto | Documento del socio |
| fecha_nacimiento | Fecha | Fecha de nacimiento |
| telefono | Texto | Teléfono de contacto |
| email | Texto | Email del socio, opcional |
| direccion | Texto | Dirección del socio |
| contacto_emergencia_nombre | Texto | Nombre del contacto de emergencia |
| contacto_emergencia_telefono | Texto | Teléfono del contacto de emergencia |
| contacto_emergencia_vinculo | Texto | Relación con el socio |
| estado | Texto | ACTIVO, INACTIVO o BAJA |
| club | Relación | Club al que pertenece |

### Reglas iniciales

- Un socio pertenece a un club.
- Un socio puede inscribirse a muchas actividades.
- Un socio puede tener muchas cuotas.
- Un socio puede tener muchas asistencias.
- El DNI no debería repetirse dentro del mismo club.
- No conviene eliminar físicamente un socio, sino cambiar su estado a BAJA.

---

## 7. Actividad

Representa una actividad, taller o clase ofrecida por un club.

Ejemplos:

- Yoga
- Folklore
- Gimnasia suave
- Taller de memoria
- Tejido
- Pintura
- Ajedrez

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único |
| nombre | Texto | Nombre de la actividad |
| descripcion | Texto | Descripción breve |
| categoria | Texto | Salud, Cultura, Taller, Recreación, etc. |
| dia | Texto | Día o días de cursada |
| horario_inicio | Hora | Horario de inicio |
| horario_fin | Hora | Horario de finalización |
| cupo_maximo | Número | Cantidad máxima de inscriptos |
| profesor | Relación | Usuario o persona responsable |
| club | Relación | Club al que pertenece |
| estado | Texto | ACTIVA o INACTIVA |

### Reglas iniciales

- Una actividad pertenece a un club.
- Una actividad puede tener muchos socios inscriptos.
- Una actividad puede tener muchas asistencias.
- No se debería permitir superar el cupo máximo.
- Una actividad puede desactivarse sin borrar su historial.

---

## 8. Inscripcion

Representa la relación entre un socio y una actividad.

Es necesaria porque un socio puede anotarse en muchas actividades, y una actividad puede tener muchos socios.

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único |
| socio | Relación | Socio inscripto |
| actividad | Relación | Actividad a la que se inscribe |
| fecha_inscripcion | Fecha | Fecha en la que se realizó la inscripción |
| estado | Texto | ACTIVA, BAJA o EN_ESPERA |

### Reglas iniciales

- No debería existir una inscripción duplicada activa para el mismo socio y la misma actividad.
- Antes de inscribir, se debe validar el cupo disponible.
- Una inscripción puede darse de baja sin perder el historial.

---

## 9. Cuota

Representa una cuota mensual o pago asociado a un socio.

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único |
| socio | Relación | Socio al que pertenece la cuota |
| mes | Número | Mes correspondiente |
| anio | Número | Año correspondiente |
| monto | Decimal | Importe de la cuota |
| estado | Texto | PENDIENTE, PAGADA, VENCIDA o ANULADA |
| fecha_vencimiento | Fecha | Fecha límite de pago |
| fecha_pago | Fecha | Fecha en la que se pagó |
| medio_pago | Texto | Efectivo, transferencia, mercado pago, etc. |
| observacion | Texto | Comentario opcional |

### Reglas iniciales

- Una cuota pertenece a un socio.
- Un socio puede tener muchas cuotas.
- Una cuota pagada debe registrar fecha de pago.
- Una cuota pendiente puede pasar a vencida.
- No debería haber dos cuotas del mismo mes y año para el mismo socio.

---

## 10. Asistencia

Representa la presencia o ausencia de un socio en una actividad en una fecha determinada.

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único |
| socio | Relación | Socio relacionado |
| actividad | Relación | Actividad relacionada |
| fecha | Fecha | Fecha de la clase o encuentro |
| presente | Booleano | Indica si asistió o no |
| observacion | Texto | Comentario opcional |

### Reglas iniciales

- Una asistencia pertenece a un socio y a una actividad.
- Solo deberían marcarse asistencias de socios inscriptos en esa actividad.
- No debería duplicarse la asistencia del mismo socio, actividad y fecha.

---

## 11. Evento

Representa un evento organizado por el club.

Ejemplos:

- Peña
- Viaje
- Cumpleaños del mes
- Almuerzo comunitario
- Bingo

### Campos iniciales

| Campo | Tipo conceptual | Descripción |
|---|---|---|
| id | Número | Identificador único |
| titulo | Texto | Nombre del evento |
| descripcion | Texto | Detalle del evento |
| fecha | Fecha | Fecha del evento |
| hora | Hora | Horario del evento |
| lugar | Texto | Lugar donde se realiza |
| cupo | Número | Cupo máximo, si aplica |
| costo | Decimal | Costo del evento, si aplica |
| estado | Texto | ACTIVO, CANCELADO o FINALIZADO |
| club | Relación | Club organizador |

### Reglas iniciales

- Un evento pertenece a un club.
- Un club puede tener muchos eventos.
- En una versión futura, los socios podrán inscribirse a eventos.

---

## 12. Relaciones principales

### Club y Socio

Un club puede tener muchos socios.

