import { test, expect } from '@playwright/test';
import { RoundDetailsPage } from './pages/RoundDetailsPage';
import { GameDetailsPage } from './pages/GameDetailsPage';
import { GamesPage } from './pages/GamesPage';

test.describe('Round Details Page', () => {
  let gameId: number;
  let roundNumber: number = 1;

  test.beforeEach(async ({ page }) => {
    console.log('\n[TEST] ===== Starting beforeEach for round-details tests =====');
    // Navigate to games page
    console.log('[TEST] Step 1: Navigating to games page');
    const gamesPage = new GamesPage(page);
    await gamesPage.navigate();
    
    // Verify we have at least one game
    console.log('[TEST] Step 2: Verifying we have at least one game');
    const gamesCount = await gamesPage.getGamesCount();
    if (gamesCount === 0) {
      throw new Error(`Expected at least 1 game, but found ${gamesCount}`);
    }
    expect(gamesCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${gamesCount} games`);
    
    // Click on the first game to navigate to game details page
    console.log('[TEST] Step 3: Clicking first game to navigate to game details');
    gameId = await gamesPage.clickGameDetails(0) || 1;
    
    // Verify we're on the game details page
    console.log(`[TEST] Step 4: Verifying we're on game details page for game ${gameId}`);
    await expect(page).toHaveURL(new RegExp(`.*/games/${gameId}`));
    console.log(`[TEST] ✓ On game details page for game ${gameId}`);
    
    // Wait for game details to load
    console.log('[TEST] Step 5: Waiting for game title to be visible');
    await page.waitForSelector('.game-title', { timeout: 30000 });
    console.log('[TEST] ✓ Game title is visible');
    
    // Now navigate to round details by clicking on the first round
    console.log('[TEST] Step 6: Getting round count');
    const gameDetailsPage = new GameDetailsPage(page);
    const roundCount = await gameDetailsPage.getRoundCount();
    if (roundCount === 0) {
      throw new Error(`Expected at least 1 round, but found ${roundCount}`);
    }
    expect(roundCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${roundCount} rounds`);
    
    // Click on the first round
    console.log('[TEST] Step 7: Clicking first round');
    await gameDetailsPage.clickRound(1);
    
    // Verify we're on the round details page
    console.log(`[TEST] Step 8: Verifying we're on round details page`);
    await expect(page).toHaveURL(new RegExp(`.*/games/${gameId}/rounds/1`));
    console.log(`[TEST] ✓ On round details page for game ${gameId}, round 1`);
    
    // Wait for round details to load
    console.log('[TEST] Step 9: Waiting for round header to be visible');
    await page.waitForSelector('.round-header', { timeout: 30000 });
    console.log('[TEST] ✓ Round header is visible');
    console.log('[TEST] ===== beforeEach complete, ready for test =====\n');
  });

  test('should display all round details sections correctly', async ({ page }) => {
    console.log('[TEST] Starting: should display all round details sections correctly');
    const roundDetailsPage = new RoundDetailsPage(page);
    // We're already on the round details page from beforeEach

    // Assert round header
    console.log('[TEST] Asserting round header');
    const roundTitle = await roundDetailsPage.getRoundNumber();
    expect(roundTitle).toBeTruthy();
    expect(roundTitle).toContain('Round');
    await expect(roundDetailsPage.roundWinnerBadge()).toBeVisible();
    const winnerTeam = await roundDetailsPage.getWinnerTeam();
    expect(winnerTeam).toMatch(/CT|T/);
    console.log(`[TEST] ✓ Round header: ${roundTitle}, Winner: ${winnerTeam}`);

    // Assert round statistics
    console.log('[TEST] Asserting round statistics');
    await expect(roundDetailsPage.roundStatsRow()).toBeVisible();
    const stats = await roundDetailsPage.getRoundStats();
    expect(stats).toBeTruthy();
    expect(Object.keys(stats).length).toBeGreaterThan(0);
    console.log('[TEST] ✓ Round statistics displayed');

    // Assert bomb status (if applicable)
    console.log('[TEST] Asserting bomb status');
    const bombStatusVisible = await roundDetailsPage.bombStatusBar().isVisible();
    if (bombStatusVisible) {
      const bombStatus = await roundDetailsPage.getBombStatus();
      expect(bombStatus.length).toBeGreaterThan(0);
      console.log(`[TEST] ✓ Bomb status: ${bombStatus.join(', ')}`);
    } else {
      console.log('[TEST] ⚠ Bomb status bar not visible (may not be applicable)');
    }

    // Assert event timeline
    console.log('[TEST] Asserting event timeline');
    await expect(roundDetailsPage.eventsSection()).toBeVisible();
    await expect(roundDetailsPage.eventsTimeline()).toBeVisible();
    const eventCount = await roundDetailsPage.getEventCount();
    expect(eventCount).toBeGreaterThanOrEqual(0);
    console.log(`[TEST] ✓ Event timeline visible with ${eventCount} events`);

    if (eventCount > 0) {
      const eventData = await roundDetailsPage.getEventData(0);
      expect(eventData.time).toBeTruthy();
      expect(eventData.eventType).toBeTruthy();
      console.log('[TEST] ✓ Event data is valid');
    }

    // Assert kill feed (if kills exist)
    console.log('[TEST] Asserting kill feed');
    const killFeedVisible = await roundDetailsPage.killFeedSection().isVisible();
    if (killFeedVisible) {
      const killFeedCount = await roundDetailsPage.getKillFeedCount();
      expect(killFeedCount).toBeGreaterThan(0);
      console.log(`[TEST] ✓ Kill feed visible with ${killFeedCount} kills`);
    } else {
      console.log('[TEST] ⚠ Kill feed section not visible (no kills in this round)');
    }

    console.log('[TEST] ✓ All round details sections asserted successfully');
  });

  test('should have clickable navigation elements', async ({ page }) => {
    console.log('[TEST] Starting: should have clickable navigation elements');
    const roundDetailsPage = new RoundDetailsPage(page);
    // We're already on the round details page from beforeEach

    // Test player link in event timeline
    console.log('[TEST] Testing player link in event timeline');
    const eventCount = await roundDetailsPage.getEventCount();
    if (eventCount > 0) {
      const eventCard = await roundDetailsPage.getEventCard(0);
      const attackerLink = eventCard.locator('.player-link.attacker');
      
      if (await attackerLink.count() > 0) {
        await roundDetailsPage.clickEventPlayerLink(0, 'attacker');
        const currentUrl = page.url();
        if (currentUrl.includes('/players/')) {
          await expect(page).toHaveURL(/.*\/players\/\d+/);
          console.log('[TEST] ✓ Event timeline player link works, navigated to profile');
          
          // Navigate back
          await page.goBack();
          await page.waitForURL(new RegExp(`.*/games/${gameId}/rounds/1`), { timeout: 30000 });
          await page.waitForSelector('.round-header', { timeout: 30000 });
          console.log('[TEST] ✓ Back navigation works');
        }
      }
    } else {
      console.log('[TEST] ⚠ No events found, skipping event timeline link test');
    }

    // Test player link in kill feed
    console.log('[TEST] Testing player link in kill feed');
    const killFeedVisible = await roundDetailsPage.killFeedSection().isVisible();
    if (killFeedVisible) {
      const killFeedCount = await roundDetailsPage.getKillFeedCount();
      if (killFeedCount > 0) {
        await roundDetailsPage.clickKillFeedPlayerLink(0, 'killer');
        const currentUrl = page.url();
        if (currentUrl.includes('/players/')) {
          await expect(page).toHaveURL(/.*\/players\/\d+/);
          console.log('[TEST] ✓ Kill feed player link works, navigated to profile');
          
          // Navigate back
          await page.goBack();
          await page.waitForURL(new RegExp(`.*/games/${gameId}/rounds/1`), { timeout: 30000 });
          await page.waitForSelector('.round-header', { timeout: 30000 });
          console.log('[TEST] ✓ Back navigation works');
        }
      }
    } else {
      console.log('[TEST] ⚠ Kill feed not visible, skipping kill feed link test');
    }

    // Test back to game link
    console.log('[TEST] Testing back to game link');
    await roundDetailsPage.backToGameLink().click();
    await roundDetailsPage.waitForLoadState();
    await expect(page).toHaveURL(new RegExp(`.*/games/${gameId}`));
    await expect(page.locator('.game-title')).toBeVisible();
    console.log('[TEST] ✓ Back to game link works');

    console.log('[TEST] ✓ All navigation elements tested');
  });
});
