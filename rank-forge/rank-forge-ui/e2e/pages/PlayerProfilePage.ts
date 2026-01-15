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
    const nameLocator = this.playerName();
    await nameLocator.scrollIntoViewIfNeeded();
    return await this.safeTextContent(nameLocator);
  }

  async getPlayerId(): Promise<string | null> {
    const idLocator = this.playerIdBadge();
    await idLocator.scrollIntoViewIfNeeded();
    return await this.safeTextContent(idLocator);
  }

  async getCurrentRank(): Promise<string | null> {
    const rankBadge = this.rankBadge();
    await rankBadge.scrollIntoViewIfNeeded();
    const rankNumber = await this.safeTextContent(rankBadge.locator('.rank-number'));
    return rankNumber;
  }

  async getKDRatio(): Promise<string | null> {
    const rating = this.currentRating();
    await rating.scrollIntoViewIfNeeded();
    const ratingValue = await this.safeTextContent(rating.locator('.rating-value'));
    return ratingValue;
  }

  async getStatCardCount(): Promise<number> {
    return await this.statCards().count();
  }

  async getStatCardData(index: number) {
    const card = await this.statCards().nth(index);
    // Wait for card to be visible and scroll into view
    await card.waitFor({ state: 'visible', timeout: 10000 });
    await card.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    
    const icon = await this.safeTextContent(card.locator('.stat-icon'));
    const value = await this.safeTextContent(card.locator('.stat-value'));
    const label = await this.safeTextContent(card.locator('.stat-label'));
    
    return {
      icon: icon || '',
      value: value || '',
      label: label || '',
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
    // Wait for item to be visible and scroll into view
    await item.waitFor({ state: 'visible', timeout: 10000 });
    await item.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    
    const type = await this.safeTextContent(item.locator('.accolade-type'));
    const position = await this.safeTextContent(item.locator('.accolade-position'));
    const value = await this.safeTextContent(item.locator('.value-number').first());
    
    return {
      type: type || '',
      position: position || '',
      value: value || '',
    };
  }

  async clickViewGameLink(index: number) {
    const item = await this.getAccoladeItem(index);
    await item.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    const gameLink = item.locator('.view-game-link');
    if (await this.safeCount(gameLink) > 0) {
      await this.safeClick(gameLink);
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
    const item = items.nth(index);
    // Wait for item to be visible and scroll into view
    await item.waitFor({ state: 'visible', timeout: 10000 });
    await item.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    return await this.safeTextContent(item);
  }

  async getAllPastNicks(): Promise<string[]> {
    const items = await this.pastNickItems();
    const count = await items.count();
    const nicks: string[] = [];
    for (let i = 0; i < count; i++) {
      const item = items.nth(i);
      // Scroll each item into view
      try {
        await item.scrollIntoViewIfNeeded();
        await this.page.waitForTimeout(100);
      } catch (error) {
        // Continue if scroll fails
      }
      const text = await this.safeTextContent(item);
      if (text) {
        nicks.push(text);
      }
    }
    return nicks;
  }
}
