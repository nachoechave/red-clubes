# Checklist ejecutable del piloto ficticio

Estado inicial de cada casilla: **PENDIENTE EN SERVIDOR REAL**. Copiar este documento al registro privado de la operación. Por cada casilla: fecha UTC, operador, comando/acción, resultado esperado/obtenido y evidencia sin tokens/passwords. Nunca marcar aprobado por existir un script.

Release/tag: ____ SHA: ____ imagen backend: ____ frontend: ____ MySQL: ____
Host/dominio: ____ Responsable: ____ Ventana: ____ Resultado final: ____

## Infraestructura

- [ ] Usuario dedicado, .env propio 0600, backup 0700, scripts/release sin cambios; grupo Docker reservado a operadores.
- [ ] Docker Engine/Compose v2 versiones registradas; servicio habilitado al boot.
- [ ] `timedatectl status`: NTP activo, zona documentada; `df -h; df -i; free -h`: espacio/RAM suficiente.
- [ ] DNS A y AAAA (si aplica) coinciden desde autoritativo y dos resolvers; nslookup desde otra red.
- [ ] HTTPS válido; HTTP→HTTPS; `certbot renew --dry-run` y reload hook aprobados; timer activo.
- [ ] Firewall host/cloud: 22 restringido, 80/443 públicos; segunda sesión SSH por clave probada.
- [ ] `ss -lntp` y `docker ps`: APP_PORT solo loopback; sin DB/backend; prueba externa de puertos cerrados.
- [ ] Peer/CIDR exacto e IP real anti-spoof comprobados según PROXY_Y_RATE_LIMIT.
- [ ] Containers healthy; Flyway V1–V6 success sin baseline en DB nueva; Hibernate validate.
- [ ] Smoke local y HTTPS externo; headers en HTML; assets cargan sin errores CSP.
- [ ] Reboot autorizado: app vuelve sola, MySQL recupera, backend se recupera, Nginx/health/timers/IP real siguen correctos.
- [ ] Monitoreo ejecutado, fallo simulado y aviso recibido por responsable; registrar tiempo de respuesta.

## Dataset inequívocamente ficticio

Mantener DEMO_DATA_ENABLED=false: el seed existente contiene identificadores/contactos con apariencia real y no crea usuarios. No modificarlo ni usar datos de personas. Estos identificadores de **diez dígitos 9900000000+ son valores sintéticos**, no documentos emitidos; satisfacen el contrato numérico de login/socios y VARCHAR(10). No recortar a ocho dígitos. Nombres deben incluir PILOTO. Correos `a1@example.invalid`; teléfonos/direcciones/emergencias opcionales vacíos o texto explícito de prueba (sin contacto de persona).

| Entidad | Club A: Club Atlético Norte PILOTO | Club B: Club Social Sur PILOTO |
|---|---|---|
| Administrador | Ana Admin PILOTO, 9900000011 | Bruno Admin PILOTO, 9900000021 |
| Profesor | Pablo Profesor PILOTO, 9900000012 | Paula Profesora PILOTO, 9900000022 |
| Operador | Olga Operadora PILOTO, 9900000013 | Omar Operador PILOTO, 9900000023 |
| Socio activo 1 | Juan Prueba PILOTO, 9900000101 | María Demo PILOTO, 9900000201 |
| Socio activo 2 | Julia Prueba PILOTO, 9900000102 | Mario Demo PILOTO, 9900000202 |
| Socio inactivo | Inactivo Norte PILOTO, 9900000103 | Inactivo Sur PILOTO, 9900000203 |
| Actividad | Taller Norte PILOTO, cupo 2 | Taller Sur PILOTO, cupo 2 |
| Cuota | período de ensayo registrado, importe 1000 | mismo período, importe 1500 |

Superusuario de bootstrap: 9900000000; usuario separado de rate test: 9900000090. Passwords fuertes diferentes generados por operador, nunca en esta tabla. Si esos registros ya existen en una DB compartida, usar DB de piloto NUEVA, no sobrescribir.

## Aplicación y cuentas

- [ ] Bootstrap crea SUPERUSUARIO; cambiar password; enabled=false y eliminar DNI/PASSWORD; recrear contenedor.
- [ ] Login correcto, password incorrecto 401; sesión restaura tras recargar navegador; /me responde.
- [ ] Cambio de password: nueva funciona, anterior rechazada; no registrar bearer en evidencia.
- [ ] Logout: mismo token devuelve 401; navegación protegida pide sesión.
- [ ] Expiración de sesión verificada con AUTH_SESSION_HOURS configurado (esperar o ensayar en entorno descartable).
- [ ] Superusuario crea ambos clubes y sus administradores. Admin local no puede crear administradores/superusuarios.
- [ ] Cuenta rate test: cinco fallos/bloqueo/expiración/login y spoof XFF según procedimiento, A/B con distinta IP.

