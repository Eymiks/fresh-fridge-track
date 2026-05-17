import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const SCREENSHOTS_DIR = path.join('test-results', 'screenshots');
const VIEWPORT = { width: 390, height: 844 };

test.beforeAll(() => {
  fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
});

test.use({ viewport: VIEWPORT });

test('pwa-auth', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'pwa-auth.png'), fullPage: true });
});

test('pwa-home-guest', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  // Try to continue as guest
  const guestBtn = page.getByRole('button', { name: /invité|guest|continuer/i });
  if (await guestBtn.isVisible()) {
    await guestBtn.click();
    await page.waitForLoadState('networkidle');
  }
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'pwa-home.png'), fullPage: true });
});

test('pwa-stats-guest', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  const guestBtn = page.getByRole('button', { name: /invité|guest|continuer/i });
  if (await guestBtn.isVisible()) {
    await guestBtn.click();
    await page.waitForLoadState('networkidle');
  }
  await page.goto('/stats');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'pwa-stats.png'), fullPage: true });
});

test('pwa-history-guest', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  const guestBtn = page.getByRole('button', { name: /invité|guest|continuer/i });
  if (await guestBtn.isVisible()) {
    await guestBtn.click();
    await page.waitForLoadState('networkidle');
  }
  await page.goto('/history');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'pwa-history.png'), fullPage: true });
});

test('pwa-notifications-guest', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  const guestBtn = page.getByRole('button', { name: /invité|guest|continuer/i });
  if (await guestBtn.isVisible()) {
    await guestBtn.click();
    await page.waitForLoadState('networkidle');
  }
  await page.goto('/notifications');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'pwa-notifications.png'), fullPage: true });
});

test('pwa-settings-guest', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  const guestBtn = page.getByRole('button', { name: /invité|guest|continuer/i });
  if (await guestBtn.isVisible()) {
    await guestBtn.click();
    await page.waitForLoadState('networkidle');
  }
  await page.goto('/settings');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, 'pwa-settings.png'), fullPage: true });
});
