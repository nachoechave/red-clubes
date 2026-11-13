# PR #29: bloqueo de Trivy por Spring MVC

Revisión del 13 de noviembre de 2026, rama `deploy/donweb`, commit de aplicación
`b02f8da`. El [CI del PR](https://github.com/nachoechave/red-clubes/actions/runs/43480322741)
reportó dos hallazgos `CRITICAL` en `spring-webmvc:6.2.19` dentro de `app.jar`.
El escaneo de imagen terminó con código 1 y los pasos posteriores de `containers`
no se ejecutaron. `backend-dependencies` aparece en verde, pero su log informa
`Number of language-specific files num=0`; ese resultado no contradice el análisis
del JAR dentro de la imagen.

| Aviso de Spring | Condición para que se manifieste | Resultado de la revisión |
| --- | --- | --- |
| [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) (media según Spring) | Uso de `XsltView` con un mapeo `/**` que renderiza una vista sin nombre explícito. | No se encontró `XsltView`, configuración de resolución de vistas XSLT ni plantillas XSLT. |
| [CVE-2026-47890](https://spring.io/security/cve-2026-47890/) (baja según Spring) | Streaming SSE de fragmentos de vistas con datos que un tercero puede controlar y que se envían a otros usuarios. | No se encontró SSE ni renderizado de fragmentos: sin `SseEmitter`, `ServerSentEvent`, `text/event-stream`, `FragmentsRendering` ni `EventSource`. |

Se revisaron fuentes, recursos, configuración y dependencias empaquetadas. Los
controladores de aplicación son REST, la web Angular se sirve por separado y no se
encontraron motores de plantillas de servidor en el JAR. La ausencia de esos usos
reduce la exposición observada; la dependencia sigue afectada y no se rebaja la
severidad usada por el CI.

## Opciones de actualización

- **Spring Boot 3.5 con soporte comercial:** Spring Framework `6.2.20` corrige ambos
  CVE. [Boot 3.5.17 Enterprise](https://enterprise.spring.io/projects/spring-boot/3.5.17/)
  ya gestiona esa versión. Si la organización tiene acceso comercial autorizado,
  elegir un parche mantenido de Boot 3.5 que incluya Framework 6.2.20 o superior,
  verificar también los parches de Tomcat y Jackson que hoy se fijan en el POM y
  repetir los escaneos. El CI registró `7.0.9` como versión corregida; no se pudo
  probar si ese feed reconoce el parche Enterprise al analizar su JAR. No se
  configuraron repositorios privados ni se agregaron credenciales.
- **Spring Boot 3.5 OSS:** [3.5.16 es la última publicación abierta de la serie](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).
  [Maven Central, Framework](https://repo.maven.apache.org/maven2/org/springframework/spring-webmvc/maven-metadata.xml)
  termina en `6.2.19` para 6.2.x y [Maven Central, Boot](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)
  en `3.5.16` para 3.5.x; la consulta directa de `6.2.20` y `3.5.17` devolvió 404.
  No hay actualización pública compatible dentro de esa línea.
- **Migración OSS a Boot 4:** ambos avisos identifican Framework `7.0.9` como
  primera versión OSS corregida. Como referencia, el [BOM de Boot 4.0.9](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.9/spring-boot-dependencies-4.0.9.pom)
  publicado en Central gestiona Framework `7.0.10`. No insertar Framework 7 en Boot
  3.5 ni actualizar solo `spring-webmvc`: son generaciones distintas. Evaluar una
  versión mantenida de Boot 4 en un PR separado. La [guía de migración](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
  documenta cambios en Jackson, Servlet/Tomcat, starters y configuración. Validar
  contratos JSON, autenticación y autorización, proxy, Flyway, MySQL, backup/restore
  y ambos escaneos de imagen antes de proponer un despliegue.

## Verificación local

- Java 21, Maven `clean verify`: 141 pruebas, cero fallos, errores u omitidas.
  Flyway validó y aplicó V1–V11 en H2 en modo MySQL.
- Imagen reconstruida desde `backend/Dockerfile` con `--pull --no-cache`.
  El JAR mantiene Framework `6.2.19` y Tomcat embebido `10.1.60`.
- MySQL 8.4 aislado y Mailpit: Flyway validó y aplicó V1–V11 en una base vacía;
  Hibernate validó el esquema. Pasaron el ensayo de OTP, cuotas, pagos parciales,
  aislamiento y NET Pagos simulado. Se retiró el proyecto Docker de prueba.

Para reproducir el análisis de imagen con los mismos criterios de CI:

```sh
docker build --pull --no-cache -t red-clubes-backend:pr29-spring-audit backend
trivy image --format table --exit-code 1 --ignore-unfixed=false \
  --pkg-types os,library --severity HIGH,CRITICAL --scanners vuln,secret \
  red-clubes-backend:pr29-spring-audit
```

Usar Trivy `0.70.0` (versión instalada por `trivy-action@v0.36.0` en esa corrida).
La comprobación local usa la misma imagen vía `docker save` y `trivy image --input`,
con la base de vulnerabilidades disponible al momento de la prueba.

**Resultado:** código 1. Alpine 3.24.4 mostró cero vulnerabilidades altas o
críticas; `app/app.jar` volvió a mostrar exactamente los dos CVE críticos en
`spring-webmvc:6.2.19` con `7.0.9` como corrección según Trivy. No se registró un
pipeline verde ni se ejecutaron localmente todos los pasos del CI.

La decisión para `deploy/donweb` es mantener los controles y el bloqueo hasta
disponer de una actualización compatible verificable o de una migración probada.
No se modifican POM, Dockerfile, workflow, migraciones, exclusiones ni credenciales.
