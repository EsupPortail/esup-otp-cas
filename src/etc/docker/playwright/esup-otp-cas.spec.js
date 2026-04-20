const { test, expect } = require('@playwright/test');

const USERNAME = 'joe';
const PASSWORD = 'pass';

async function loginWithCas(page) {
  await page.goto('/');
  await page.locator('#login').click();
  await expect(page.locator('#username')).toBeVisible();
  await page.locator('#username').fill(USERNAME);
  await page.locator('#password').fill(PASSWORD);
  await page.locator('[name="submitBtn"]').click();
}

test.describe.configure({ mode: 'serial' });

test('Login to CAS, activate backup codes, logout, login again with backup code, desactivate backup codes', async ({ page }) => {
  // important to accept the alerts triggered by the backup code activation/deactivation
  page.on('dialog', dialog => dialog.accept());

  await loginWithCas(page);

  await page.locator('#bypass').click();
  await page.locator('.switch .lever').click();
  await page.waitForLoadState('networkidle').catch(() => {});
  const backupCode = await  page.locator('.bypass-method-codes tr td').first().innerText();
  expect(backupCode).toMatch(/^\d{6}$/);

  await page.locator('#logout').click();

  await loginWithCas(page);

  await page.locator('#methodChoices a').click();
  await page.locator('#token').fill(backupCode);
  await expect(page).toHaveURL(/localhost:4000/);

  await page.locator('#bypass').click();
  await page.locator('.switch .lever').click();
  await expect(page.locator('#bypass_switch')).not.toBeChecked();
});

