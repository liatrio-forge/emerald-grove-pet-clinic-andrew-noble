import type { Locator, Page } from '@playwright/test';

import { BasePage } from './base-page';

export class VetPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  heading(): Locator {
    return this.page.getByRole('heading', { name: /Veterinarians/i });
  }

  vetsTable(): Locator {
    return this.page.locator('table#vets');
  }

  vetRows(): Locator {
    return this.vetsTable().locator('tbody tr');
  }

  specialtyFilter(): Locator {
    return this.page.locator('select[name="specialty"]');
  }

  async open(): Promise<void> {
    await this.goto('/vets.html');
    await this.heading().waitFor();
  }

  async openFiltered(specialty: string): Promise<void> {
    await this.goto(`/vets.html?specialty=${encodeURIComponent(specialty)}`);
    await this.heading().waitFor();
  }

  async selectSpecialty(value: string): Promise<void> {
    // The dropdown auto-submits via onchange, triggering a navigation.
    await Promise.all([this.page.waitForNavigation(), this.specialtyFilter().selectOption(value)]);
    await this.heading().waitFor();
  }
}
