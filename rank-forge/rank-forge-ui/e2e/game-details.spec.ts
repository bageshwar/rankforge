import { test, expect } from '@playwright/test';
import { GameDetailsPage } from './pages/GameDetailsPage';
import { GamesPage } from './pages/GamesPage';

test.describe('Game Details Page', () => {
  let gameId: number;

  test.beforeEach(async ({ page }) => {
    console.log('\n[TEST] ===== Starting beforeEach for game-details tests =====');
    // Navigate to games page
    console.log('[TEST] Step 1: Navigating to games page');
    const gamesPage = new GamesPage(page);
    await gamesPage.navigate();
    
    // Wait for table rows to be visible
    console.log('[TEST] Step 2: Waiting for games table rows to be visible');
    await page.waitForSelector('.games-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[TEST] ✓ Games table rows are visible');
    
    // Verify we have at least one game
    console.log('[TEST] Step 3: Verifying we have at least one game');
    const gamesCount = await gamesPage.getGamesCount();
    if (gamesCount === 0) {
      throw new Error(`Expected at least 1 game, but found ${gamesCount}`);
    }
    expect(gamesCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${gamesCount} games`);
    
    // Wait for the first game's details button to be visible and clickable
    console.log('[TEST] Step 4: Waiting for first game details button to be visible and clickable');
    const firstGameRow = await gamesPage.getGameRow(0);
    const detailsBtn = firstGameRow.locator('.details-btn');
    await expect(detailsBtn).toBeVisible({ timeout: 30000 });
    console.log('[TEST] ✓ Details button is visible');
    await expect(detailsBtn).toBeEnabled({ timeout: 30000 });
    console.log('[TEST] ✓ Details button is enabled');
    
    // Get the game ID from the href before clicking
    console.log('[TEST] Step 5: Extracting game ID from details button href');
    const href = await detailsBtn.getAttribute('href');
    if (!href) {
      throw new Error('Could not find href attribute on details button');
    }
    const match = href.match(/\/games\/(\d+)/);
    if (!match) {
      throw new Error(`Could not extract game ID from href: ${href}`);
    }
    gameId = parseInt(match[1], 10);
    console.log(`[TEST] ✓ Extracted game ID: ${gameId}`);
    
    // Click on the first game to navigate to game details page
    console.log(`[TEST] Step 6: Clicking details button for game ${gameId}`);
    await detailsBtn.click();
    console.log(`[TEST] ✓ Details button clicked`);
    
    // Wait for navigation to complete
    console.log(`[TEST] Step 7: Waiting for URL to change to /games/${gameId}`);
    await page.waitForURL(new RegExp(`.*/games/${gameId}`), { timeout: 30000 });
    console.log(`[TEST] ✓ URL changed to /games/${gameId}`);
    
    // Wait for game details API calls to complete
    console.log(`[TEST] Step 8: Waiting for game API response: /api/games/${gameId}`);
    await gamesPage.waitForApiResponse(`/api/games/${gameId}`, 70000);
    console.log(`[TEST] Step 9: Waiting for game details API response: /api/games/${gameId}/details`);
    await gamesPage.waitForApiResponse(`/api/games/${gameId}/details`, 70000);
    
    // Wait for key elements to be visible
    console.log(`[TEST] Step 10: Waiting for game title to be visible`);
    await page.waitForSelector('.game-title', { timeout: 30000 });
    console.log(`[TEST] ✓ Game title is visible`);
    console.log('[TEST] ===== beforeEach complete, ready for test =====\n');
  });

  test('should display all game details sections correctly', async ({ page }) => {
    console.log('[TEST] Starting: should display all game details sections correctly');
    const gameDetailsPage = new GameDetailsPage(page);
    // We're already on the game details page from beforeEach

    // Wait for page to be fully loaded
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.game-title', { timeout: 30000, state: 'visible' });

    // Assert game header (map name, score, date, duration, rounds)
    console.log('[TEST] Asserting game header');
    await expect(gameDetailsPage.gameTitle()).toBeVisible({ timeout: 30000 });
    const mapName = await gameDetailsPage.getMapName();
    console.log(`[TEST] Map name: ${mapName}`);
    expect(mapName).toBeTruthy();
    expect(mapName).toContain('de_');

    await expect(gameDetailsPage.headerScoreCT()).toBeVisible({ timeout: 30000 });
    await expect(gameDetailsPage.headerScoreT()).toBeVisible({ timeout: 30000 });
    const score = await gameDetailsPage.getScore();
    expect(score.ct).toBeTruthy();
    expect(score.t).toBeTruthy();
    await expect(gameDetailsPage.gameMeta()).toBeVisible({ timeout: 30000 });
    console.log('[TEST] ✓ Game header assertions passed');

    // Assert round timeline section
    console.log('[TEST] Asserting round timeline');
    await expect(gameDetailsPage.roundTimeline()).toBeVisible({ timeout: 30000 });
    // Wait for at least one round badge to be visible
    await page.waitForSelector('.round-badge', { timeout: 30000, state: 'visible' });
    const roundCount = await gameDetailsPage.getRoundCount();
    expect(roundCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${roundCount} rounds`);

    // Assert round badges have CT/T win indicators
    if (roundCount > 0) {
      const roundBadgeClass = await gameDetailsPage.getRoundBadgeClass(1);
      expect(roundBadgeClass).toMatch(/ct-win|t-win/);
      console.log('[TEST] ✓ Round badges have CT/T indicators');
    }

    // Assert player statistics table
    console.log('[TEST] Asserting player statistics table');
    await expect(gameDetailsPage.playerStatsTable()).toBeVisible({ timeout: 30000 });
    // Wait for table rows to be visible
    await page.waitForSelector('.stats-table tbody tr', { timeout: 30000, state: 'visible' });
    await expect(page.getByRole('columnheader', { name: 'Player', exact: true })).toBeVisible({ timeout: 30000 });
    await expect(page.getByRole('columnheader', { name: 'K', exact: true })).toBeVisible({ timeout: 30000 });
    await expect(page.getByRole('columnheader', { name: 'D', exact: true })).toBeVisible({ timeout: 30000 });
    await expect(page.getByRole('columnheader', { name: 'A', exact: true })).toBeVisible({ timeout: 30000 });
    await expect(page.getByRole('columnheader', { name: 'K/D', exact: true })).toBeVisible({ timeout: 30000 });

    const playerStatsCount = await gameDetailsPage.getPlayerStatsCount();
    expect(playerStatsCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${playerStatsCount} players in stats table`);

    // Assert player statistics data
    if (playerStatsCount > 0) {
      const playerStat = await gameDetailsPage.getPlayerStatData(0);
      expect(playerStat.playerName).toBeTruthy();
      expect(playerStat.kills).toBeTruthy();
      expect(playerStat.deaths).toBeTruthy();
      expect(playerStat.assists).toBeTruthy();
      expect(playerStat.kd).toBeTruthy();
      console.log('[TEST] ✓ Player statistics data is valid');
    }

    // Assert accolades section (if it exists)
    console.log('[TEST] Asserting accolades section');
    const accoladesVisible = await gameDetailsPage.accoladesSection().isVisible();
    if (accoladesVisible) {
      await expect(gameDetailsPage.accoladesGrid()).toBeVisible();
      const accoladesCount = await gameDetailsPage.getAccoladesCount();
      expect(accoladesCount).toBeGreaterThan(0);
      console.log(`[TEST] ✓ Found ${accoladesCount} accolades`);

      if (accoladesCount > 0) {
        const accolade = await gameDetailsPage.getAccoladeData(0);
        expect(accolade.type).toBeTruthy();
        expect(accolade.playerName).toBeTruthy();
        expect(accolade.value).toBeTruthy();
        console.log('[TEST] ✓ Accolade data is valid');
      }
    } else {
      console.log('[TEST] ⚠ Accolades section not visible (may not exist for this game)');
    }

    console.log('[TEST] ✓ All game details sections asserted successfully');
  });

  test('should have clickable navigation elements', async ({ page }) => {
    console.log('[TEST] Starting: should have clickable navigation elements');
    const gameDetailsPage = new GameDetailsPage(page);
    // We're already on the game details page from beforeEach

    // Test round badge click
    console.log('[TEST] Testing round badge click');
    const roundCount = await gameDetailsPage.getRoundCount();
    if (roundCount > 0) {
      await gameDetailsPage.clickRound(1);
      await expect(page).toHaveURL(new RegExp(`.*/games/${gameId}/rounds/1`));
      console.log('[TEST] ✓ Round badge click works, navigated to round details');
      
      // Navigate back to game details
      console.log('[TEST] Navigating back to game details');
      await page.goBack();
      await page.waitForURL(new RegExp(`.*/games/${gameId}`), { timeout: 30000 });
      await page.waitForSelector('.game-title', { timeout: 30000 });
      console.log('[TEST] ✓ Back navigation works');
    }

    // Test player link in stats table
    console.log('[TEST] Testing player link in stats table');
    const playerStatsCount = await gameDetailsPage.getPlayerStatsCount();
    if (playerStatsCount > 0) {
      await gameDetailsPage.clickPlayerLink(0);
      await expect(page).toHaveURL(/.*\/players\/\d+/);
      console.log('[TEST] ✓ Player link in stats table works, navigated to profile');
      
      // Navigate back to game details
      console.log('[TEST] Navigating back to game details');
      await page.goBack();
      await page.waitForURL(new RegExp(`.*/games/${gameId}`), { timeout: 30000 });
      await page.waitForSelector('.game-title', { timeout: 30000 });
      console.log('[TEST] ✓ Back navigation works');
    }

    // Test player link in accolades (if accolades exist)
    console.log('[TEST] Testing player link in accolades');
    const accoladesVisible = await gameDetailsPage.accoladesSection().isVisible();
    if (accoladesVisible) {
      const accoladesCount = await gameDetailsPage.getAccoladesCount();
      if (accoladesCount > 0) {
        await gameDetailsPage.clickAccoladePlayerLink(0);
        const currentUrl = page.url();
        if (currentUrl.includes('/players/')) {
          await expect(page).toHaveURL(/.*\/players\/\d+/);
          console.log('[TEST] ✓ Player link in accolades works, navigated to profile');
        }
      }
    } else {
      console.log('[TEST] ⚠ Accolades section not visible, skipping accolade link test');
    }

    console.log('[TEST] ✓ All navigation elements tested');
  });
});
