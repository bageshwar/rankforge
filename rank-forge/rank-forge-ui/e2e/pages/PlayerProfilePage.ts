import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class PlayerProfilePage extends BasePage {
  readonly profileHeader = () => this.page.locator('.profile-header');
  readonly playerName = () => this.page.locator('.player-name');
  readonly playerIdBadge = () => this.page.locator('.player-id-badge');
  readonly rankBadge = () => this.page.locator('.rank-badge');
  readonly currentRating = () => this.page.locator('.current-rating');
  readonly statsGrid = () => this.page.locator('.stats-grid');
  readonly statCards = () => this.page.locator('.stat-card');
  readonly ratingChart = () => this.page.locator('.rating-chart');
  readonly accoladesSection = () => this.page.locator('.accolades-summary');
  readonly recentAccolades = () => this.page.locator('.recent-accolades');
  readonly accoladesList = () => this.page.locator('.accolades-list .accolade-item');
  readonly backToRankingsLink = () => this.page.locator('.back-btn');
  readonly pastNicksSection = () => this.page.locator('.past-nicks-section');
  readonly pastNicksList = () => this.page.locator('.past-nicks-list');
  readonly pastNickItems = () => this.page.locator('.past-nick-item');

  constructor(page: Page) {
    super(page);
  }

  async navigate(playerId: string) {
    console.log(`[PlayerProfilePage] Navigating to player profile for player ${playerId}`);
    
    const encodedId = encodeURIComponent(playerId);
    // Set up response listener BEFORE navigation
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().includes(`/api/players/${encodedId}`) && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log(`[PlayerProfilePage] ⚠ Response listener timed out`);
      return null;
    });
    
    await this.goto(`/players/${playerId}`);
    await this.waitForLoadState();
    
    // Wait for either API response or UI elements
    console.log(`[PlayerProfilePage] Waiting for player profile API: /api/players/${encodedId}`);
    try {
      await Promise.race([
        responsePromise,
        this.page.waitForSelector('.profile-header', { timeout: 70000, state: 'visible' })
      ]);
      console.log(`[PlayerProfilePage] ✓ Player profile API response received or header visible`);
    } catch (error) {
      console.log(`[PlayerProfilePage] ⚠ Wait timed out, checking if data is already loaded`);
    }
    
    // Wait for key elements to be visible
    console.log(`[PlayerProfilePage] Verifying profile header is visible`);
    await this.waitForSelector('.profile-header', 30000);
    console.log(`[PlayerProfilePage] Player profile page loaded for player ${playerId}`);
  }

  async getPlayerName(): Promise<string | null> {
    return await this.playerName().textContent();
  }

  async getPlayerId(): Promise<string | null> {
    return await this.playerIdBadge().textContent();
  }

  async getCurrentRank(): Promise<string | null> {
    const rankNumber = await this.rankBadge().locator('.rank-number').textContent();
    return rankNumber;
  }

  async getKDRatio(): Promise<string | null> {
    const ratingValue = await this.currentRating().locator('.rating-value').textContent();
    return ratingValue;
  }

  async getStatCardCount(): Promise<number> {
    return await this.statCards().count();
  }

  async getStatCardData(index: number) {
    const card = await this.statCards().nth(index);
    const icon = await card.locator('.stat-icon').textContent();
    const value = await card.locator('.stat-value').textContent();
    const label = await card.locator('.stat-label').textContent();
    
    return {
      icon: icon?.trim() || '',
      value: value?.trim() || '',
      label: label?.trim() || '',
    };
  }

  async hasRatingChart(): Promise<boolean> {
    return await this.ratingChart().isVisible();
  }

  async getAccoladesCount(): Promise<number> {
    return await this.accoladesList().count();
  }

  async getAccoladeItem(index: number) {
    const items = await this.accoladesList();
    return items.nth(index);
  }

  async getAccoladeItemData(index: number) {
    const item = await this.getAccoladeItem(index);
    const type = await item.locator('.accolade-type').textContent();
    const position = await item.locator('.accolade-position').textContent();
    const value = await item.locator('.value-number').first().textContent();
    
    return {
      type: type?.trim() || '',
      position: position?.trim() || '',
      value: value?.trim() || '',
    };
  }

  async clickViewGameLink(index: number) {
    const item = await this.getAccoladeItem(index);
    const gameLink = item.locator('.view-game-link');
    if (await gameLink.count() > 0) {
      await gameLink.click();
      await this.waitForLoadState();
    }
  }

  async hasPastNicksSection(): Promise<boolean> {
    return await this.pastNicksSection().isVisible();
  }

  async getPastNicksCount(): Promise<number> {
    return await this.pastNickItems().count();
  }

  async getPastNickText(index: number): Promise<string | null> {
    const items = await this.pastNickItems();
    return await items.nth(index).textContent();
  }

  async getAllPastNicks(): Promise<string[]> {
    const items = await this.pastNickItems();
    const count = await items.count();
    const nicks: string[] = [];
    for (let i = 0; i < count; i++) {
      const text = await items.nth(i).textContent();
      if (text) {
        nicks.push(text.trim());
      }
    }
    return nicks;
  }
}
