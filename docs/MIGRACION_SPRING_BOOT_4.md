# Migración a Spring Boot 4

Rama `migration/spring-boot-4`, creada desde `deploy/donweb` (`bd9335e`).
Motivo: corregir CVE-2026-47884 y CVE-2026-47890 sin depender de parches Enterprise.
El [informe del PR #29](PR29_TRIVY_SPRING.md) conserva la evidencia de la imagen anterior.

## Dependencias y compatibilidad

- Spring Boot **4.0.9** estable, publicado en Maven Central, con Spring Framework
  **7.0.10**. Java permanece en **21** en el POM, la imagen y CI. El
  [BOM publicado](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.9/spring-boot-dependencies-4.0.9.pom)
  gestiona también Security 7.0.8, Tomcat 11.0.27, Hibernate 7.2.25.Final,
  Flyway 11.14.1 y Connector/J 9.7.0. Se retiran los overrides antiguos de Tomcat
  10.1 y Jackson; no se mezclan versiones de Spring Framework.
- Se usan los starters modulares `webmvc`, `flyway`, `webmvc-test` y
  `security-test`. El driver MySQL y `flyway-mysql` se conservan. Las 11
  migraciones SQL no se modifican. No se utiliza `repair` ni se cambian opciones
  de baseline, validación de esquema o migración fuera de orden.
- El mapper de la aplicación y de MVC pasa a Jackson 3 (`tools.jackson`).
  `spring.jackson.use-jackson2-defaults=true` conserva el comportamiento JSON de
  Boot 3; se mantienen los nombres de campos, las fechas ISO y los importes como
  números. Las anotaciones `JsonProperty` y `JsonAnySetter` siguen en su paquete
  original. Flyway 11 trae Jackson 2 transitivamente; el JAR incluye ambas
  generaciones, pero los controladores y servicios usan el mapper de Jackson 3.
- Se actualiza el nombre de `UserDetailsServiceAutoConfiguration` para conservar
  la exclusión preexistente de usuarios por defecto. Las cadenas de filtros,
  los permisos, CORS y la autenticación con bearer no cambian. El límite del
  webhook sigue devolviendo 413 (la constante ahora es `CONTENT_TOO_LARGE`).
- NET Pagos permanece `OFF` por defecto. Webhook, acreditación y reconciliación
  conservan el valor `false`. El ensayo usa el simulador con datos ficticios,
  sin llamadas a NET ni cobros reales.

Fuentes: [guía oficial de migración](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide),
[requisitos de Boot 4.0](https://docs.spring.io/spring-boot/4.0/system-requirements.html),
[CVE-2026-47884](https://spring.io/security/cve-2026-47884/) y
[CVE-2026-47890](https://spring.io/security/cve-2026-47890/).

## Reproducción

1. Java 21: `cd backend && ./mvnw --batch-mode --no-transfer-progress clean verify`.
2. Desde la raíz, construir y analizar la imagen con Trivy 0.70.0 (la versión
   usada por el pipeline al verificar el PR #29):

   ```sh
   docker build --pull -t red-clubes-backend:boot4-migration backend
   trivy image --format table --exit-code 1 --ignore-unfixed=false \
     --pkg-types os,library --severity HIGH,CRITICAL --scanners vuln,secret \
     red-clubes-backend:boot4-migration
   ```

3. Usar los Compose descartables de `ops/compose.portal-test.yml` y
   `ops/compose.netpagos-test.yml`, con nombre de proyecto único y un overlay
   que seleccione la imagen recién construida. Esperar `/api/health`, ejecutar
   `powershell -NoProfile -File ops/test-portal-mysql.ps1 -NetPagos` y reiniciar
   solo el backend de ensayo para verificar Flyway sobre el mismo esquema.
   Retirar ese proyecto y su volumen de prueba al terminar.

## Validaciones

- Backend: 142 pruebas, cero fallos, errores u omitidas. Las 141 originales se
  mantienen y se añade una prueba que confirma que no se crea un usuario por
  defecto. Se refuerzan las aserciones del JSON de errores, fechas de sesión y
  vencimientos de cuotas.
- Angular: 29 pruebas y compilación correctas, sin cambios al cliente. Continúa
  el aviso previo de tamaño inicial (618,85 kB frente al aviso de 500 kB).
- MySQL 8.4 aislado: Flyway validó y aplicó V1–V11, Hibernate validó el esquema
  y el backend inició con Tomcat 11. Pasaron el ensayo de OTP, cuotas, aislamiento,
  pagos parciales y NET simulado. Tras reiniciar, Flyway volvió a validar las 11
  migraciones y no aplicó ninguna de nuevo. Se retiró el entorno descartable.
- Imagen Linux reconstruida con el Dockerfile existente. La comprobación local
  de Trivy usa `docker save` y `image --input` con los mismos filtros y
  escáneres del job de imagen: **cero hallazgos altos o críticos** en Alpine
  3.24.4 y `app/app.jar`. No aparecen los dos CVE que bloqueaban el PR #29.
  No cambia el workflow ni se agregan exclusiones.

El ensayo con MySQL utiliza un esquema descartable. Antes de un despliegue,
repetir la validación con una restauración aislada y autorizada si se necesita
comprobar datos históricos. No se cambia infraestructura de Donweb, Comercio
Flex, credenciales ni bases de datos reales.
