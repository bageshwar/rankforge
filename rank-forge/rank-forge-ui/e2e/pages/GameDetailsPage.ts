import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class GameDetailsPage extends BasePage {
  readonly gameTitle = () => this.page.locator('.game-title');
  readonly headerScore = () => this.page.locator('.header-score');
  readonly headerScoreCT = () => this.page.locator('.header-score-ct');
  readonly headerScoreT = () => this.page.locator('.header-score-t');
  readonly gameMeta = () => this.page.locator('.game-meta');
  readonly backToGamesLink = () => this.page.locator('.back-btn');
  readonly roundTimeline = () => this.page.locator('.rounds-timeline');
  readonly roundBadges = () => this.page.locator('.round-badge');
  readonly playerStatsTable = () => this.page.locator('.stats-table');
  readonly playerStatsRows = () => this.page.locator('.stats-table tbody tr');
  readonly accoladesSection = () => this.page.locator('.accolades-section');
  readonly accoladesGrid = () => this.page.locator('.accolades-grid');
  readonly accoladeCards = () => this.page.locator('.accolade-card');

  constructor(page: Page) {
    super(page);
  }

  async navigate(gameId: number) {
    console.log(`[GameDetailsPage] Navigating to game details for game ${gameId}`);
    
    // Set up response listeners BEFORE navigation
    const gameResponsePromise = this.page.waitForResponse(
      (response) => response.url().includes(`/api/games/${gameId}`) && !response.url().includes('/details') && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log(`[GameDetailsPage] ⚠ Game API response listener timed out`);
      return null;
    });
    
    const detailsResponsePromise = this.page.waitForResponse(
      (response) => response.url().includes(`/api/games/${gameId}/details`) && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log(`[GameDetailsPage] ⚠ Game details API response listener timed out`);
      return null;
    });
    
    await this.goto(`/games/${gameId}`);
    await this.waitForLoadState();
    
    // Wait for either API responses or UI elements
    console.log(`[GameDetailsPage] Waiting for game API: /api/games/${gameId}`);
    try {
      await Promise.race([
        gameResponsePromise,
        this.page.waitForSelector('.game-title', { timeout: 70000, state: 'visible' })
      ]);
      console.log(`[GameDetailsPage] ✓ Game API response received or title visible`);
    } catch (error) {
      console.log(`[GameDetailsPage] ⚠ Wait timed out, checking if data is already loaded`);
    }
    
    console.log(`[GameDetailsPage] Waiting for game details API: /api/games/${gameId}/details`);
    try {
      await Promise.race([
        detailsResponsePromise,
        this.page.waitForSelector('.rounds-timeline, .stats-table', { timeout: 70000, state: 'visible' })
      ]);
      console.log(`[GameDetailsPage] ✓ Game details API response received or content visible`);
    } catch (error) {
      console.log(`[GameDetailsPage] ⚠ Wait timed out, checking if data is already loaded`);
    }
    
    // Wait for key elements to be visible
    console.log(`[GameDetailsPage] Verifying game title is visible`);
    await this.waitForSelector('.game-title', 30000);
    console.log(`[GameDetailsPage] Game details page loaded for game ${gameId}`);
  }

  async getMapName(): Promise<string | null> {
    return await this.gameTitle().textContent();
  }

  async getScore(): Promise<{ ct: string | null; t: string | null }> {
    const ct = await this.headerScoreCT().textContent();
    const t = await this.headerScoreT().textContent();
    return { ct, t };
  }

  async getRoundCount(): Promise<number> {
    console.log('[GameDetailsPage] Getting round count');
    const count = await this.roundBadges().count();
    console.log(`[GameDetailsPage] Found ${count} rounds`);
    return count;
  }

  async clickRound(roundNumber: number) {
    console.log(`[GameDetailsPage] Clicking round ${roundNumber}`);
    const roundBadge = this.roundBadges().nth(roundNumber - 1);
    await roundBadge.click();
    console.log(`[GameDetailsPage] Round ${roundNumber} clicked, waiting for navigation`);
    await this.waitForLoadState();
    // Wait for round details API call (with extended timeout for slow DB)
    // Extract gameId from current URL
    const currentUrl = this.page.url();
    const gameIdMatch = currentUrl.match(/\/games\/(\d+)/);
    if (gameIdMatch) {
      const gameId = gameIdMatch[1];
      console.log(`[GameDetailsPage] Waiting for round details API: /api/games/${gameId}/rounds/${roundNumber}`);
      await this.waitForApiResponse(`/api/games/${gameId}/rounds/${roundNumber}`, 70000);
    }
    console.log(`[GameDetailsPage] Navigation to round ${roundNumber} complete`);
  }

  async getRoundBadgeClass(roundNumber: number): Promise<string | null> {
    const roundBadge = this.roundBadges().nth(roundNumber - 1);
    return await roundBadge.getAttribute('class');
  }

  async getPlayerStatsCount(): Promise<number> {
    return await this.playerStatsRows().count();
  }

  async getPlayerStatRow(index: number) {
    const rows = await this.playerStatsRows();
    return rows.nth(index);
  }

  async getPlayerStatData(index: number) {
    const row = await this.getPlayerStatRow(index);
    const playerName = await row.locator('.player-name-cell').textContent();
    const kills = await row.locator('.kills-cell').textContent();
    const deaths = await row.locator('.deaths-cell').textContent();
    const assists = await row.locator('.assists-cell').textContent();
    const kd = await row.locator('.rating-cell').textContent();
    
    return {
      playerName: playerName?.trim() || '',
      kills: kills?.trim() || '',
      deaths: deaths?.trim() || '',
      assists: assists?.trim() || '',
      kd: kd?.trim() || '',
    };
  }

  async clickPlayerLink(index: number) {
    const row = await this.getPlayerStatRow(index);
    const playerLink = row.locator('.player-profile-link');
    await playerLink.click();
    await this.waitForLoadState();
  }

  async getAccoladesCount(): Promise<number> {
    return await this.accoladeCards().count();
  }

  async getAccoladeCard(index: number) {
    const cards = await this.accoladeCards();
    return cards.nth(index);
  }

  async getAccoladeData(index: number) {
    const card = await this.getAccoladeCard(index);
    const type = await card.locator('.accolade-type').textContent();
    const playerName = await card.locator('.accolade-player-link, .accolade-player').textContent();
    const value = await card.locator('.accolade-value').textContent();
    
    return {
      type: type?.trim() || '',
      playerName: playerName?.trim() || '',
      value: value?.trim() || '',
    };
  }

  async clickAccoladePlayerLink(index: number) {
    const card = await this.getAccoladeCard(index);
    const playerLink = card.locator('.accolade-player-link');
    if (await playerLink.count() > 0) {
      await playerLink.click();
      await this.waitForLoadState();
    }
  }
}
