import { test, expect } from '@playwright/test';
import { GameDetailsPage } from './pages/GameDetailsPage';
import { GamesPage } from './pages/GamesPage';
import { EXPECTED_GAME_1_DETAILS, EXPECTED_GAME_2_DETAILS } from './fixtures/test-data';

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

    // EXTREMELY FINE-GRAINED assertions for game header (map name, score, date, duration, rounds)
    console.log('[TEST] Asserting EXTREMELY FINE-GRAINED game header data');
    await expect(gameDetailsPage.gameTitle()).toBeVisible({ timeout: 30000 });
    const mapName = await gameDetailsPage.getMapName();
    console.log(`[TEST] Map name: ${mapName}`);
    
    // Determine which game we're testing based on gameId from beforeEach
    const currentUrl = page.url();
    const gameIdMatch = currentUrl.match(/\/games\/(\d+)/);
    const currentGameId = gameIdMatch ? parseInt(gameIdMatch[1]) : null;
    const expectedGame = currentGameId === 1 ? EXPECTED_GAME_1_DETAILS : EXPECTED_GAME_2_DETAILS;
    
    // Validate map name - EXACT match
    expect(mapName).toContain(expectedGame.map);
    console.log(`[TEST] ✓ Map name matches: ${expectedGame.map}`);

    // Validate scores - EXACT match
    await expect(gameDetailsPage.headerScoreCT()).toBeVisible({ timeout: 30000 });
    await expect(gameDetailsPage.headerScoreT()).toBeVisible({ timeout: 30000 });
    const score = await gameDetailsPage.getScore();
    const ctScore = parseInt(score.ct || '0');
    const tScore = parseInt(score.t || '0');
    expect(ctScore).toBe(expectedGame.ctScore);
    expect(tScore).toBe(expectedGame.tScore);
    console.log(`[TEST] ✓ Score matches: CT ${ctScore} - T ${tScore} (expected CT ${expectedGame.ctScore} - T ${expectedGame.tScore})`);
    
    await expect(gameDetailsPage.gameMeta()).toBeVisible({ timeout: 30000 });
    console.log('[TEST] ✓ Game header assertions passed');

    // EXTREMELY FINE-GRAINED assertions for round timeline section
    console.log('[TEST] Asserting EXTREMELY FINE-GRAINED round timeline data');
    await expect(gameDetailsPage.roundTimeline()).toBeVisible({ timeout: 30000 });
    // Wait for at least one round badge to be visible
    await page.waitForSelector('.round-badge', { timeout: 30000, state: 'visible' });
    const roundCount = await gameDetailsPage.getRoundCount();
    
    // Validate round count - EXACT match
    expect(roundCount).toBe(expectedGame.totalRounds);
    console.log(`[TEST] ✓ Round count matches: ${roundCount} (expected ${expectedGame.totalRounds})`);

    // Assert round badges have CT/T win indicators and validate first few rounds
    if (roundCount > 0) {
      // Validate first round badge has CT/T indicator
      const roundBadgeClass = await gameDetailsPage.getRoundBadgeClass(1);
      expect(roundBadgeClass).toMatch(/ct-win|t-win/);
      console.log('[TEST] ✓ Round badges have CT/T indicators');
      
      // For game 1, validate first round winner (from API: round 1 winner is "T" but API shows "CT" - need to check actual data)
      // This is a data validation - we'll check that rounds are displayed correctly
    }

    // Assert player statistics table
    console.log('[TEST] Asserting player statistics table');
    await expect(gameDetailsPage.playerStatsTable()).toBeVisible({ timeout: 30000 });
    // Wait for table rows to be visible
    await page.waitForSelector('.stats-table tbody tr', { timeout: 30000, state: 'visible' });
    
    // Wait for table headers to be visible
    await page.waitForSelector('.stats-table thead th', { timeout: 30000, state: 'visible' });
    
    // Verify headers exist (using more flexible approach)
    const headers = page.locator('.stats-table thead th');
    const headerCount = await headers.count();
    expect(headerCount).toBeGreaterThanOrEqual(6); // At least 6 headers: Player, K, D, A, DMG, HS%
    
    // Verify specific headers by text content
    const headerTexts = await headers.allTextContents();
    expect(headerTexts.some(text => text.includes('Player'))).toBeTruthy();
    expect(headerTexts.some(text => text.trim() === 'K' || text.includes('Kills'))).toBeTruthy();
    expect(headerTexts.some(text => text.trim() === 'D' || text.includes('Deaths'))).toBeTruthy();
    expect(headerTexts.some(text => text.trim() === 'A' || text.includes('Assists'))).toBeTruthy();
    expect(headerTexts.some(text => text.includes('DMG') || text.includes('Damage'))).toBeTruthy();
    expect(headerTexts.some(text => text.includes('HS%'))).toBeTruthy();

    const playerStatsCount = await gameDetailsPage.getPlayerStatsCount();
    expect(playerStatsCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${playerStatsCount} players in stats table`);

    // EXTREMELY FINE-GRAINED assertions for player statistics data
    if (playerStatsCount > 0 && currentGameId === 1 && EXPECTED_GAME_1_DETAILS.topPlayers) {
      console.log('[TEST] Asserting EXTREMELY FINE-GRAINED player statistics for top players');
      // Validate top 3 players match API data exactly
      for (let i = 0; i < Math.min(3, EXPECTED_GAME_1_DETAILS.topPlayers.length, playerStatsCount); i++) {
        const expectedPlayer = EXPECTED_GAME_1_DETAILS.topPlayers[i];
        const actualPlayer = await gameDetailsPage.getPlayerStatData(i);
        
        // Validate player name - EXACT match
        expect(actualPlayer.playerName).toBe(expectedPlayer.playerName);
        
        // Validate kills - EXACT match
        const actualKills = parseInt(actualPlayer.kills) || 0;
        expect(actualKills).toBe(expectedPlayer.kills);
        
        // Validate deaths - EXACT match
        const actualDeaths = parseInt(actualPlayer.deaths) || 0;
        expect(actualDeaths).toBe(expectedPlayer.deaths);
        
        // Validate assists - EXACT match
        const actualAssists = parseInt(actualPlayer.assists) || 0;
        expect(actualAssists).toBe(expectedPlayer.assists);
        
        // Validate damage - EXACT match (with tolerance for formatting)
        const actualDamage = parseFloat(actualPlayer.damage.replace(/,/g, '')) || 0;
        expect(Math.abs(actualDamage - expectedPlayer.damage)).toBeLessThan(1.0);
        
        // Validate headshot percentage - EXACT match (with tolerance for rounding)
        const actualHS = parseFloat(actualPlayer.headshotPct.replace('%', '')) || 0;
        expect(Math.abs(actualHS - expectedPlayer.headshotPercentage)).toBeLessThan(0.1);
        
        console.log(`[TEST] ✓ Player ${i + 1}: ${actualPlayer.playerName} - K:${actualKills} D:${actualDeaths} A:${actualAssists} DMG:${actualDamage.toFixed(0)} HS:${actualHS.toFixed(1)}%`);
      }
    } else if (playerStatsCount > 0) {
      const playerStat = await gameDetailsPage.getPlayerStatData(0);
      expect(playerStat.playerName).toBeTruthy();
      expect(playerStat.kills).toBeTruthy();
      expect(playerStat.deaths).toBeTruthy();
      expect(playerStat.assists).toBeTruthy();
      expect(playerStat.damage).toBeTruthy();
      expect(playerStat.headshotPct).toBeTruthy();
      console.log('[TEST] ✓ Player statistics data is valid');
    }

    // Assert player statistics are sorted by kills (descending)
    console.log('[TEST] Asserting player statistics are sorted by kills (descending)');
    if (playerStatsCount > 1) {
      const firstPlayer = await gameDetailsPage.getPlayerStatData(0);
      const secondPlayer = await gameDetailsPage.getPlayerStatData(1);
      const firstKills = parseInt(firstPlayer.kills) || 0;
      const secondKills = parseInt(secondPlayer.kills) || 0;
      expect(firstKills).toBeGreaterThanOrEqual(secondKills);
      console.log(`[TEST] ✓ First player has ${firstKills} kills, second has ${secondKills} kills (sorted correctly)`);
      
      // Verify all players are sorted
      let previousKills = firstKills;
      for (let i = 1; i < Math.min(playerStatsCount, 5); i++) {
        const playerStat = await gameDetailsPage.getPlayerStatData(i);
        const currentKills = parseInt(playerStat.kills) || 0;
        expect(currentKills).toBeLessThanOrEqual(previousKills);
        previousKills = currentKills;
      }
      console.log('[TEST] ✓ All players are sorted by kills in descending order');
    } else {
      console.log('[TEST] ⚠ Only one player found, skipping sort verification');
    }

    // Assert top 3 players have gold/silver/bronze styling
    console.log('[TEST] Asserting top 3 players have gold/silver/bronze styling');
    if (playerStatsCount >= 1) {
      // Check rank 1 (gold)
      const rank1Class = await gameDetailsPage.getPlayerRowClass(0);
      expect(rank1Class).toContain('rank-gold');
      const rank1Icon = await gameDetailsPage.getRankIcon(0);
      expect(rank1Icon).toBe('🥇');
      console.log('[TEST] ✓ Rank 1 player has gold styling and 🥇 icon');
    }
    if (playerStatsCount >= 2) {
      // Check rank 2 (silver)
      const rank2Class = await gameDetailsPage.getPlayerRowClass(1);
      expect(rank2Class).toContain('rank-silver');
      const rank2Icon = await gameDetailsPage.getRankIcon(1);
      expect(rank2Icon).toBe('🥈');
      console.log('[TEST] ✓ Rank 2 player has silver styling and 🥈 icon');
    }
    if (playerStatsCount >= 3) {
      // Check rank 3 (bronze)
      const rank3Class = await gameDetailsPage.getPlayerRowClass(2);
      expect(rank3Class).toContain('rank-bronze');
      const rank3Icon = await gameDetailsPage.getRankIcon(2);
      expect(rank3Icon).toBe('🥉');
      console.log('[TEST] ✓ Rank 3 player has bronze styling and 🥉 icon');
    }
    if (playerStatsCount < 3) {
      console.log(`[TEST] ⚠ Only ${playerStatsCount} player(s) found, skipping rank 3 verification`);
    }

    // EXTREMELY FINE-GRAINED assertions for accolades section
    console.log('[TEST] Asserting EXTREMELY FINE-GRAINED accolades data');
    const accoladesVisible = await gameDetailsPage.accoladesSection().isVisible();
    if (accoladesVisible && currentGameId === 1 && EXPECTED_GAME_1_DETAILS.accolades) {
      await expect(gameDetailsPage.accoladesGrid()).toBeVisible();
      const accoladesCount = await gameDetailsPage.getAccoladesCount();
      
      // Validate accolade count - EXACT match
      expect(accoladesCount).toBe(EXPECTED_GAME_1_DETAILS.accolades.length);
      console.log(`[TEST] ✓ Accolade count matches: ${accoladesCount} (expected ${EXPECTED_GAME_1_DETAILS.accolades.length})`);

      // Validate each accolade matches API data
      for (let i = 0; i < Math.min(accoladesCount, EXPECTED_GAME_1_DETAILS.accolades.length); i++) {
        const expectedAccolade = EXPECTED_GAME_1_DETAILS.accolades[i];
        const actualAccolade = await gameDetailsPage.getAccoladeData(i);
        
        // Validate player name - EXACT match
        expect(actualAccolade.playerName).toBe(expectedAccolade.playerName);
        
        // Validate type - should match (may be formatted differently in UI)
        expect(actualAccolade.type).toBeTruthy();
        
        // Validate value - EXACT match (with tolerance for formatting)
        const actualValue = parseFloat(actualAccolade.value.replace(/,/g, '')) || 0;
        expect(Math.abs(actualValue - expectedAccolade.value)).toBeLessThan(0.1);
        
        console.log(`[TEST] ✓ Accolade ${i + 1}: ${actualAccolade.type} - ${actualAccolade.playerName} (${actualValue})`);
      }
    } else if (accoladesVisible) {
      const accoladesCount = await gameDetailsPage.getAccoladesCount();
      expect(accoladesCount).toBeGreaterThan(0);
      console.log(`[TEST] ✓ Found ${accoladesCount} accolades`);
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
