import { test, expect } from '../fixtures/base-test';

test.describe('Friendly 404 for missing owner/pet', () => {
  test('navigating to a non-existent owner returns 404 with a friendly message', async ({ page }, testInfo) => {
    const response = await page.goto('/owners/999999');

    expect(response?.status()).toBe(404);
    await expect(page.getByText(/The requested page was not found\./i)).toBeVisible();

    await page.screenshot({ path: testInfo.outputPath('owner-not-found.png'), fullPage: true });
  });

  test('navigating to a non-existent pet returns 404 with a friendly message', async ({ page }) => {
    const response = await page.goto('/owners/1/pets/999999/edit');

    expect(response?.status()).toBe(404);
    await expect(page.getByText(/The requested page was not found\./i)).toBeVisible();
  });

  test('the 404 page links back to Find Owners', async ({ page }) => {
    await page.goto('/owners/999999');

    const findOwnersLink = page.locator('.liatrio-error-card').getByRole('link', { name: /Find Owners/i });
    await expect(findOwnersLink).toBeVisible();

    await findOwnersLink.click();
    await expect(page).toHaveURL(/\/owners\/find$/);
    await expect(page.getByRole('heading', { name: /Find Owners/i })).toBeVisible();
  });

  test('the 404 page does not leak internal exception detail', async ({ page }) => {
    await page.goto('/owners/999999');

    // The friendly page must not expose the raw exception text or stack traces.
    await expect(page.locator('body')).not.toContainText(/Owner not found with id/i);
    await expect(page.locator('body')).not.toContainText(/NotFoundException/i);
  });
});
