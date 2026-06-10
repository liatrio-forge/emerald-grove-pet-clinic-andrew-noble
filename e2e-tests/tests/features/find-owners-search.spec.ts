import { test, expect } from '@fixtures/base-test';

import { OwnerPage } from '@pages/owner-page';
import { createOwner } from '@utils/data-factory';

const PROOF_IMG_DIR = '../docs/specs/05-spec-find-owners-by-phone-city/05-proofs/img';

test.describe('Find Owners Search', () => {
  test('finds a newly created owner by telephone and by city', async ({ page }) => {
    const ownerPage = new OwnerPage(page);

    // Create an owner with a unique city and telephone so each search resolves uniquely.
    const unique = `${Date.now()}`.slice(-6);
    const owner = createOwner({ city: `Findburg${unique}` });
    const fullName = `${owner.firstName} ${owner.lastName}`;

    await ownerPage.openFindOwners();
    // Capture the updated Find Owners form (Last name + Telephone + City inputs).
    await ownerPage.screenshot(`${PROOF_IMG_DIR}/find-owners-form.png`);
    await expect(page.locator('input#telephone')).toBeVisible();
    await expect(page.locator('input#city')).toBeVisible();

    await ownerPage.clickAddOwner();
    await ownerPage.fillOwnerForm(owner);
    await ownerPage.submitOwnerForm();
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();

    // Find by telephone (exact, unique) -> redirects straight to the owner's details.
    await ownerPage.openFindOwners();
    await ownerPage.searchByTelephone(owner.telephone);
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();
    await expect(page.getByRole('cell', { name: fullName })).toBeVisible();

    // Find by city (unique) -> resolves to the same owner.
    await ownerPage.openFindOwners();
    await ownerPage.searchByCity(owner.city);
    await expect(page.getByRole('heading', { name: /Owner Information/i })).toBeVisible();
    await expect(page.getByRole('cell', { name: fullName })).toBeVisible();
  });

  test('rejects an invalid telephone with a clear validation message', async ({ page }) => {
    const ownerPage = new OwnerPage(page);

    await ownerPage.openFindOwners();
    await ownerPage.searchByTelephone('abc');

    await expect(ownerPage.telephoneError()).toBeVisible();
    // Still on the find form (no search performed).
    await expect(page.getByRole('heading', { name: /Find Owners/i })).toBeVisible();
    await ownerPage.screenshot(`${PROOF_IMG_DIR}/find-owners-invalid-telephone.png`);
  });
});
