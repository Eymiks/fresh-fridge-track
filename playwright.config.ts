import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  outputDir: 'test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,

  use: {
    baseURL: 'https://localhost:3000',
    ignoreHTTPSErrors: true, // mkcert certs non reconnus par Chromium headless
    screenshot: 'only-on-failure',
    video: 'off',
    headless: process.env.PWHEADLESS !== 'false',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: {
    command: 'bun run dev -- --port 3000 --host',
    url: 'https://localhost:3000',
    reuseExistingServer: true,
    ignoreHTTPSErrors: true,
    timeout: 30_000,
  },
});
