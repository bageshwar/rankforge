import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class RoundDetailsPage extends BasePage {
  readonly roundHeader = () => this.page.locator('.round-header');
  readonly roundTitle = () => this.page.locator('.round-title');
  readonly roundWinnerBadge = () => this.page.locator('.round-winner-badge');
  readonly roundStatsRow = () => this.page.locator('.round-stats-row');
  readonly bombStatusBar = () => this.page.locator('.bomb-status-bar');
  readonly backToGameLink = () => this.page.locator('.back-btn');
  readonly eventsSection = () => this.page.locator('.events-section');
  readonly eventsTimeline = () => this.page.locator('.events-timeline');
  readonly eventCards = () => this.page.locator('.event-card');
  readonly killFeedSection = () => this.page.locator('.kill-feed-section');
  readonly killFeedItems = () => this.page.locator('.kill-feed-item');

  constructor(page: Page) {
    super(page);
  }

  async navigate(gameId: number, roundNumber: number) {
    console.log(`[RoundDetailsPage] Navigating to round ${roundNumber} for game ${gameId}`);
    
    // Set up response listener BEFORE navigation
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().includes(`/api/games/${gameId}/rounds/${roundNumber}`) && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log(`[RoundDetailsPage] ⚠ Response listener timed out`);
      return null;
    });
    
    await this.goto(`/games/${gameId}/rounds/${roundNumber}`);
    await this.waitForLoadState();
    
    // Wait for either API response or UI elements
    console.log(`[RoundDetailsPage] Waiting for round details API: /api/games/${gameId}/rounds/${roundNumber}`);
    try {
      await Promise.race([
        responsePromise,
        this.page.waitForSelector('.round-header', { timeout: 70000, state: 'visible' })
      ]);
      console.log(`[RoundDetailsPage] ✓ Round details API response received or header visible`);
    } catch (error) {
      console.log(`[RoundDetailsPage] ⚠ Wait timed out, checking if data is already loaded`);
    }
    
    // Wait for key elements to be visible
    console.log(`[RoundDetailsPage] Verifying round header is visible`);
    await this.waitForSelector('.round-header', 30000);
    console.log(`[RoundDetailsPage] Round details page loaded for game ${gameId}, round ${roundNumber}`);
  }

  async getRoundNumber(): Promise<string | null> {
    return await this.roundTitle().textContent();
  }

  async getWinnerTeam(): Promise<string | null> {
    const winnerText = await this.roundWinnerBadge().locator('.winner-text').textContent();
    return winnerText;
  }

  async getRoundStats() {
    const stats = this.roundStatsRow().locator('.round-stat');
    const count = await stats.count();
    const result: Record<string, string> = {};
    
    for (let i = 0; i < count; i++) {
      const stat = stats.nth(i);
      const label = await stat.locator('.stat-label').textContent();
      const value = await stat.locator('.stat-value').textContent();
      if (label && value) {
        result[label.trim()] = value.trim();
      }
    }
    
    return result;
  }

  async getBombStatus(): Promise<string[]> {
    const statuses = this.bombStatusBar().locator('.bomb-status');
    const count = await statuses.count();
    const result: string[] = [];
    
    for (let i = 0; i < count; i++) {
      const status = await statuses.nth(i).textContent();
      if (status) {
        result.push(status.trim());
      }
    }
    
    return result;
  }

  async getEventCount(): Promise<number> {
    return await this.eventCards().count();
  }

  async getEventCard(index: number) {
    const cards = await this.eventCards();
    return cards.nth(index);
  }

  async getEventData(index: number) {
    const card = await this.getEventCard(index);
    const time = await card.locator('.time-badge').textContent();
    const eventType = await card.locator('.event-type-badge').textContent();
    const icon = await card.locator('.event-icon').textContent();
    
    return {
      time: time?.trim() || '',
      eventType: eventType?.trim() || '',
      icon: icon?.trim() || '',
    };
  }

  async clickEventPlayerLink(index: number, linkType: 'attacker' | 'victim' | 'assister' | 'bomber' = 'attacker') {
    const card = await this.getEventCard(index);
    const playerLink = card.locator(`.player-link.${linkType}`);
    if (await playerLink.count() > 0) {
      await playerLink.click();
      await this.waitForLoadState();
    }
  }

  async getKillFeedCount(): Promise<number> {
    return await this.killFeedItems().count();
  }

  async getKillFeedItem(index: number) {
    const items = await this.killFeedItems();
    return items.nth(index);
  }

  async clickKillFeedPlayerLink(index: number, linkType: 'killer' | 'victim' = 'killer') {
    const item = await this.getKillFeedItem(index);
    const linkSelector = linkType === 'killer' ? '.killer-name' : '.victim-name';
    const playerLink = item.locator(linkSelector);
    if (await playerLink.count() > 0) {
      await playerLink.click();
      await this.waitForLoadState();
    }
  }
}
