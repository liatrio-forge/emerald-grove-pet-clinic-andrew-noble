import type { Page } from '@playwright/test';

import { test, expect } from '@fixtures/base-test';

import { VisitPage } from '@pages/visit-page';
import { UpcomingVisitsPage } from '@pages/upcoming-visits-page';

/**
 * Returns a date offset from today, formatted as yyyy-MM-dd. Visit dates must be
 * today or later (the app enforces a future-or-present rule), so only
 * non-negative offsets are used here.
 */
function isoDateOffset(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Schedule a visit for owner 1 (George Franklin) / first pet (Leo) via the UI,
 * returning once the owner detail page has re-rendered with the new visit.
 */
async function scheduleVisit(page: Page, visitDate: string, description: string): Promise<void> {
  const visitPage = new VisitPage(page);
  await page.goto('/owners/1');
  await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();

  await page.getByRole('link', { name: /^Add Visit$/i }).first().click();
  await expect(visitPage.heading()).toBeVisible();

  await visitPage.fillVisitDate(visitDate);
  await visitPage.fillDescription(description);
  await visitPage.submit();

  await expect(page.getByRole('heading', { name: /Pets and Visits/i })).toBeVisible();
}

test.describe('Upcoming Visits', () => {
  test('shows a visit created within the window', async ({ page }, testInfo) => {
    const today = isoDateOffset(0);
    const description = `E2E upcoming ${Date.now()}`;

    await scheduleVisit(page, today, description);

    const upcoming = new UpcomingVisitsPage(page);
    await upcoming.open();
    await expect(upcoming.heading()).toBeVisible();

    const row = upcoming.rowFor(description);
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('George Franklin');
    await expect(row).toContainText('Leo');
    await expect(row).toContainText(today);

    await page.screenshot({ path: testInfo.outputPath('upcoming-visits.png'), fullPage: true });
  });

  test('days parameter controls the window', async ({ page }) => {
    const farDate = isoDateOffset(20);
    const description = `E2E far ${Date.now()}`;

    await scheduleVisit(page, farDate, description);

    const upcoming = new UpcomingVisitsPage(page);

    // Default 7-day window excludes a visit ~20 days out.
    await upcoming.open();
    await expect(upcoming.rowFor(description)).toHaveCount(0);

    // Widening the window to 30 days includes it.
    await upcoming.open(30);
    await expect(upcoming.rowFor(description)).toHaveCount(1);
  });
});
