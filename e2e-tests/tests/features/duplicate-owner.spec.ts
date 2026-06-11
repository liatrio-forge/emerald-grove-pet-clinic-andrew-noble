import { test, expect } from '@fixtures/base-test';

import { OwnerPage } from '@pages/owner-page';
import { createOwner } from '@utils/data-factory';

test.describe('Duplicate Owner Prevention', () => {
  test('blocks creating a duplicate owner and does not create a second record', async ({ page }, testInfo) => {
    const ownerPage = new OwnerPage(page);
    const owner = createOwner();

    // Create the owner for the first time — should succeed and land on details.
    await ownerPage.openFindOwners();
    await ownerPage.clickAddOwner();
    await ownerPage.fillOwnerForm(owner);
    await ownerPage.submitOwnerForm();
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();

    // Attempt to create the exact same owner again — should be blocked with an error.
    await ownerPage.openFindOwners();
    await ownerPage.clickAddOwner();
    await ownerPage.fillOwnerForm(owner);
    await ownerPage.submitOwnerForm();

    // The creation form is redisplayed with a visible duplicate error and the
    // submitted last name preserved (no redirect to a new details page).
    await expect(ownerPage.duplicateError()).toBeVisible();
    await expect(page.getByLabel(/Last Name/i)).toHaveValue(owner.lastName);
    await page.screenshot({ path: testInfo.outputPath('duplicate-owner-error.png'), fullPage: true });

    // No second record was created: searching by the unique last name resolves to
    // a single owner and redirects straight to that owner's details page.
    await ownerPage.openFindOwners();
    await ownerPage.searchByLastName(owner.lastName);
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();
    await expect(page).toHaveURL(/\/owners\/\d+$/);
  });
});
