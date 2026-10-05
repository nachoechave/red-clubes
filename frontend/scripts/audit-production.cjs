const { spawnSync } = require('node:child_process');

const ALLOWED_ADVISORIES = new Set([
  // Red Clubes is a client-side Angular SPA served by nginx and does not use Angular SSR.
  // This advisory affects Angular Server-Side Rendering through numeric URL matrix parameters.
  'https://github.com/advisories/GHSA-ff3f-86qr-9cv3',
]);

const result = spawnSync('npm', ['audit', '--omit=dev', '--json'], {
  encoding: 'utf8',
  shell: process.platform === 'win32',
});

let report;
try {
  report = JSON.parse(result.stdout || '{}');
} catch (error) {
  console.error('No se pudo interpretar la salida de npm audit.');
  if (result.stderr) console.error(result.stderr);
  process.exit(1);
}

const blocked = [];
for (const [name, vulnerability] of Object.entries(report.vulnerabilities || {})) {
  if (!['high', 'critical'].includes(vulnerability.severity)) continue;

  const advisories = (vulnerability.via || [])
    .filter((item) => typeof item === 'object' && item !== null)
    .map((item) => item.url)
    .filter(Boolean);

  const onlyAllowed = advisories.length > 0 && advisories.every((url) => ALLOWED_ADVISORIES.has(url));
  if (!onlyAllowed) {
    blocked.push({
      name,
      severity: vulnerability.severity,
      advisories,
      range: vulnerability.range,
    });
  }
}

if (blocked.length > 0) {
  console.error('Se encontraron vulnerabilidades HIGH/CRITICAL de producción no exceptuadas:');
  for (const item of blocked) {
    console.error(`- ${item.name} (${item.severity}) ${item.range || ''}`);
    for (const url of item.advisories) console.error(`  ${url}`);
  }
  process.exit(1);
}

console.log('Auditoría de dependencias de producción aprobada.');
if ((report.metadata?.vulnerabilities?.high || 0) > 0) {
  console.log('La excepción SSR documentada permanece presente y debe retirarse al actualizar Angular 21 LTS.');
}
