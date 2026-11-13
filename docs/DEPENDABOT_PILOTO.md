# Dependabot — revisión del 2026-09-16

PRs abiertos consultados en GitHub; ninguno modificado ni mergeado. SAFE significa candidato a evaluación con CI, no aprobación automática. No actualizar runtimes durante preparación del piloto salvo vulnerabilidad bloqueante demostrada.

| PR | Cambio | Clasificación | Acción propuesta |
|---|---|---|---|
| [#4](https://github.com/nachoechave/red-clubes/pull/4) | Temurin JRE 21 → 25 | MAJOR / requiere migración | Mantener Java 21; evaluar build y runtime juntos después del piloto. |
| [#5](https://github.com/nachoechave/red-clubes/pull/5) | setup-java 5 → 6 | MAJOR / requiere migración | Revisar release notes y runtime del runner; posponer, no cambia por sí solo la versión Java configurada. |
| [#6](https://github.com/nachoechave/red-clubes/pull/6) | Boot 3.5.16 → 4.1.1 | MAJOR / requiere migración | Tarea específica de Spring/Security/Jakarta y regresiones. |
| [#7](https://github.com/nachoechave/red-clubes/pull/7) | Maven image 3.9.11/Java 21 → 3.9.15/Java 26 | MAJOR / requiere migración | No aceptar mezclado: conservar Java 21 aunque Maven sea actualización menor. |
| [#8](https://github.com/nachoechave/red-clubes/pull/8) | Angular router 21 → 22 | MAJOR / requiere migración | Evaluar toda la familia Angular coordinada, no paquetes aislados. |
| [#9](https://github.com/nachoechave/red-clubes/pull/9) | jsdom 28 → 30 | MAJOR / requiere migración | Revisar compatibilidad Node/Vitest; posponer hasta después del piloto. |
| [#10](https://github.com/nachoechave/red-clubes/pull/10) | Maven wrapper 3.9.15 → 3.9.16 | SAFE / evaluar pronto | Patch; revisar diff del wrapper, checksum/distribución y ejecutar CI completo. No merge en esta tarea. |
| [#11](https://github.com/nachoechave/red-clubes/pull/11) | Angular build 21 → 22 | MAJOR / requiere migración | Coordinar con #8/#12 y compatibilidad TypeScript/Node. |
| [#12](https://github.com/nachoechave/red-clubes/pull/12) | Angular platform-browser 21 → 22 | MAJOR / requiere migración | Misma migración Angular; mantener 21. |
| [#13](https://github.com/nachoechave/red-clubes/pull/13) | TypeScript 5.9 → 7.0 | MAJOR / requiere migración | Mantener 5.9; requiere compatibilidad del compilador Angular. |
| [#14](https://github.com/nachoechave/red-clubes/pull/14) | Node 22 → 26 | MAJOR / requiere migración | Mantener 22 en Docker, .nvmrc, engines y CI. |
| [#15](https://github.com/nachoechave/red-clubes/pull/15) | Nginx 1.29 → 1.31 Alpine | POSPONER HASTA DESPUÉS DEL PILOTO | Cambia línea e imagen base; revisar release notes, CSP/headers/IP real, health y Trivy antes de aceptarlo. |

Volver a consultar PRs/alertas antes del release: este inventario es una foto fechada. Un scanner verde no sustituye pruebas de compatibilidad.
