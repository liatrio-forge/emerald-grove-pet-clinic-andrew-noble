import { test, expect } from '@fixtures/base-test';

import { OwnerPage } from '@pages/owner-page';
import { VetPage } from '@pages/vet-page';
import { createOwner } from '@utils/data-factory';

const PROOF_IMG_DIR = '../docs/specs/06-spec-preserve-filters-pagination/06-proofs/img';

// Page size for the Owners list (see OwnerController#findPaginatedForOwnersCriteria).
const PAGE_SIZE = 5;

test.describe('Preserve filters across pagination', () => {
  test('Owners: filtered results stay filtered when paging forward and back', async ({ page }) => {
    const ownerPage = new OwnerPage(page);

    // Seed data yields <1 page for any owner filter, so create a cohort of owners
    // that share a unique last name to produce a real multi-page filtered result.
    const sharedLastName = `Pagefilter${Date.now()}`;
    const cohortSize = PAGE_SIZE + 1; // 6 owners -> 2 pages (5 + 1)
    for (let i = 0; i < cohortSize; i++) {
      const owner = createOwner({ lastName: sharedLastName, firstName: `Cohort${i}${Date.now()}` });
      await ownerPage.openFindOwners();
      await ownerPage.clickAddOwner();
      await ownerPage.fillOwnerForm(owner);
      await ownerPage.submitOwnerForm();
      await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();
    }

    // Apply the filter.
    await ownerPage.openFindOwners();
    await ownerPage.searchByLastName(sharedLastName);
    await expect(ownerPage.ownersTable()).toBeVisible();

    const pagination = page.locator('.liatrio-pagination');
    const rows = ownerPage.ownersTable().locator('tbody tr');

    // Page 1: full page, pagination control present, every row matches the filter.
    await expect(pagination).toBeVisible();
    await expect(rows).toHaveCount(PAGE_SIZE);
    for (let i = 0; i < (await rows.count()); i++) {
      await expect(rows.nth(i)).toContainText(sharedLastName);
    }

    // Page forward via the numbered "2" link; the URL must carry the filter.
    await pagination.getByRole('link', { name: '2', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`[?&]page=2\\b`));
    await expect(page).toHaveURL(new RegExp(`[?&]lastName=${sharedLastName}\\b`));

    // Page 2: remaining row(s), still inside the filtered set.
    await expect(rows).toHaveCount(cohortSize - PAGE_SIZE);
    for (let i = 0; i < (await rows.count()); i++) {
      await expect(rows.nth(i)).toContainText(sharedLastName);
    }

    // Proof screenshot: a later filtered page with the filter visible in the URL.
    await ownerPage.screenshot(`${PROOF_IMG_DIR}/owners-filtered-page-2.png`);

    // Page back to the first page; the filter must persist.
    await pagination.getByRole('link', { name: '1', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`[?&]lastName=${sharedLastName}\\b`));
    await expect(rows).toHaveCount(PAGE_SIZE);
  });

  test('Vets: specialty filter is carried in the URL (link parameter preserved)', async ({ page }) => {
    // Seed data has a single page per specialty, so multi-page link preservation is
    // covered by the VetControllerTests regression test. Here we assert the filter
    // parameter itself is preserved in the shareable URL after filtering.
    const vetPage = new VetPage(page);
    await vetPage.open();

    await vetPage.selectSpecialty('surgery');
    await expect(page).toHaveURL(/[?&]specialty=surgery\b/);
    await expect(vetPage.specialtyFilter()).toHaveValue('surgery');

    await vetPage.screenshot(`${PROOF_IMG_DIR}/vets-filter-surgery.png`);
  });
});
