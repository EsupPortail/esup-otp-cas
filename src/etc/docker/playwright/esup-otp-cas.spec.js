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

const PROXY_CONTROL = 'http://127.0.0.1:3002/fault';

async function setFault(request, target = null) {
  const response = await request.post(PROXY_CONTROL, { data: { target } });
  expect(response.ok()).toBeTruthy();
}

test.beforeEach(async ({ request }) => setFault(request));
test.afterEach(async ({ request }) => setFault(request));

async function activateBackupCodes(page) {
  page.on('dialog', dialog => dialog.accept());
  await loginWithCas(page);
  await page.locator('#bypass').click();
  if (!await page.locator('#bypass_switch').isChecked()) {
    await page.locator('.switch .lever').click();
  }
  await expect(page.locator('#bypass_switch')).toBeChecked();
  const backupCode = (await page.locator('.bypass-method-codes tr td').first().innerText()).trim();
  expect(backupCode).toMatch(/^\d{6}$/);
  await page.locator('#logout').click();
  return backupCode;
}

async function selectBackupCode(page) {
  await page.locator('#methodChoices a').click();
  await expect(page.locator('#token')).toBeVisible();
}

async function finishWithBackupCode(page, backupCode) {
  await page.locator('#token').fill(backupCode);
  await expect(page).toHaveURL(/localhost:4000/);
  await page.locator('#bypass').click();
  await page.locator('.switch .lever').click();
  await expect(page.locator('#bypass_switch')).not.toBeChecked();
}

async function expectMfaStillRequired(page, context, ticketRequests) {
  await expect(page).toHaveURL(/localhost:8080\/cas\/login/);
  expect(ticketRequests).toEqual([]);
  expect((await context.cookies()).filter(cookie => cookie.name === 'TGC')).toEqual([]);
}

function watchServiceTickets(page) {
  const requests = [];
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.searchParams.has('ticket')) requests.push(url.origin + url.pathname);
  });
  return requests;
}

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

test('An API timeout rejects OTP verification and does not grant a service ticket', async ({ page, context, request }) => {
  test.setTimeout(60000);
  const backupCode = await activateBackupCodes(page);
  await loginWithCas(page);
  await selectBackupCode(page);
  const ticketRequests = watchServiceTickets(page);

  await setFault(request, 'verification');
  await page.locator('#token').fill(backupCode);
  // Wait for the CAS failure response, rather than checking the URL while the request is pending.
  await expect(page.locator('#errors')).toBeVisible();
  expect(await (await request.get(PROXY_CONTROL)).json()).toEqual({ target: 'verification', hits: 1 });
  await expectMfaStillRequired(page, context, ticketRequests);

  // A retry without a fault proves the code was valid and restores the account state.
  await setFault(request);
  await selectBackupCode(page);
  await finishWithBackupCode(page, backupCode);
});

test('An explicit ESUP-OTP refusal issues no ticket or TGC and permits a valid retry', async ({ page, context, request }) => {
  const backupCode = await activateBackupCodes(page);
  await loginWithCas(page);
  await selectBackupCode(page);
  const ticketRequests = watchServiceTickets(page);

  await setFault(request, 'verification-refusal');
  await page.locator('#token').fill(backupCode);
  await expect(page.locator('#errors')).toBeVisible();
  expect(await (await request.get(PROXY_CONTROL)).json()).toEqual({ target: 'verification-refusal', hits: 1 });
  await expectMfaStillRequired(page, context, ticketRequests);

  await setFault(request);
  await selectBackupCode(page);
  await finishWithBackupCode(page, backupCode);
  expect(ticketRequests).toHaveLength(1);
  expect((await context.cookies()).filter(cookie => cookie.name === 'TGC')).toHaveLength(1);
});

test('CLOSED failure mode keeps MFA mandatory when the bypass user lookup times out', async ({ page, context, request }) => {
  test.setTimeout(60000);
  const backupCode = await activateBackupCodes(page);
  const ticketRequests = watchServiceTickets(page);

  await setFault(request, 'user');
  await loginWithCas(page);
  // The browser can load the methods after the single delayed CAS lookup.
  await selectBackupCode(page);
  expect(await (await request.get(PROXY_CONTROL)).json()).toEqual({ target: 'user', hits: 1 });
  await expectMfaStillRequired(page, context, ticketRequests);

  await setFault(request);
  await finishWithBackupCode(page, backupCode);
});

