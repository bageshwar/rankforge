import { test, expect } from '@playwright/test';
import { RankingsPage } from './pages/RankingsPage';

test.describe('Rankings Page', () => {
  test.beforeEach(async ({ page }) => {
    console.log('\n[TEST] ===== Starting beforeEach for rankings tests =====');
    const rankingsPage = new RankingsPage(page);
    await rankingsPage.navigate();
    console.log('[TEST] ===== beforeEach complete, ready for test =====\n');
  });

  test('should display rankings table with all columns and data', async ({ page }) => {
    console.log('[TEST] Starting: should display rankings table with all columns and data');
    const rankingsPage = new RankingsPage(page);
    // We're already on the rankings page from beforeEach

    // Verify table headers
    console.log('[TEST] Asserting table headers');
    await expect(page.getByRole('columnheader', { name: 'Rank', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Player', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'K/D', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Kills', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Deaths', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Assists', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'HS%', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Rounds', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Clutches', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Damage', exact: true })).toBeVisible();
    console.log('[TEST] ✓ All table headers are visible');

    // Get rankings count and data
    console.log('[TEST] Asserting rankings data');
    const rankingsCount = await rankingsPage.getRankingsCount();
    expect(rankingsCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${rankingsCount} players`);

    // Get first player's ranking data
    const rankingData = await rankingsPage.getRankingData(0);
    expect(rankingData.rank).toBeTruthy();
    expect(rankingData.playerName).toBeTruthy();
    expect(rankingData.kd).toBeTruthy();
    expect(rankingData.kills).toBeTruthy();
    expect(rankingData.deaths).toBeTruthy();
    expect(rankingData.assists).toBeTruthy();
    expect(rankingData.hs).toBeTruthy();
    console.log(`[TEST] ✓ First player data: ${rankingData.playerName}, Rank ${rankingData.rank}, K/D ${rankingData.kd}`);

    // Check rank icons for top 3 players
    console.log('[TEST] Asserting rank icons for top 3 players');
    if (rankingsCount >= 3) {
      const rank1Icon = await rankingsPage.getRankIcon(0);
      const rank2Icon = await rankingsPage.getRankIcon(1);
      const rank3Icon = await rankingsPage.getRankIcon(2);

      if (rank1Icon) {
        expect(['🥇', '🥈', '🥉']).toContain(rank1Icon);
      }
      if (rank2Icon) {
        expect(['🥇', '🥈', '🥉']).toContain(rank2Icon);
      }
      if (rank3Icon) {
        expect(['🥇', '🥈', '🥉']).toContain(rank3Icon);
      }
      console.log('[TEST] ✓ Rank icons are correct for top 3');
    }

    // Verify statistics for multiple players
    console.log('[TEST] Asserting statistics for multiple players');
    for (let i = 0; i < Math.min(5, rankingsCount); i++) {
      const rankingData = await rankingsPage.getRankingData(i);
      expect(rankingData.rank).toBeTruthy();
      expect(rankingData.playerName).toBeTruthy();
      expect(rankingData.kd).toBeTruthy();
    }
    console.log('[TEST] ✓ Statistics are valid for multiple players');

    console.log('[TEST] ✓ All rankings page assertions passed');
  });

  test('should filter rankings correctly', async ({ page }) => {
    console.log('[TEST] Starting: should filter rankings correctly');
    const rankingsPage = new RankingsPage(page);
    // We're already on the rankings page from beforeEach

    // Wait for initial load
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    
    const allPlayersCount = await rankingsPage.getRankingsCount();
    console.log(`[TEST] Initial players count: ${allPlayersCount}`);

    // Test Top 10 filter
    console.log('[TEST] Testing Top 10 filter');
    await rankingsPage.clickFilterTop10();
    // Wait a bit for the table to update
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    const top10Count = await rankingsPage.getRankingsCount();
    expect(top10Count).toBeLessThanOrEqual(10);
    expect(top10Count).toBeLessThanOrEqual(allPlayersCount);
    console.log(`[TEST] ✓ Top 10 filter: ${top10Count} players`);

    // Test Top 25 filter
    console.log('[TEST] Testing Top 25 filter');
    await rankingsPage.clickFilterTop25();
    // Wait a bit for the table to update
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    const top25Count = await rankingsPage.getRankingsCount();
    expect(top25Count).toBeLessThanOrEqual(25);
    expect(top25Count).toBeLessThanOrEqual(allPlayersCount);
    console.log(`[TEST] ✓ Top 25 filter: ${top25Count} players`);

    // Test All Players filter
    console.log('[TEST] Testing All Players filter');
    await rankingsPage.clickFilterAllPlayers();
    // Wait a bit for the table to update
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    const allPlayersCountAfter = await rankingsPage.getRankingsCount();
    expect(allPlayersCountAfter).toBe(allPlayersCount);
    console.log(`[TEST] ✓ All Players filter: ${allPlayersCountAfter} players (matches initial)`);

    console.log('[TEST] ✓ All filter tests passed');
  });

  test('should navigate to player profile when player link is clicked', async ({ page }) => {
    console.log('[TEST] Starting: should navigate to player profile when player link is clicked');
    const rankingsPage = new RankingsPage(page);
    // We're already on the rankings page from beforeEach

    const rankingsCount = await rankingsPage.getRankingsCount();
    if (rankingsCount > 0) {
      console.log('[TEST] Clicking first player link');
      await rankingsPage.clickPlayerLink(0);

      // Verify we're on player profile page
      await expect(page).toHaveURL(/.*\/players\/\d+/);
      await expect(page.locator('.player-name')).toBeVisible();
      console.log('[TEST] ✓ Navigation to player profile works');
    } else {
      console.log('[TEST] ⚠ No players found, skipping navigation test');
    }
  });
});
