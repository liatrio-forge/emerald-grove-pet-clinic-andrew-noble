import { test, expect } from '@fixtures/base-test';

import { VisitPage } from '@pages/visit-page';

/**
 * Returns a date offset from today, formatted as yyyy-MM-dd.
 * Use non-negative offsets for valid (today/future) visit dates and
 * negative offsets to exercise the past-date validation rule.
 */
function isoDateOffset(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

test.describe('Visit Scheduling', () => {
  test('can schedule a visit for an existing pet', async ({ page }, testInfo) => {
    const visitPage = new VisitPage(page);
    // Note: searching by last name may redirect directly to owner details when there is a single match.
    // Use a stable direct URL to avoid depending on the owners list table.
    await page.goto('/owners/1');
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();

    const addVisitLink = page.getByRole('link', { name: /^Add Visit$/i }).first();
    const addVisitHref = await addVisitLink.getAttribute('href');
    if (!addVisitHref) {
      throw new Error('Expected Add Visit link to have an href');
    }

    const petIdMatch = addVisitHref.match(/pets\/(\d+)\//);
    if (!petIdMatch) {
      throw new Error(`Expected Add Visit href to include pet id, got: ${addVisitHref}`);
    }

    const petId = petIdMatch[1];

    await addVisitLink.click();

    await expect(visitPage.heading()).toBeVisible();

    // Use today's date so the visit is valid under the "today or later" rule.
    const visitDate = isoDateOffset(0);
    const description = `E2E visit ${Date.now()}`;
    await visitPage.fillVisitDate(visitDate);
    await visitPage.fillDescription(description);

    await page.screenshot({ path: testInfo.outputPath('visit-scheduling-form.png'), fullPage: true });

    await visitPage.submit();

    await expect(page.getByRole('heading', { name: /Pets and Visits/i })).toBeVisible();

    const petVisitsTable = page
      .locator(`a[href*="pets/${petId}/visits/new"]`)
      .first()
      .locator('xpath=ancestor::table[1]');

    const visitRow = petVisitsTable.locator('tr').filter({ hasText: visitDate }).filter({ hasText: description });
    await expect(visitRow).toHaveCount(1);
  });

  test('validates visit description is required', async ({ page }) => {
    const visitPage = new VisitPage(page);
    await page.goto('/owners/1');
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();

    await page.getByRole('link', { name: /Add Visit/i }).first().click();

    // A valid (today) date isolates the failure to the missing description.
    await visitPage.fillVisitDate(isoDateOffset(0));
    await visitPage.submit();

    await expect(page.getByText(/must not be blank/i)).toBeVisible();
  });

  test('rejects a visit scheduled in the past', async ({ page }, testInfo) => {
    const visitPage = new VisitPage(page);
    await page.goto('/owners/1');
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();

    await page.getByRole('link', { name: /Add Visit/i }).first().click();
    await expect(visitPage.heading()).toBeVisible();

    const pastDate = isoDateOffset(-1);
    const description = `E2E past-date visit ${Date.now()}`;
    await visitPage.fillVisitDate(pastDate);
    await visitPage.fillDescription(description);
    await visitPage.submit();

    // The localized validation error is shown...
    await expect(page.getByText(/must be today or a future date/i)).toBeVisible();
    // ...the user stays on the visit form (not redirected to the owner page)...
    await expect(visitPage.heading()).toBeVisible();
    await expect(page.getByRole('heading', { name: /Pets and Visits/i })).toHaveCount(0);
    // ...and the entered description is preserved on the re-rendered form.
    await expect(page.locator('input#description')).toHaveValue(description);

    await page.screenshot({ path: testInfo.outputPath('visit-past-date-rejected.png'), fullPage: true });
  });
});
