import type { Locator, Page } from '@playwright/test';

import { BasePage } from './base-page';

/**
 * Page object for the read-only Upcoming Visits page (`/visits/upcoming`).
 */
export class UpcomingVisitsPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  /** Navigate to the page, optionally with a `days` window override. */
  async open(days?: number): Promise<void> {
    await this.goto(days === undefined ? '/visits/upcoming' : `/visits/upcoming?days=${days}`);
  }

  heading(): Locator {
    return this.page.getByRole('heading', { name: /Upcoming Visits/i });
  }

  table(): Locator {
    return this.page.locator('table#upcomingVisits');
  }

  /** A table row matching the given visit description. */
  rowFor(description: string): Locator {
    return this.table().locator('tbody tr').filter({ hasText: description });
  }

  emptyState(): Locator {
    return this.page.getByText(/no upcoming visits/i);
  }
}
