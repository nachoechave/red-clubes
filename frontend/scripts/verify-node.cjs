const [major] = process.versions.node.split('.').map(Number);

if (major !== 22) {
  console.error(`Red Clubes requiere Node 22 para compilar. Version actual: ${process.versions.node}`);
  console.error('Ejecuta "nvm use" dentro de frontend antes de npm run build.');
  process.exit(1);
}
