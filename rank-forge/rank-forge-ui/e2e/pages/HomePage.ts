import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class HomePage extends BasePage {
  readonly rankingsLink = () => this.page.locator('[data-testid="testid-home-rankings-link"]');
  readonly gamesLink = () => this.page.locator('[data-testid="testid-home-games-link"]');
  readonly heroTitle = () => this.page.locator('.hero-title');
  readonly navCards = () => this.page.locator('.nav-cards');

  constructor(page: Page) {
    super(page);
  }

  async navigate() {
    console.log('[HomePage] Navigating to home page');
    await this.goto('/');
    await this.waitForLoadState();
    console.log('[HomePage] Home page loaded');
  }

  async clickRankingsLink() {
    console.log('[HomePage] Clicking rankings link');
    await this.rankingsLink().click();
    console.log('[HomePage] Rankings link clicked, waiting for navigation');
    await this.waitForLoadState();
    // Wait for rankings API call if navigating to rankings
    await this.waitForApiResponse('/api/rankings', 70000);
    console.log('[HomePage] Navigation to rankings page complete');
  }

  async clickGamesLink() {
    console.log('[HomePage] Clicking games link');
    await this.gamesLink().click();
    console.log('[HomePage] Games link clicked, waiting for navigation');
    await this.waitForLoadState();
    // Wait for games API call if navigating to games
    await this.waitForApiResponse('/api/games', 70000);
    console.log('[HomePage] Navigation to games page complete');
  }

  async verifyRankingsLinkVisible() {
    console.log('[HomePage] Verifying rankings link is visible');
    await this.rankingsLink().waitFor({ state: 'visible', timeout: 30000 });
    const isVisible = await this.rankingsLink().isVisible();
    console.log(`[HomePage] Rankings link visible: ${isVisible}`);
    return isVisible;
  }

  async verifyGamesLinkVisible() {
    console.log('[HomePage] Verifying games link is visible');
    await this.gamesLink().waitFor({ state: 'visible', timeout: 30000 });
    const isVisible = await this.gamesLink().isVisible();
    console.log(`[HomePage] Games link visible: ${isVisible}`);
    return isVisible;
  }
}
