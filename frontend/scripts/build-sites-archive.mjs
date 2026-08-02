import { createHash } from 'node:crypto';
import { mkdir, readdir, readFile, rm, writeFile, cp } from 'node:fs/promises';
import { extname, join, relative } from 'node:path';
import { spawnSync } from 'node:child_process';

const root = new URL('..', import.meta.url).pathname;
const browserDir = join(root, 'dist', 'frontend', 'browser');
const outputDir = join('/tmp', 'assinador-sites-build');
const archivePath = join('/tmp', 'assinador-sites-build.tar.gz');

const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.ico', 'image/x-icon'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.map', 'application/json; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml; charset=utf-8'],
  ['.webp', 'image/webp'],
]);

async function collectFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    if (entry.name === '.DS_Store') {
      continue;
    }

    const absolutePath = join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectFiles(absolutePath)));
    } else if (entry.isFile()) {
      files.push(absolutePath);
    }
  }

  return files;
}

const files = await collectFiles(browserDir);
const assets = {};

for (const file of files) {
  const route = `/${relative(browserDir, file).replaceAll('\\', '/')}`;
  const body = await readFile(file);
  assets[route] = {
    body: body.toString('base64'),
    contentType: contentTypes.get(extname(file)) ?? 'application/octet-stream',
    etag: createHash('sha256').update(body).digest('hex'),
  };
}

await rm(outputDir, { recursive: true, force: true });
await mkdir(join(outputDir, 'dist', 'server'), { recursive: true });
await mkdir(join(outputDir, '.openai'), { recursive: true });
await cp(join(root, '.openai', 'hosting.json'), join(outputDir, '.openai', 'hosting.json'));

await writeFile(
  join(outputDir, 'dist', 'server', 'index.js'),
  `const assets = ${JSON.stringify(assets)};\n\n` +
    `function serveAsset(pathname, request) {\n` +
    `  const asset = assets[pathname] ?? assets['/index.html'];\n` +
    `  const headers = new Headers({\n` +
    `    'content-type': asset.contentType,\n` +
    `    etag: '"' + asset.etag + '"',\n` +
    `    'cache-control': pathname === '/index.html' ? 'no-cache' : 'public, max-age=31536000, immutable',\n` +
    `  });\n\n` +
    `  if (request.headers.get('if-none-match') === '"' + asset.etag + '"') {\n` +
    `    return new Response(null, { status: 304, headers });\n` +
    `  }\n\n` +
    `  return new Response(Uint8Array.from(atob(asset.body), (char) => char.charCodeAt(0)), { headers });\n` +
    `}\n\n` +
    `export default {\n` +
    `  async fetch(request) {\n` +
    `    const url = new URL(request.url);\n` +
    `    const pathname = url.pathname === '/' ? '/index.html' : url.pathname;\n` +
    `    return serveAsset(pathname, request);\n` +
    `  },\n` +
    `};\n`,
);

const tar = spawnSync('tar', ['-czf', archivePath, '-C', outputDir, '.'], {
  encoding: 'utf8',
});

if (tar.status !== 0) {
  throw new Error(tar.stderr || 'Failed to create Sites archive');
}

console.log(archivePath);
