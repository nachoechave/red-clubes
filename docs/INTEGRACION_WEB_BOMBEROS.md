# Inscripciones desde la web de Bomberos

## Circuito implementado

La web pública envía una solicitud. El socio no existe hasta que un administrador de la institución o un superusuario aprueba. Operadores, profesores y administradores de otras instituciones no pueden consultar ni resolver solicitudes. La aprobación no genera cuotas.

- Público: `POST /api/public/instituciones/bomberos-ensenada/solicitudes`.
- Privado: `GET /api/clubes/{clubId}/solicitudes?estado=PENDIENTE&pagina=0` (20 resultados por página).
- Privado: `POST /api/clubes/{clubId}/solicitudes/{id}/aprobar` con los datos corregidos.
- Privado: `POST /api/clubes/{clubId}/solicitudes/{id}/rechazar` con `{"motivo":"..."}`.

El formulario envía:

```json
{
  "datos": {"nombre":"Ana","apellido":"Prueba","dni":"99111222","email":"prueba@example.invalid","telefono":"2215551234"},
  "consentimiento": true,
  "consentimientoVersion": "2026-10-v1",
  "claveEnvio": "48be90fa-67aa-49f6-94d9-367c2f350ce6",
  "sitioWeb": ""
}
```

La clave UUID identifica un envío y se conserva ante errores de red. La misma clave con otros datos se rechaza. No se guardan datos personales en sessionStorage: solo la clave. Los envíos repetidos del mismo DNI pendiente no duplican registros. Una solicitud rechazada permite otra presentación con una clave nueva. La respuesta pública es siempre genérica para no revelar el padrón.

La aprobación se serializa por institución, al igual que las altas e importaciones de socios, para proteger DNI y numeración. Repetir una aprobación devuelve el socio ya creado. Un rechazo no puede aprobarse después. Las resoluciones registran usuario, fecha, resultado y auditoría.

## Configuración

En Red Clubes, `.env` (el ID debe verificarse en esa instalación):

```dotenv
PUBLIC_ENROLLMENT_ENABLED=true
PUBLIC_ENROLLMENT_CLUB_ID=1
PUBLIC_ENROLLMENT_SLUG=bomberos-ensenada
PUBLIC_FORM_ORIGINS=http://localhost:4201,http://127.0.0.1:4201
```

Deshabilitado por defecto. El cliente nunca puede elegir `clubId`, estado, número de socio ni cuotas. La institución debe estar activa y tener PARTICIPANTES habilitado.

La web usa una ruta relativa. Para desarrollo, `npm start -- --host 127.0.0.1 --port 4201` y `proxy.conf.json` reenvía exclusivamente `/api/public` a Red Clubes en el puerto 8080. En producción el servidor de la web debe reenviar esta ruta a Nginx de Red Clubes. Configurar el origen HTTPS real en PUBLIC_FORM_ORIGINS; no usar `*`. No agregar credenciales de administrador a Angular.

Ejemplo en el servidor Nginx de la web, ajustando la dirección privada de Red Clubes:

```nginx
location = /api/public/instituciones/bomberos-ensenada/solicitudes {
    client_max_body_size 8k;
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Forwarded "";
}
```

Mantener MySQL y backend sin puertos públicos. Si web y Red Clubes están en servidores distintos, usar HTTPS o un enlace privado protegido entre proxies. El proxy público conserva sus propios límites y headers. Verificar el peer observado en TRUSTED_PROXY_CIDR.

Nginx limita tamaño y frecuencia; backend limita 10 envíos válidos por IP cada 15 minutos, en una instancia, y limita memoria a 10.000 IP. Un campo señuelo reduce envíos automáticos simples. Estos límites no sustituyen monitoreo; CAPTCHA y limitación distribuida se pueden agregar si el uso lo requiere. CORS restringe navegadores, no autentica a terceros.

El texto de privacidad inicial describe el circuito. Antes de publicar, Bomberos debe confirmar sus canales de contacto, política de conservación y responsables. No hay notificaciones por email ni eliminación automática: la revisión es desde la bandeja y la conservación debe definirse antes de recibir datos reales.

## Reconciliación de la rama local

GitHub tiene `V7__tipo_institucion_club.sql`. La rama local anterior usaba V7 para grupos. La integración conserva la V7 oficial y mueve **sin modificar el SQL** de grupos a V8. V9 incorpora solicitudes.

