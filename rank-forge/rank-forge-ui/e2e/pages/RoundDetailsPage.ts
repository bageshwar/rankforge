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
    // Wait for event card to be visible
    await card.waitFor({ state: 'visible', timeout: 30000 });
    
    // Try multiple selectors for time
    let time = '';
    const timeSelectors = [
      '.event-time .time-badge',
      '.event-time',
      '.time-badge',
      '[class*="time"]'
    ];
    for (const selector of timeSelectors) {
      const timeElement = card.locator(selector).first();
      if (await timeElement.count() > 0) {
        time = (await timeElement.textContent())?.trim() || '';
        if (time) break;
      }
    }
    
    // Try multiple selectors for event type
    let eventType = '';
    const eventTypeSelectors = [
      '.event-content .event-header .event-type-badge',
      '.event-type-badge',
      '.event-header',
      '.event-type',
      '[class*="event-type"]',
      '.event-content'
    ];
    for (const selector of eventTypeSelectors) {
      const eventTypeElement = card.locator(selector).first();
      if (await eventTypeElement.count() > 0) {
        eventType = (await eventTypeElement.textContent())?.trim() || '';
        if (eventType) break;
      }
    }
    
    // Try multiple selectors for icon
    let icon = '';
    const iconSelectors = [
      '.event-connector .event-icon',
      '.event-icon',
      '.event-connector',
      '[class*="icon"]'
    ];
    for (const selector of iconSelectors) {
      const iconElement = card.locator(selector).first();
      if (await iconElement.count() > 0) {
        icon = (await iconElement.textContent())?.trim() || '';
        if (icon) break;
      }
    }
    
    return {
      time: time || '',
      eventType: eventType || '',
      icon: icon || '',
    };
  }

  async clickEventPlayerLink(index: number, linkType: 'attacker' | 'victim' | 'assister' | 'bomber' = 'attacker') {
    console.log(`[RoundDetailsPage] Clicking event player link (${linkType}) for event at index ${index}`);
    // Wait for event cards to be visible
    await this.eventCards().first().waitFor({ state: 'visible', timeout: 30000 });
    const card = await this.getEventCard(index);
    const playerLink = card.locator(`.player-link.${linkType}`);
    
    // Wait for the link to be visible
    await playerLink.waitFor({ state: 'visible', timeout: 30000 });
    
    if (await playerLink.count() > 0) {
      await playerLink.click();
      console.log(`[RoundDetailsPage] Event player link clicked, waiting for navigation`);
      await this.waitForLoadState();
      await this.waitForApiResponse(new RegExp(`.*/api/players/.*`), 70000);
      console.log(`[RoundDetailsPage] Navigated to player profile from event timeline (${linkType})`);
    } else {
      console.log(`[RoundDetailsPage] ⚠ No player link found in event card at index ${index}`);
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
    console.log(`[RoundDetailsPage] Clicking kill feed player link (${linkType}) for item at index ${index}`);
    // Wait for kill feed items to be visible
    await this.killFeedItems().first().waitFor({ state: 'visible', timeout: 30000 });
    const item = await this.getKillFeedItem(index);
    const linkSelector = linkType === 'killer' ? '.killer-name' : '.victim-name';
    const playerLink = item.locator(linkSelector);
    
    // Wait for the link to be visible
    await playerLink.waitFor({ state: 'visible', timeout: 30000 });
    
    if (await playerLink.count() > 0) {
      await playerLink.click();
      console.log(`[RoundDetailsPage] Kill feed player link clicked, waiting for navigation`);
      await this.waitForLoadState();
      await this.waitForApiResponse(new RegExp(`.*/api/players/.*`), 70000);
      console.log(`[RoundDetailsPage] Navigated to player profile from kill feed (${linkType})`);
    } else {
      console.log(`[RoundDetailsPage] ⚠ No player link found in kill feed item at index ${index}`);
    }
  }
}
