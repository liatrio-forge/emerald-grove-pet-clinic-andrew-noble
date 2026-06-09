import { test, expect } from '@fixtures/base-test';

import { VetPage } from '@pages/vet-page';

const PROOF_IMG_DIR = '../docs/specs/04-spec-filter-vets-by-specialty/04-proofs/img';

test.describe('Vet Specialty Filter', () => {
  test('filters the list to a named specialty via the dropdown and reflects it in the URL', async ({ page }) => {
    const vetPage = new VetPage(page);
    await vetPage.open();

    // Baseline screenshot: the filter control is present on the directory.
    await vetPage.screenshot(`${PROOF_IMG_DIR}/vets-filter-default.png`);
    await expect(vetPage.specialtyFilter()).toBeVisible();

    await vetPage.selectSpecialty('surgery');

    // URL now carries the shareable filter parameter.
    await expect(page).toHaveURL(/[?&]specialty=surgery\b/);

    // Only surgery vets are shown (seed data: Linda Douglas, Rafael Ortega).
    const rows = vetPage.vetRows();
    await expect(rows).toHaveCount(2);
    for (let i = 0; i < (await rows.count()); i++) {
      await expect(rows.nth(i).locator('td').nth(1)).toContainText(/surgery/i);
    }

    await vetPage.screenshot(`${PROOF_IMG_DIR}/vets-filter-surgery.png`);
  });

  test('reproduces the same filtered list when navigating directly to a shared URL', async ({ page }) => {
    const vetPage = new VetPage(page);
    await vetPage.openFiltered('surgery');

    // The dropdown reflects the active selection from the URL.
    await expect(vetPage.specialtyFilter()).toHaveValue('surgery');

    const rows = vetPage.vetRows();
    await expect(rows).toHaveCount(2);
    for (let i = 0; i < (await rows.count()); i++) {
      await expect(rows.nth(i).locator('td').nth(1)).toContainText(/surgery/i);
    }
  });

  test('shows vets with no specialty when the "No specialty" option is selected', async ({ page }) => {
    const vetPage = new VetPage(page);
    await vetPage.open();

    await vetPage.selectSpecialty('none');

    await expect(page).toHaveURL(/[?&]specialty=none\b/);

    // Seed data: James Carter and Sharon Jenkins have no specialty.
    const rows = vetPage.vetRows();
    await expect(rows).toHaveCount(2);
    for (let i = 0; i < (await rows.count()); i++) {
      await expect(rows.nth(i).locator('td').nth(1)).toContainText(/none/i);
    }

    await vetPage.screenshot(`${PROOF_IMG_DIR}/vets-filter-none.png`);
  });
});
