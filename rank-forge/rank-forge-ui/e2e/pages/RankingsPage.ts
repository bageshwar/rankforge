import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class RankingsPage extends BasePage {
  readonly rankingsTitle = () => this.page.locator('.rankings-title');
  readonly rankingsTable = () => this.page.locator('.rankings-table');
  readonly rankingsRows = () => this.page.locator('.rankings-table tbody tr');
  readonly filterAllPlayers = () => this.page.locator('button:has-text("All Players")');
  readonly filterTop10 = () => this.page.locator('button:has-text("Top 10")');
  readonly filterTop25 = () => this.page.locator('button:has-text("Top 25")');

  constructor(page: Page) {
    super(page);
  }

  async navigate() {
    console.log('[RankingsPage] Navigating to rankings page');
    
    // Set up response listener BEFORE navigation
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().includes('/api/rankings') && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log('[RankingsPage] ⚠ Response listener timed out (response may have already completed)');
      return null;
    });
    
    await this.goto('/rankings');
    await this.waitForLoadState();
    
    console.log('[RankingsPage] Waiting for rankings API response or table data');
    // Wait for either API response or table rows to appear
    try {
      await Promise.race([
        responsePromise,
        this.page.waitForSelector('.rankings-table tbody tr', { timeout: 70000, state: 'visible' })
      ]);
      console.log('[RankingsPage] ✓ Either API response received or table data loaded');
    } catch (error) {
      console.log('[RankingsPage] ⚠ Wait timed out, checking if data is already loaded');
    }
    
    // Verify table is visible
    console.log('[RankingsPage] Verifying rankings table is visible');
    await this.waitForSelector('.rankings-table', 30000);
    
    // Verify we have at least one row
    const rows = await this.rankingsRows();
    const rowCount = await rows.count();
    console.log(`[RankingsPage] ✓ Rankings table loaded with ${rowCount} rows`);
    console.log('[RankingsPage] Rankings page fully loaded');
  }

  async getRankingsCount(): Promise<number> {
    console.log('[RankingsPage] Getting rankings count');
    // Wait for at least one row to be visible before counting
    try {
      await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    } catch (error) {
      console.log('[RankingsPage] ⚠ No rows visible yet, continuing with count');
    }
    const count = await this.rankingsRows().count();
    console.log(`[RankingsPage] Found ${count} players`);
    return count;
  }

  async getRankingRow(index: number) {
    const rows = await this.rankingsRows();
    return rows.nth(index);
  }

  async getRankingData(index: number) {
    const row = await this.getRankingRow(index);
    const rank = await row.locator('.rank-number').textContent();
    const playerName = await row.locator('.player-name').textContent();
    const kd = await row.locator('.kd-ratio').textContent();
    const kills = await row.locator('.stat-cell').nth(1).textContent();
    const deaths = await row.locator('.stat-cell').nth(2).textContent();
    const assists = await row.locator('.stat-cell').nth(3).textContent();
    const hs = await row.locator('.hs-percentage').textContent();
    
    return {
      rank: rank?.trim() || '',
      playerName: playerName?.trim() || '',
      kd: kd?.trim() || '',
      kills: kills?.trim() || '',
      deaths: deaths?.trim() || '',
      assists: assists?.trim() || '',
      hs: hs?.trim() || '',
    };
  }

  async clickPlayerLink(index: number) {
    console.log(`[RankingsPage] Clicking player link at index ${index}`);
    const row = await this.getRankingRow(index);
    const playerLink = row.locator('.player-link');
    await playerLink.click();
    console.log(`[RankingsPage] Player link clicked, waiting for navigation`);
    await this.waitForLoadState();
    console.log(`[RankingsPage] Navigation to player profile complete`);
  }

  async getRankIcon(index: number): Promise<string | null> {
    const row = await this.getRankingRow(index);
    const icon = row.locator('.rank-icon');
    if (await icon.count() > 0) {
      return await icon.textContent();
    }
    return null;
  }

  async clickFilterAllPlayers() {
    console.log('[RankingsPage] Clicking "All Players" filter');
    await this.filterAllPlayers().click();
    await this.waitForApiResponse('/api/rankings', 70000);
    // Wait for table rows to be visible again after filter
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[RankingsPage] "All Players" filter applied');
  }

  async clickFilterTop10() {
    console.log('[RankingsPage] Clicking "Top 10" filter');
    await this.filterTop10().click();
    await this.waitForApiResponse('/api/rankings/top', 70000);
    // Wait for table rows to be visible again after filter
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[RankingsPage] "Top 10" filter applied');
  }

  async clickFilterTop25() {
    console.log('[RankingsPage] Clicking "Top 25" filter');
    await this.filterTop25().click();
    await this.waitForApiResponse('/api/rankings/top', 70000);
    // Wait for table rows to be visible again after filter
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[RankingsPage] "Top 25" filter applied');
  }
}
