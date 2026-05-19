const path = require('path');
const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests/smoke',
  timeout: 45_000,
  expect: {
    timeout: 8_000,
  },
  fullyParallel: false,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
  ],
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  webServer: [
    {
      command: 'npx vite --host 127.0.0.1 --port 3000',
      url: 'http://127.0.0.1:3000',
      cwd: path.join(__dirname, 'School-uniform-intelligent-ordering-system-usersystem', 'user-vue'),
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: 'npx vite --host 127.0.0.1 --port 5173',
      url: 'http://127.0.0.1:5173',
      cwd: path.join(__dirname, 'School-uniform-intelligent-ordering-system-managementor', 'admin-web'),
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
