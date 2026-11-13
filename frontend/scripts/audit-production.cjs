const { spawnSync } = require('node:child_process');

// Sin excepciones: los hallazgos HIGH/CRITICAL y los errores de auditoría bloquean CI.
const result = spawnSync('npm', ['audit', '--omit=dev', '--audit-level=high'], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
});
if (result.error) console.error('No se pudo ejecutar la auditoría de producción.');
process.exit(result.status ?? 1);