Una base vacía o basada en main migra normalmente. Una base con V7 local requiere este procedimiento, primero sobre una copia restaurada:

1. Generar un backup y comprobar su restauración. Registrar checksum y conteos.
2. Detener el backend. No borrar el volumen.
3. Ejecutar `ops/reconcile-local-v7.ps1` sin `-Apply` para verificar el caso exacto.
4. Ejecutar con `-Apply -BackupPath <backup.sql.gz>` únicamente si coincide. El script exige el nombre y checksum conocidos; no ejecuta Flyway repair, no cambia checksums y no toca datos de socios.
5. Arrancar la nueva versión una vez con `FLYWAY_OUT_OF_ORDER=true`, para aplicar la V7 oficial que falta y V9.
6. Comprobar salud y migraciones 1 a 9 exitosas, volver a `FLYWAY_OUT_OF_ORDER=false` y recrear backend.
7. Verificar conteos, usuarios, configuración de módulos y acceso por institución.

No se puede volver al backend anterior sin plan de rollback: su historial V7 no coincide con el integrado. Conservar el backup y el commit previo para restaurar juntos código y base si fuera necesario.

## Provincia NET: futura API de integración

La integración de pagos es distinta del endpoint público de solicitudes. No se concede acceso SQL ni se reutilizan sesiones de administradores.

Diseño previsto, pendiente de contrato técnico real:

- Credencial por integrador y ambiente, revocable, con vencimiento, rotación y almacenamiento del hash; transporte HTTPS.
- Alcance explícito por institución y operación: consultar socio por referencia externa y consultar cuotas/saldos. Sin listado global ni datos de otras instituciones.
- Entregar solo datos necesarios para identificar y cobrar; excluir contactos de emergencia, hashes, usuarios y auditoría interna.
- Usar saldo pendiente real, considerando pagos parciales, anulaciones y vencimientos.
- Registrar identificador de cliente, recurso, fecha y resultado, sin registrar tokens ni cuerpos con datos personales.
- Si se reciben pagos: firma del proveedor, validación de importe y moneda, identificador externo único, transacción y conciliación; aceptar reintentos sin duplicar pagos.
- La vuelta del navegador desde el proveedor nunca basta para marcar una cuota como pagada.
- Definir paginación, límites, pruebas de contrato, ambiente de pruebas y versionado `/api/integraciones/v1`.

La preparación de Fase 2 ya cuenta con cliente de staging, intenciones, recepción de avisos y conciliación simulada, deshabilitados por defecto. Ver [NET Pagos: preparación y pendientes del contrato](NETPAGOS_FASE2_PREPARACION.md). No se habilita producción ni se supone un mecanismo de autenticación de webhook que el proveedor no haya confirmado.

## Verificación local del 23 de octubre de 2026

- Backend: 79 pruebas aprobadas, incluyendo dos envíos simultáneos, dos aprobaciones simultáneas, rechazo, aislamiento entre instituciones, consentimiento, honeypot y rate limit.
- Frontend: 29 pruebas aprobadas, incluyendo descarte de respuestas al cambiar de institución y prevención de doble aprobación.
- Angular actualizado a 21.2.25; auditorías npm de producción sin vulnerabilidades en ambos proyectos. Se retiró la excepción anterior del auditor.
- Web pública: compilación correcta, validación visible de formulario vacío y prueba visual del envío completo.
- Backup local restaurado en MySQL 8.4 aislado; reconciliación V7/V8, migración V9 y validación Hibernate exitosas.
- Prueba visual en la copia aislada: envío desde la web → pendiente en Red Clubes → aprobación → un socio activo, sin cuotas. Persistencia verificada tras reiniciar el backend de prueba.
- Instalación local actualizada; Flyway out-of-order deshabilitado al finalizar y datos ficticios restringidos a los contenedores temporales.
- Backups privados en `backups/pre-solicitudes-20261023.sql.gz` y `backups/pre-solicitudes-20261023-final.sql.gz`, excluidos de Git. Conservar junto al commit de respaldo `52179f6`.

La publicación externa, los dominios HTTPS, la política final de conservación/contacto y el contrato de Provincia NET siguen pendientes. La advertencia de tamaño inicial del frontend sigue presente (aproximadamente 619 kB frente a 500 kB de advertencia; por debajo del límite de error de 1 MB).