## Club A y Club B (ejecutar CADA fila en ambos)

| Acción / aceptación | A | B |
|---|:---:|:---:|
| Superusuario crea club y administrador exclusivo; registrar IDs | [ ] | [ ] |
| Admin crea profesor y operador del club, password inicial cambiado | [ ] | [ ] |
| Crear socios activos/inactivo; buscar, editar y verificar persistencia | [ ] | [ ] |
| Crear actividad con profesor propio/cupo/horario; editar y consultar | [ ] | [ ] |
| Inscribir dos activos; duplicado rechazado; inactivo/cupo excedido rechazados | [ ] | [ ] |
| Profesor ve solo su actividad; asistencia en día/fecha de clase actual; repetir actualiza sin duplicar | [ ] | [ ] |
| Generar cuotas del período; segunda generación no duplica | [ ] | [ ] |
| Registrar pago completo; verificar estado e importe; repetir pago rechazado | [ ] | [ ] |
| Anular pago y verificar deuda/auditoría; registrar nuevamente si prueba requiere | [ ] | [ ] |
| Reporte período/actividad concuerda con socios, deuda y pagos activos registrados | [ ] | [ ] |

Registrar conteos/totales al finalizar en una tabla privada; no presumir cuántas cuotas genera para inactivos: comparar con reglas y respuesta de la aplicación. El piloto detecta discrepancias, no autoriza cambiarlas silenciosamente.

## Aislamiento negativo

Como admin/operador/profesor A y luego simétricamente B:

- [ ] No listar/consultar club ajeno ni sus reportes.
- [ ] No editar/borrar socio, inscripción, asistencia, cuota, pago ni usuario ajeno.
- [ ] Sustituir manualmente clubId/ID de recursos en ruta usando IDs previamente registrados.
- [ ] Mezclar club A en ruta con ID de entidad B en body/query.
- [ ] Ejecutar llamadas API directas desde cliente local con token de A; verificar 403/404 según endpoint y ausencia de datos B.
- [ ] Reabrir B con usuario autorizado y verificar que las peticiones denegadas NO modificaron registros.
- [ ] Profesor solo actividad asignada/día actual; operador no administra usuarios; admin A no afecta usuario multiclub fuera de su alcance.
- [ ] Sin token →401; logout revoca. Nunca incluir Authorization en capturas/tickets.

Usar solo IDs del dataset; no hacer exploración masiva. Las pruebas automatizadas de aislamiento complementan, no sustituyen, estas pruebas de aceptación.

## Backup, restore y rollback

- [ ] Generar backup vía servicio; exit 0, marcador actual, checksum y gzip correctos.
- [ ] Cifrado age; copia fuera del host comprobada desde otro dispositivo; clave privada custodiada separadamente.
- [ ] Simular destino no disponible: backup falla, marcador no avanza; restablecer y repetir.
- [ ] Retención y timer diario activos, logs con permisos y sin secretos.
- [ ] Descargar, verificar y descifrar una copia externa; restore aislado exitoso.
- [ ] Restore de ensayo completo: conteos/totales A/B iguales al corte y login/aislamiento válidos; medir RTO y RPO.
- [ ] Rollback de aplicación a imágenes anteriores conservadas ensayado; documentar compatibilidad de esquema.
- [ ] Procedimiento de recuperación de DB explicado/aprobado; no ejecutado sobre instancia de uso sin autorización.
- [ ] Monitoreo detecta backup >26h, containers caídos, disco >=80%, TLS <21 días y llega aviso al responsable.

## Puerta de aceptación

- [ ] Todas las pruebas ficticias aceptadas o incidencias bloqueantes resueltas.
- [ ] Evidencia CI/local separada de evidencia host; release inmutable e imágenes conservadas.
- [ ] Responsable de operación, backups, incidentes, datos y decisión de apertura asignado.
- [ ] Para datos reales: revisión de finalidad/accesos/retención, base histórica (si aplica), infraestructura y recuperación aprobadas. Esta checklist ficticia por sí sola NO autoriza esa carga.

**Dictamen permitido al cerrar este piloto:** infraestructura validada con datos ficticios. Datos reales requieren decisión posterior explícita.
