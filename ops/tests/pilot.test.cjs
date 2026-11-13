// Linux: node --test ops/tests/pilot.test.cjs (bash, jq, curl, gzip, sha256sum).
// Solo fixtures temporales: nunca usa Docker real ni credenciales del operador.
const { test, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { gzipSync, gunzipSync } = require('node:zlib');
const roots = [];
function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'redclubes-ops-'));
  roots.push(root);
  fs.cpSync(path.join(__dirname, '..'), path.join(root, 'ops'), { recursive: true });
  fs.mkdirSync(path.join(root, 'bin'));
  const env = { ...process.env, PATH: `${root}/bin:${process.env.PATH}`, BACKUP_DIR: `${root}/backups`, BACKUP_RETENTION_DAYS: '14', BACKUP_EXPORT_HOOK: '', CALLS: `${root}/calls` };
  const stub = (name, body) => fs.writeFileSync(`${root}/bin/${name}`, `#!/bin/sh\nset -eu\n${body}\n`, { mode: 0o700 });
  const run = (file, args = [], vars = {}) => spawnSync('bash', [`${root}/ops/${file}`, ...args], { env: { ...env, ...vars }, cwd: root, encoding: 'utf8' });
  return { root, env, stub, run };
}
afterEach(() => { for (const root of roots.splice(0)) fs.rmSync(root, { recursive: true, force: true }); });
test('all shell scripts parse', () => {
  for (const name of fs.readdirSync(path.join(__dirname, '..')).filter(n => n.endsWith('.sh'))) {
    const result = spawnSync(name === 'deploy.sh' ? 'bash' : 'sh', ['-n', path.join(__dirname, '..', name)], { encoding: 'utf8' });
    assert.equal(result.status, 0, `${name}: ${result.stderr}`);
  }
});
test('backup publishes valid gzip/checksum only after export success', () => {
  const f = fixture();
  f.stub('docker', `printf 'CREATE TABLE fixture (id int);\\n'`);
  const result = f.run('backup-mysql.sh');
  assert.equal(result.status, 0, result.stderr);
  const name = fs.readFileSync(`${f.env.BACKUP_DIR}/.last-success`, 'utf8').trim();
  assert.match(gunzipSync(fs.readFileSync(`${f.env.BACKUP_DIR}/${name}`)).toString(), /CREATE TABLE/);
  assert.equal(spawnSync('sha256sum', ['-c', `${name}.sha256`], { cwd: f.env.BACKUP_DIR }).status, 0);
  assert.equal(fs.statSync(`${f.env.BACKUP_DIR}/${name}`).mode & 0o777, 0o600);
});
test('failed export preserves prior success marker and local backup', () => {
  const f = fixture();
  f.stub('docker', `printf 'CREATE TABLE fixture (id int);\\n'`);
  fs.mkdirSync(f.env.BACKUP_DIR);
  fs.writeFileSync(`${f.env.BACKUP_DIR}/.last-success`, 'prior-backup\n');
  fs.writeFileSync(`${f.root}/fail-hook.sh`, 'exit 7\n');
  const result = f.run('backup-mysql.sh', [], { BACKUP_EXPORT_HOOK: `${f.root}/fail-hook.sh` });
  assert.notEqual(result.status, 0);
  assert.equal(fs.readFileSync(`${f.env.BACKUP_DIR}/.last-success`, 'utf8'), 'prior-backup\n');
  assert.equal(fs.readdirSync(f.env.BACKUP_DIR).filter(n => n.endsWith('.sql.gz')).length, 1);
});
test('empty dump never publishes a success marker', () => {
  const f = fixture(); f.stub('docker', 'exit 0');
  assert.notEqual(f.run('backup-mysql.sh').status, 0);
  assert.equal(fs.existsSync(`${f.env.BACKUP_DIR}/.last-success`), false);
});
test('restore rejects missing checksum before invoking Docker', () => {
  const f = fixture(); f.stub('docker', 'echo invoked >> "$CALLS"');
  const dump = `${f.root}/fixture.sql.gz`;
  fs.writeFileSync(dump, gzipSync('CREATE TABLE fixture (id int);\n'));
  const result = f.run('restore-mysql.sh', [dump], { CONFIRM_RESTORE: 'RESTAURAR', ALLOW_PRODUCTION_RESTORE: 'true' });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /checksum/);
  assert.equal(fs.existsSync(f.env.CALLS), false);
});
test('restore requires both explicit confirmations', () => {
  const f = fixture(); f.stub('docker', 'echo invoked >> "$CALLS"');
  assert.equal(f.run('restore-mysql.sh', ['missing.gz'], { CONFIRM_RESTORE: '', ALLOW_PRODUCTION_RESTORE: '' }).status, 2);
  assert.equal(fs.existsSync(f.env.CALLS), false);
});
test('encrypted export refuses an unmounted destination', () => {
  const f = fixture(); f.stub('age', 'echo invoked >> "$CALLS"'); f.stub('mountpoint', 'exit 1');
  const result = f.run('backup-export-age.sh', ['missing.sql.gz'], { BACKUP_AGE_RECIPIENT: 'fixture-public-recipient', BACKUP_COPY_DIR: `${f.root}/offsite` });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /sin montar/);
  assert.equal(fs.existsSync(f.env.CALLS), false);
});
test('HTTPS enforcement rejects HTTP before any request', () => {
  const f = fixture(); f.stub('curl', 'echo invoked >> "$CALLS"');
  assert.equal(f.run('smoke-test.sh', [], { BASE_URL: 'http://127.0.0.1', REQUIRE_HTTPS: 'true' }).status, 2);
  assert.equal(fs.existsSync(f.env.CALLS), false);
});
test('real age encryption, copy and decryption preserve the dump', () => {
  const f = fixture(); f.stub('docker', `printf 'CREATE TABLE fixture (id int);\\n'`);
  // Solo simula el montaje remoto; el cifrado y checksum son reales.
  f.stub('mountpoint', 'exit 0');
  const identity = `${f.root}/test-only-identity.txt`;
  assert.equal(spawnSync('age-keygen', ['-o', identity]).status, 0);
  const recipient = spawnSync('age-keygen', ['-y', identity], { encoding: 'utf8' }).stdout.trim();
  const offsite = `${f.root}/offsite`; fs.mkdirSync(offsite);
  const result = f.run('backup-mysql.sh', [], { BACKUP_EXPORT_HOOK: `${f.root}/ops/backup-export-age.sh`, BACKUP_COPY_DIR: offsite, BACKUP_AGE_RECIPIENT: recipient });
  assert.equal(result.status, 0, result.stderr);
  const name = fs.readFileSync(`${f.env.BACKUP_DIR}/.last-success`, 'utf8').trim();
  assert.equal(fs.existsSync(`${offsite}/${name}`), false, 'No plaintext fuera del servidor');
  assert.equal(spawnSync('sha256sum', ['-c', `${name}.age.sha256`], { cwd: offsite }).status, 0);
  const decrypted = spawnSync('age', ['-d', '-i', identity, `${offsite}/${name}.age`]);
  assert.equal(decrypted.status, 0);
  assert.deepEqual(decrypted.stdout, fs.readFileSync(`${f.env.BACKUP_DIR}/${name}`));
});
function deployment() {
  const f = fixture();
  fs.writeFileSync(`${f.root}/.env`, 'IMAGE_TAG=v0.1.0-pilot\n', { mode: 0o600 });
  f.stub('git', `case "$1" in status) ;; fetch) ;; rev-parse) echo fixture-revision;; *) exit 99;; esac`);
  const config = { services: {
    backend: { image: 'fixture-backend:v0.1.0-pilot', environment: { SPRING_PROFILES_ACTIVE: 'prod', FLYWAY_ENABLED: 'true', FLYWAY_BASELINE_ON_MIGRATE: 'false', DEMO_DATA_ENABLED: 'false', BOOTSTRAP_SUPERUSER_ENABLED: 'false', CORS_ALLOWED_ORIGINS: 'https://pilot.test.invalid' } },
    mysql: { environment: { MYSQL_PASSWORD: 'a'.repeat(33), MYSQL_ROOT_PASSWORD: 'b'.repeat(33) } },
    frontend: { image: 'fixture-frontend:v0.1.0-pilot', ports: [{ host_ip: '127.0.0.1', published: '8080' }] }
  } };
  fs.writeFileSync(`${f.root}/config.json`, JSON.stringify(config));
  f.stub('docker', `echo "$*" >> "$CALLS"
case "$*" in
  'compose config --format json') cat config.json;;
  'compose ps --all --quiet backend') ;;
  'image inspect '*) exit 0;;
  'compose up '*) exit 17;;
  *) exit 99;;
esac`);
  return { ...f, config };
}
test('deploy rejects latest without invoking Docker', () => {
  const f = fixture(); f.stub('docker', 'echo invoked >> "$CALLS"');
  assert.notEqual(f.run('deploy.sh', ['latest', 'build']).status, 0);
  assert.equal(fs.existsSync(f.env.CALLS), false);
});
test('deploy refuses exposed database ports', () => {
  const f = deployment(); f.config.services.mysql.ports = [{ published: '3306' }];
  fs.writeFileSync(`${f.root}/config.json`, JSON.stringify(f.config));
  const result = f.run('deploy.sh', ['v0.1.0-pilot', 'existing']);
  assert.notEqual(result.status, 0); assert.match(result.stderr, /puertos/);
  assert.doesNotMatch(fs.readFileSync(f.env.CALLS, 'utf8'), /compose up/);
});
test('deploy refuses to overwrite existing image tags', () => {
  const f = deployment(); const result = f.run('deploy.sh', ['v0.1.0-pilot', 'build']);
  assert.notEqual(result.status, 0); assert.match(result.stderr, /Tag local ya existe/);
  assert.doesNotMatch(fs.readFileSync(f.env.CALLS, 'utf8'), /compose build|compose up/);
});
test('failed health returns error without down/rm/automatic rollback', () => {
  const f = deployment(); const result = f.run('deploy.sh', ['v0.1.0-pilot', 'existing']);
  assert.notEqual(result.status, 0); assert.match(result.stderr, /Health fallo/);
  const calls = fs.readFileSync(f.env.CALLS, 'utf8');
  assert.match(calls, /compose up/); assert.doesNotMatch(calls, /compose down| rm|prune/);
});
