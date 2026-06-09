import { test, expect } from '@fixtures/base-test';

import { HomePage } from '@pages/home-page';

const ARTIFACTS = 'test-results/artifacts';

test.describe('Header language selector', () => {
  test('switches UI language, persists across navigation, and marks the active language', async ({ page }) => {
    const home = new HomePage(page);
    await home.open();

    // The selector is visible in the global header.
    await expect(home.languageSelector()).toBeVisible();

    // Baseline (English) screenshot for the proof artifacts.
    await home.screenshot(`${ARTIFACTS}/home-en.png`);

    // Expanded dropdown screenshot (proof for the rendering task).
    await home.openLanguageMenu();
    await expect(home.languageSelector().getByRole('link', { name: 'Español', exact: true })).toBeVisible();
    await page.screenshot({ path: `${ARTIFACTS}/lang-selector-expanded.png` });

    // Switch to Spanish.
    await home.languageSelector().getByRole('link', { name: 'Español', exact: true }).click();

    // Visible nav text is now Spanish (AC: selecting a language updates UI text).
    await expect(home.navLink(/Inicio/)).toBeVisible();
    await expect(home.navLink(/Buscar propietarios/)).toBeVisible();
    await home.screenshot(`${ARTIFACTS}/home-es.png`);

    // The selector reflects the active language.
    await home.openLanguageMenu();
    await expect(home.activeLanguageOption()).toHaveText('Español');

    // Persistence: navigating to another page keeps the UI in Spanish (AC #3).
    await home.navLink(/Buscar propietarios/).click();
    await expect(home.navLink(/Inicio/)).toBeVisible();
    await expect(home.navLink(/Veterinarios/)).toBeVisible();
  });
});
