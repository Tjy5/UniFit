#!/usr/bin/env node

const { spawnSync } = require('node:child_process');
const { existsSync } = require('node:fs');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '..');
const localRunner = path.join(repoRoot, 'node_modules', '@playwright', 'test', 'cli.js');

if (!existsSync(localRunner)) {
  console.error('Cannot find the repository-local @playwright/test runner.');
  console.error('Run `npm install` from the repository root, then retry this command.');
  process.exit(1);
}

const result = spawnSync(process.execPath, [localRunner, 'test', ...process.argv.slice(2)], {
  cwd: repoRoot,
  stdio: 'inherit',
  shell: false,
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);
