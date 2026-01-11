import { test, expect } from '@playwright/test';
import { GamesPage } from './pages/GamesPage';

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

    // Assert 2 games are visible
    console.log('[TEST] Asserting 2 games are visible');
    const gamesCount = await gamesPage.getGamesCount();
    expect(gamesCount).toBeGreaterThanOrEqual(2);
    console.log(`[TEST] ✓ Found ${gamesCount} games (expected at least 2)`);

    // Verify table structure
    console.log('[TEST] Asserting table structure');
    await expect(page.getByRole('columnheader', { name: 'Date & Time', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Map', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Score', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Duration', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Actions', exact: true })).toBeVisible();
    console.log('[TEST] ✓ Table headers are correct');

    // Verify game data in rows
    console.log('[TEST] Asserting game data in table rows');
    const gameData = await gamesPage.getGameData(0);
    expect(gameData.date).toBeTruthy();
    expect(gameData.map).toBeTruthy();
    expect(gameData.score).toBeTruthy();
    expect(gameData.duration).toBeTruthy();
    console.log(`[TEST] ✓ Game data is valid: ${gameData.map}, ${gameData.score}`);

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
