import type { Locator, Page } from '@playwright/test';

export abstract class BasePage {
  protected readonly page: Page;

  protected constructor(page: Page) {
    this.page = page;
  }

  async goto(path: string): Promise<void> {
    await this.page.goto(path);
  }

  navLink(name: string | RegExp): Locator {
    return this.page.locator('nav.navbar').getByRole('link', { name });
  }

  async goHome(): Promise<void> {
    await this.navLink(/Home/i).click();
  }

  async goFindOwners(): Promise<void> {
    await this.navLink(/Find Owners/i).click();
  }

  async goVeterinarians(): Promise<void> {
    await this.navLink(/Veterinarians/i).click();
  }

  async screenshot(path: string): Promise<void> {
    await this.page.screenshot({ path, fullPage: true });
  }

  // --- Header language selector ---------------------------------------------

  languageSelector(): Locator {
    return this.page.locator('[data-testid="language-selector"]');
  }

  async openLanguageMenu(): Promise<void> {
    await this.languageSelector().locator('.dropdown-toggle').click();
  }

  async selectLanguage(name: string | RegExp): Promise<void> {
    await this.openLanguageMenu();
    await this.languageSelector().getByRole('link', { name }).click();
  }

  activeLanguageOption(): Locator {
    return this.languageSelector().locator('.dropdown-item.active');
  }
}
