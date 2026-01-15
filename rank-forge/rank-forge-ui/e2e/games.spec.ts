import { test, expect } from '@playwright/test';
import { GamesPage } from './pages/GamesPage';
import { EXPECTED_GAMES } from './fixtures/test-data';

test.describe('Games Page', () => {
  test.beforeEach(async ({ page }) => {
    console.log('\n[TEST] ===== Starting beforeEach for games tests =====');
    const gamesPage = new GamesPage(page);
    await gamesPage.navigate();
    console.log('[TEST] ===== beforeEach complete, ready for test =====\n');
  });

  test('should display games page with 2 games and correct structure', async ({ page }) => {
    console.log('[TEST] Starting: should display games page with 2 games and correct structure');
    const gamesPage = new GamesPage(page);
    // We're already on the games page from beforeEach

    // Assert exact 2 games are visible
    console.log('[TEST] Asserting exactly 2 games are visible');
    const gamesCount = await gamesPage.getGamesCount();
    expect(gamesCount).toBe(EXPECTED_GAMES.length);
    console.log(`[TEST] ✓ Found exactly ${gamesCount} games (expected ${EXPECTED_GAMES.length})`);

    // Verify table structure
    console.log('[TEST] Asserting table structure');
    await expect(page.getByRole('columnheader', { name: 'Date & Time', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Map', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Score', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Duration', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Actions', exact: true })).toBeVisible();
    console.log('[TEST] ✓ Table headers are correct');

    // Fine-grained assertions for each game based on API data
    console.log('[TEST] Asserting fine-grained game data for all games');
    for (let i = 0; i < Math.min(EXPECTED_GAMES.length, gamesCount); i++) {
      const expectedGame = EXPECTED_GAMES[i];
      const actualGameData = await gamesPage.getGameData(i);
      
      // Validate map name
      expect(actualGameData.map).toBe(expectedGame.map);
      
      // Validate score
      expect(actualGameData.score).toBe(expectedGame.score);
      
      // Validate duration (formatted as "X min")
      expect(actualGameData.duration).toContain('min');
      const durationMatch = actualGameData.duration.match(/(\d+)/);
      if (durationMatch) {
        const durationMinutes = parseInt(durationMatch[1]);
        expect(durationMinutes).toBe(parseInt(expectedGame.duration));
      }
      
      // Validate date is present (format may vary)
      expect(actualGameData.date).toBeTruthy();
      
      console.log(`[TEST] ✓ Game ${i + 1}: ${actualGameData.map} - ${actualGameData.score} (${actualGameData.duration})`);
    }

    // Verify games are sorted by date (most recent first)
    console.log('[TEST] Asserting games are sorted by date (most recent first)');
    if (gamesCount >= 2) {
      const game1Data = await gamesPage.getGameData(0);
      const game2Data = await gamesPage.getGameData(1);
      // Game 2 (de_ancient) should be first as it's more recent
      expect(game1Data.map).toBe('de_ancient');
      expect(game2Data.map).toBe('de_anubis');
      console.log('[TEST] ✓ Games are sorted correctly by date');
    }

    console.log('[TEST] ✓ All games page assertions passed');
  });

  test('should navigate to game details when details link is clicked', async ({ page }) => {
    console.log('[TEST] Starting: should navigate to game details when details link is clicked');
    const gamesPage = new GamesPage(page);
    // We're already on the games page from beforeEach

    // Click first game details link
    console.log('[TEST] Clicking first game details link');
    await gamesPage.clickGameDetails(0);

    // Verify we're on the game details page
    await expect(page).toHaveURL(/.*\/games\/\d+/);
    await expect(page.locator('.game-title')).toBeVisible();
    console.log('[TEST] ✓ Navigation to game details works');
  });

  test('should filter games correctly', async ({ page }) => {
    console.log('[TEST] Starting: should filter games correctly');
    const gamesPage = new GamesPage(page);
    // We're already on the games page from beforeEach

    const allGamesCount = await gamesPage.getGamesCount();
    console.log(`[TEST] Initial games count: ${allGamesCount}`);

    // Test Recent 10 filter
    console.log('[TEST] Testing Recent 10 filter');
    await gamesPage.clickFilterRecent10();
    const recent10Count = await gamesPage.getGamesCount();
    expect(recent10Count).toBeLessThanOrEqual(10);
    expect(recent10Count).toBeLessThanOrEqual(allGamesCount);
    console.log(`[TEST] ✓ Recent 10 filter: ${recent10Count} games`);

    // Test Recent 25 filter
    console.log('[TEST] Testing Recent 25 filter');
    await gamesPage.clickFilterRecent25();
    const recent25Count = await gamesPage.getGamesCount();
    expect(recent25Count).toBeLessThanOrEqual(25);
    expect(recent25Count).toBeLessThanOrEqual(allGamesCount);
    console.log(`[TEST] ✓ Recent 25 filter: ${recent25Count} games`);

    // Test All Games filter
    console.log('[TEST] Testing All Games filter');
    await gamesPage.clickFilterAllGames();
    const allGamesCountAfter = await gamesPage.getGamesCount();
    expect(allGamesCountAfter).toBe(allGamesCount);
    console.log(`[TEST] ✓ All Games filter: ${allGamesCountAfter} games (matches initial)`);

    console.log('[TEST] ✓ All filter tests passed');
  });
});
