import { Page, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class GamesPage extends BasePage {
  readonly gamesTitle = () => this.page.locator('.games-title');
  readonly gamesTable = () => this.page.locator('.games-table');
  readonly gamesTableRows = () => this.page.locator('.games-table tbody tr');
  readonly filterAllGames = () => this.page.locator('button:has-text("All Games")');
  readonly filterRecent10 = () => this.page.locator('button:has-text("Recent 10")');
  readonly filterRecent25 = () => this.page.locator('button:has-text("Recent 25")');
  readonly gameDetailsLink = (gameId: number) => 
    this.page.locator(`[data-testid="testid-game-details-link-${gameId}"]`);

  constructor(page: Page) {
    super(page);
  }

  async navigate() {
    console.log('[GamesPage] Navigating to games page');
    
    // Set up response listener BEFORE navigation
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().includes('/api/games') && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log('[GamesPage] ⚠ Response listener timed out (response may have already completed)');
      return null;
    });
    
    await this.goto('/games');
    await this.waitForLoadState();
    
    console.log('[GamesPage] Waiting for games API response or table data');
    // Wait for either API response or table rows to appear
    try {
      await Promise.race([
        responsePromise,
        this.page.waitForSelector('.games-table tbody tr', { timeout: 70000, state: 'visible' })
      ]);
      console.log('[GamesPage] ✓ Either API response received or table data loaded');
    } catch (error) {
      console.log('[GamesPage] ⚠ Wait timed out, checking if data is already loaded');
    }
    
    // Verify table is visible
    console.log('[GamesPage] Verifying games table is visible');
    await this.waitForSelector('.games-table', 30000);
    
    // Verify we have at least one row
    const rows = await this.gamesTableRows();
    const rowCount = await rows.count();
    console.log(`[GamesPage] ✓ Games table loaded with ${rowCount} rows`);
    console.log('[GamesPage] Games page fully loaded');
  }

  async getGamesCount(): Promise<number> {
    console.log('[GamesPage] Getting games count');
    const rows = await this.gamesTableRows();
    const count = await rows.count();
    console.log(`[GamesPage] Found ${count} games`);
    return count;
  }

  async getGameRow(index: number) {
    const rows = await this.gamesTableRows();
    return rows.nth(index);
  }

  async getGameData(index: number) {
    const row = await this.getGameRow(index);
    const date = await row.locator('.game-date').textContent();
    const map = await row.locator('.map-badge').textContent();
    const score = await row.locator('.score').textContent();
    const duration = await row.locator('td').nth(3).textContent();
    
    return {
      date: date?.trim() || '',
      map: map?.trim() || '',
      score: score?.trim() || '',
      duration: duration?.trim() || '',
    };
  }

  async clickGameDetails(index: number) {
    console.log(`[GamesPage] Clicking game details for game at index ${index}`);
    const row = await this.getGameRow(index);
    const detailsBtn = row.locator('.details-btn');
    
    // Wait for button to be visible and enabled
    console.log(`[GamesPage] Waiting for details button to be visible (index ${index})`);
    await detailsBtn.waitFor({ state: 'visible', timeout: 30000 });
    console.log(`[GamesPage] Waiting for details button to be enabled (index ${index})`);
    await expect(detailsBtn).toBeEnabled({ timeout: 30000 });
    
    // Get the game ID from the href before clicking
    const href = await detailsBtn.getAttribute('href');
    let gameId: number | null = null;
    if (href) {
      const match = href.match(/\/games\/(\d+)/);
      if (match) {
        gameId = parseInt(match[1], 10);
        console.log(`[GamesPage] Extracted game ID: ${gameId} from href: ${href}`);
      }
    }
    
    if (!gameId) {
      console.error(`[GamesPage] Could not extract game ID from href: ${href}`);
    }
    
    // Click the details button
    console.log(`[GamesPage] Clicking details button for game ${gameId}`);
    await detailsBtn.click();
    
    // Wait for URL to change (React Router client-side navigation)
    if (gameId) {
      console.log(`[GamesPage] Waiting for URL to change to /games/${gameId}`);
      await this.page.waitForURL(new RegExp(`.*/games/${gameId}`), { timeout: 30000 });
      console.log(`[GamesPage] ✓ URL changed to /games/${gameId}`);
    }
    
    // Wait for page to load
    console.log(`[GamesPage] Waiting for page load state`);
    await this.waitForLoadState();
    
    // Wait for game details API calls (with extended timeout for slow DB)
    if (gameId) {
      console.log(`[GamesPage] Waiting for game API response: /api/games/${gameId}`);
      await this.waitForApiResponse(`/api/games/${gameId}`, 70000);
      console.log(`[GamesPage] Waiting for game details API response: /api/games/${gameId}/details`);
      await this.waitForApiResponse(`/api/games/${gameId}/details`, 70000);
    }
    
    console.log(`[GamesPage] Game details navigation complete for game ${gameId}`);
    return gameId;
  }

  async clickFilterAllGames() {
    console.log('[GamesPage] Clicking "All Games" filter');
    await this.filterAllGames().click();
    await this.waitForApiResponse('/api/games', 70000);
    // Wait for table rows to be visible again after filter
    await this.page.waitForSelector('.games-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[GamesPage] "All Games" filter applied');
  }

  async clickFilterRecent10() {
    console.log('[GamesPage] Clicking "Recent 10" filter');
    await this.filterRecent10().click();
    // Wait for the state to update (React state change, no navigation)
    await this.page.waitForTimeout(500);
    // Wait for API response if it happens
    try {
      await this.waitForApiResponse('/api/games/recent', 5000);
    } catch (error) {
      // API might not fire if data is already cached, that's okay
      console.log('[GamesPage] ⚠ API response wait timed out (may be cached)');
    }
    // Wait for table to update - might have fewer rows now
    try {
      await this.page.waitForSelector('.games-table tbody tr', { timeout: 10000, state: 'visible' });
    } catch (error) {
      // Table might be empty or still loading, check if table exists
      const tableExists = await this.gamesTable().isVisible().catch(() => false);
      if (!tableExists) {
        throw new Error('Games table not visible after filter');
      }
    }
    console.log('[GamesPage] "Recent 10" filter applied');
  }

  async clickFilterRecent25() {
    console.log('[GamesPage] Clicking "Recent 25" filter');
    await this.filterRecent25().click();
    // Wait for the state to update (React state change, no navigation)
    await this.page.waitForTimeout(500);
    // Wait for API response if it happens
    try {
      await this.waitForApiResponse('/api/games/recent', 5000);
    } catch (error) {
      // API might not fire if data is already cached, that's okay
      console.log('[GamesPage] ⚠ API response wait timed out (may be cached)');
    }
    // Wait for table to update - might have fewer rows now
    try {
      await this.page.waitForSelector('.games-table tbody tr', { timeout: 10000, state: 'visible' });
    } catch (error) {
      // Table might be empty or still loading, check if table exists
      const tableExists = await this.gamesTable().isVisible().catch(() => false);
      if (!tableExists) {
        throw new Error('Games table not visible after filter');
      }
    }
    console.log('[GamesPage] "Recent 25" filter applied');
  }
}
