import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class RankingsPage extends BasePage {
  readonly rankingsTitle = () => this.page.locator('.rankings-title');
  readonly rankingsTable = () => this.page.locator('.rankings-table');
  readonly rankingsRows = () => this.page.locator('.rankings-table tbody tr');
  readonly filterAllPlayers = () => this.page.getByRole('link', { name: 'All Players', exact: true });
  readonly filterTop10 = () => this.page.getByRole('link', { name: 'Top 10', exact: true });
  readonly filterTop25 = () => this.page.getByRole('link', { name: 'Top 25', exact: true });

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
    // Check if page is closed first
    if (this.page.isClosed()) {
      console.log('[RankingsPage] ⚠ Page is closed, returning 0');
      return 0;
    }
    
    // Wait for rankings table to be visible
    try {
      await this.page.waitForSelector('.rankings-table', { timeout: 30000, state: 'visible' });
    } catch (error) {
      console.log('[RankingsPage] ⚠ Rankings table not visible, continuing with count');
    }
    
    // Check again if page is still open
    if (this.page.isClosed()) {
      console.log('[RankingsPage] ⚠ Page closed while waiting, returning 0');
      return 0;
    }
    
    // Wait for at least one row to be visible before counting
    try {
      await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 10000, state: 'visible' });
    } catch (error) {
      console.log('[RankingsPage] ⚠ No rows visible yet, continuing with count');
    }
    
    // Final check if page is still open before counting
    if (this.page.isClosed()) {
      console.log('[RankingsPage] ⚠ Page closed before counting, returning 0');
      return 0;
    }
    
    try {
      const count = await this.rankingsRows().count({ timeout: 5000 });
      console.log(`[RankingsPage] Found ${count} players`);
      return count;
    } catch (error) {
      console.log('[RankingsPage] ⚠ Error getting count, returning 0');
      return 0;
    }
  }

  async getRankingRow(index: number) {
    const rows = await this.rankingsRows();
    const row = rows.nth(index);
    // Scroll row into view to ensure it's accessible
    await row.scrollIntoViewIfNeeded();
    return row;
  }

  async getRankingData(index: number) {
    const row = await this.getRankingRow(index);
    // Wait for row to be visible
    await row.waitFor({ state: 'visible', timeout: 5000 });
    
    // Rank is in: <td class="rank-cell"><span class="rank-number">1</span>...</td>
    // Get the rank number directly from the span inside rank-cell
    const rankCell = row.locator('td.rank-cell').first();
    
    // Wait for rank cell to be visible
    await rankCell.waitFor({ state: 'visible', timeout: 5000 });
    await rankCell.scrollIntoViewIfNeeded();
    
    const rankNumberSpan = rankCell.locator('span.rank-number').first();
    
    // Try innerText first (more reliable)
    let rank = '';
    
    // Check if rank-number span exists
    const spanCount = await rankNumberSpan.count();
    if (spanCount === 0) {
      // Try getting from rank-cell directly
      const cellText = await rankCell.innerText({ timeout: 2000 });
      if (cellText) {
        // Remove emoji and any icon text, keep only the number
        rank = cellText.replace(/[🥇🥈🥉]/g, '').replace(/\D/g, '').trim();
      }
      if (!rank) {
        throw new Error(`Rank cell found but no rank number span and no text in cell for row ${index}`);
      }
    } else {
      try {
        rank = await rankNumberSpan.innerText({ timeout: 2000 });
        if (rank) {
          rank = rank.replace(/[🥇🥈🥉]/g, '').trim();
        }
      } catch (error) {
        // Fall back to textContent
        rank = await this.safeTextContent(rankNumberSpan, 2000);
        if (rank) {
          rank = rank.replace(/[🥇🥈🥉]/g, '').trim();
        }
      }
      
      // If still no rank found, try getting from rank-cell directly
      if (!rank) {
        try {
          const cellText = await rankCell.innerText({ timeout: 2000 });
          if (cellText) {
            // Remove emoji and any icon text, keep only the number
            rank = cellText.replace(/[🥇🥈🥉]/g, '').replace(/\D/g, '').trim();
          }
        } catch (error) {
          const cellText = await this.safeTextContent(rankCell, 2000);
          if (cellText) {
            rank = cellText.replace(/[🥇🥈🥉]/g, '').replace(/\D/g, '').trim();
          }
        }
      }
      
      // If rank is still empty, throw error with debug info
      if (!rank) {
        const cellHtml = await rankCell.innerHTML().catch(() => '');
        const spanHtml = await rankNumberSpan.innerHTML().catch(() => '');
        throw new Error(`Could not extract rank from row ${index}. Cell HTML: ${cellHtml.substring(0, 100)}, Span HTML: ${spanHtml.substring(0, 100)}`);
      }
    }
    
    const playerName = await this.safeTextContent(row.locator('.player-cell .player-name'));
    // Stats are in order: ELO (index 0), K/D (index 1), Kills (index 2), Deaths (index 3), Assists (index 4), HS% (index 5), Rounds (index 6), Games (index 7), Clutches (index 8), Damage (index 9)
    const elo = await this.safeTextContent(row.locator('.stat-cell').nth(0));
    const kd = await this.safeTextContent(row.locator('.stat-cell.kd-ratio'));
    const kills = await this.safeTextContent(row.locator('.stat-cell').nth(2));
    const deaths = await this.safeTextContent(row.locator('.stat-cell').nth(3));
    const assists = await this.safeTextContent(row.locator('.stat-cell').nth(4));
    const hs = await this.safeTextContent(row.locator('.stat-cell.hs-percentage'));
    const rounds = await this.safeTextContent(row.locator('.stat-cell').nth(6));
    const games = await this.safeTextContent(row.locator('.stat-cell').nth(7));
    const clutches = await this.safeTextContent(row.locator('.stat-cell').nth(8));
    // Damage is at index 9, and has class "damage"
    let damage = await this.safeTextContent(row.locator('.stat-cell.damage'));
    if (!damage) {
      damage = await this.safeTextContent(row.locator('.stat-cell').nth(9));
    }
    
    return {
      rank: rank || '',
      playerName: playerName?.trim() || '',
      elo: elo?.trim() || '',
      kd: kd?.trim() || '',
      kills: kills?.trim() || '',
      deaths: deaths?.trim() || '',
      assists: assists?.trim() || '',
      hs: hs?.trim() || '',
      rounds: rounds?.trim() || '',
      clutches: clutches?.trim() || '',
      damage: damage?.trim() || '',
    };
  }

  async clickPlayerLink(index: number) {
    console.log(`[RankingsPage] Clicking player link at index ${index}`);
    const row = await this.getRankingRow(index);
    const playerLink = row.locator('.player-cell .player-link');
    await playerLink.click();
    console.log(`[RankingsPage] Player link clicked, waiting for navigation`);
    await this.waitForLoadState();
    console.log(`[RankingsPage] Navigation to player profile complete`);
  }

  async getRankIcon(index: number): Promise<string | null> {
    const row = await this.getRankingRow(index);
    const icon = row.locator('.rank-cell .rank-icon');
    if (await icon.count() > 0) {
      return await icon.textContent();
    }
    return null;
  }
  
  async getHeaderStats(): Promise<{ totalGames: string | null; totalRounds: string | null; totalPlayers: string | null }> {
    // Look for header stats in various possible locations
    const headerSelectors = [
      '.rankings-header',
      '.page-header',
      '.stats-summary',
      '.summary-stats',
      '[data-testid="rankings-header-stats"]'
    ];
    
    for (const selector of headerSelectors) {
      const header = this.page.locator(selector);
      if (await header.count() > 0) {
        const text = await header.textContent();
        if (text) {
          // Extract numbers from text
          const gamesMatch = text.match(/(\d+)\s*games?/i);
          const roundsMatch = text.match(/(\d+)\s*rounds?/i);
          const playersMatch = text.match(/(\d+)\s*players?/i);
          
          return {
            totalGames: gamesMatch ? gamesMatch[1] : null,
            totalRounds: roundsMatch ? roundsMatch[1] : null,
            totalPlayers: playersMatch ? playersMatch[1] : null,
          };
        }
      }
    }
    
    return { totalGames: null, totalRounds: null, totalPlayers: null };
  }

  async clickFilterAllPlayers() {
    console.log('[RankingsPage] Clicking "All Players" filter');
    const filterButton = this.filterAllPlayers();
    await filterButton.waitFor({ state: 'visible', timeout: 30000 });
    
    // Filter links use preventDefault, so they don't navigate - just update state
    // Wait for URL to potentially change (might update via navigate)
    const urlChanged = this.page.waitForURL(/.*\/rankings(?!.*limit).*/, { timeout: 5000 }).catch(() => 
      this.page.waitForURL(/.*\/rankings$/, { timeout: 5000 }).catch(() => null)
    );
    await filterButton.click();
    await urlChanged; // Wait for URL change if it happens, but don't fail if it doesn't
    
    // Wait for React state to update and table to reload
    await this.page.waitForTimeout(1000);
    await this.waitForLoadState();
    
    // Wait for table to be visible and scroll it into view
    await this.page.waitForSelector('.rankings-table', { timeout: 30000, state: 'visible' });
    const table = this.rankingsTable();
    await table.scrollIntoViewIfNeeded();
    
    // Wait for table rows to be visible
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    
    console.log('[RankingsPage] "All Players" filter applied');
  }

  async clickFilterTop10() {
    console.log('[RankingsPage] Clicking "Top 10" filter');
    const filterButton = this.filterTop10();
    await filterButton.waitFor({ state: 'visible', timeout: 30000 });
    
    // Wait for URL to update (React Router client-side navigation)
    const urlChanged = this.page.waitForURL(/.*\/rankings.*limit=10.*/, { timeout: 30000 }).catch(() => null);
    await filterButton.click();
    await urlChanged; // Wait for URL change
    
    await this.waitForLoadState(); // Wait for network idle
    
    // Wait for table rows to be visible again after filter (might be fewer rows)
    try {
      await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    } catch (error) {
      console.log('[RankingsPage] ⚠ Table rows wait timed out, checking if table exists');
      const tableExists = await this.rankingsTable().isVisible();
      if (!tableExists) {
        throw new Error('Rankings table not visible after filter');
      }
    }
    
    console.log('[RankingsPage] "Top 10" filter applied');
  }

  async clickFilterTop25() {
    console.log('[RankingsPage] Clicking "Top 25" filter');
    const filterButton = this.filterTop25();
    await filterButton.waitFor({ state: 'visible', timeout: 30000 });
    
    // Filter links use preventDefault, so they don't navigate - just update state
    // Wait for URL to potentially change (might update via navigate)
    const urlChanged = this.page.waitForURL(/.*\/rankings.*limit=25.*/, { timeout: 5000 }).catch(() => null);
    await filterButton.click();
    await urlChanged; // Wait for URL change if it happens, but don't fail if it doesn't
    
    // Wait for React state to update and table to reload
    await this.page.waitForTimeout(1000);
    await this.waitForLoadState();
    
    // Wait for table to be visible and scroll it into view
    await this.page.waitForSelector('.rankings-table', { timeout: 30000, state: 'visible' });
    const table = this.rankingsTable();
    await table.scrollIntoViewIfNeeded();
    
    // Wait for table rows to be visible
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    
    console.log('[RankingsPage] "Top 25" filter applied');
  }

  async clickMonthlyTab() {
    console.log('[RankingsPage] Clicking Monthly tab');
    await this.page.getByRole('link', { name: 'Monthly' }).click();
    await this.waitForApiResponse('/api/rankings/leaderboard/monthly', 70000);
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[RankingsPage] Monthly tab clicked');
  }

  async clickAllTimeTab() {
    console.log('[RankingsPage] Clicking All Time tab');
    await this.page.getByRole('link', { name: 'All Time' }).click();
    await this.waitForApiResponse('/api/rankings', 70000);
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[RankingsPage] All Time tab clicked');
  }

  async selectMonth(month: number) {
    console.log(`[RankingsPage] Selecting month ${month}`);
    await this.page.locator('#month-select').selectOption(month.toString());
    await this.waitForApiResponse('/api/rankings/leaderboard/monthly', 70000);
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log(`[RankingsPage] Month ${month} selected`);
  }

  async selectYear(year: number) {
    console.log(`[RankingsPage] Selecting year ${year}`);
    await this.page.locator('#year-select').selectOption(year.toString());
    await this.waitForApiResponse('/api/rankings/leaderboard/monthly', 70000);
    await this.page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log(`[RankingsPage] Year ${year} selected`);
  }
}
