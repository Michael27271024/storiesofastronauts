const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const errors = [];
  page.on('console', msg => {
    const type = msg.type();
    if (type === 'error' || type === 'warning') {
      errors.push(`${type}: ${msg.text()}`);
    }
  });
  page.on('pageerror', err => errors.push(`pageerror: ${err.message}`));
  page.on('requestfailed', req => errors.push(`requestfailed: ${req.url()} :: ${req.failure()?.errorText}`));
  await page.goto('http://127.0.0.1:8000/app/src/main/assets/stages/stage4.html', { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(3000);
  console.log('TITLE', await page.title());
  console.log('BODY', (await page.locator('body').innerText()).slice(0, 400));
  console.log('ERRORS');
  console.log(errors.slice(0, 50).join('\n'));
  await browser.close();
})();
