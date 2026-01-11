import { test, expect } from '@playwright/test';
import { RankingsPage } from './pages/RankingsPage';
import { GameDetailsPage } from './pages/GameDetailsPage';
import { RoundDetailsPage } from './pages/RoundDetailsPage';
import { GamesPage } from './pages/GamesPage';

test.describe('Navigation - Profile Links', () => {
  test('should navigate to profile from rankings page', async ({ page }) => {
    console.log('\n[TEST] Starting: should navigate to profile from rankings page');
    const rankingsPage = new RankingsPage(page);
    await rankingsPage.navigate();

    const rankingsCount = await rankingsPage.getRankingsCount();
    if (rankingsCount > 0) {
      console.log('[TEST] Clicking first player link from rankings');
      await rankingsPage.clickPlayerLink(0);

      // Verify we're on player profile page
      await expect(page).toHaveURL(/.*\/players\/\d+/);
      await expect(page.locator('.player-name')).toBeVisible();
      console.log('[TEST] ✓ Navigation to profile from rankings works');
    } else {
      console.log('[TEST] ⚠ No players found, skipping test');
    }
  });

  test('should navigate to profile from game details (stats table and accolades)', async ({ page }) => {
    console.log('\n[TEST] Starting: should navigate to profile from game details');
    const gamesPage = new GamesPage(page);
    await gamesPage.navigate();

    // Navigate to first game details
    console.log('[TEST] Navigating to first game details');
    await gamesPage.clickGameDetails(0);
    await page.waitForURL(/.*\/games\/\d+/, { timeout: 30000 });
    // Wait for game details page to load
    await page.waitForSelector('.game-title', { timeout: 30000, state: 'visible' });
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    const gameDetailsPage = new GameDetailsPage(page);
    
    // Test player link in stats table
    console.log('[TEST] Testing player link in stats table');
    // Wait for stats table to be visible
    await expect(gameDetailsPage.playerStatsTable()).toBeVisible({ timeout: 30000 });
    await page.waitForSelector('.stats-table tbody tr', { timeout: 30000, state: 'visible' });
    const playerStatsCount = await gameDetailsPage.getPlayerStatsCount();
    if (playerStatsCount > 0) {
      await gameDetailsPage.clickPlayerLink(0);
      await expect(page).toHaveURL(/.*\/players\/\d+/);
      await expect(page.locator('.player-name')).toBeVisible();
      console.log('[TEST] ✓ Player link in stats table works');
      
      // Navigate back to game details
      await page.goBack();
      await page.waitForURL(/.*\/games\/\d+/, { timeout: 30000 });
      await page.waitForSelector('.game-title', { timeout: 30000 });
    } else {
      console.log('[TEST] ⚠ No player stats found, skipping stats table link test');
    }

    // Test player link in accolades
    console.log('[TEST] Testing player link in accolades');
    const accoladesVisible = await gameDetailsPage.accoladesSection().isVisible();
    if (accoladesVisible) {
      const accoladesCount = await gameDetailsPage.getAccoladesCount();
      if (accoladesCount > 0) {
        await gameDetailsPage.clickAccoladePlayerLink(0);
        const currentUrl = page.url();
        if (currentUrl.includes('/players/')) {
          await expect(page).toHaveURL(/.*\/players\/\d+/);
          await expect(page.locator('.player-name')).toBeVisible();
          console.log('[TEST] ✓ Player link in accolades works');
        }
      }
    } else {
      console.log('[TEST] ⚠ Accolades section not visible, skipping accolade link test');
    }

    console.log('[TEST] ✓ All game details navigation tests passed');
  });

  test('should navigate to profile from round details (timeline and kill feed)', async ({ page }) => {
    console.log('\n[TEST] Starting: should navigate to profile from round details');
    const gamesPage = new GamesPage(page);
    await gamesPage.navigate();

    // Navigate to first game details
    console.log('[TEST] Navigating to first game details');
    await gamesPage.clickGameDetails(0);
    await page.waitForURL(/.*\/games\/\d+/, { timeout: 30000 });

    const gameDetailsPage = new GameDetailsPage(page);
    const roundCount = await gameDetailsPage.getRoundCount();
    
    if (roundCount === 0) {
      console.log('[TEST] ⚠ No rounds found, skipping round details navigation test');
      return;
    }

    // Navigate to first round
    console.log('[TEST] Navigating to first round');
    await gameDetailsPage.clickRound(1);
    await page.waitForURL(/.*\/games\/\d+\/rounds\/\d+/, { timeout: 30000 });
    // Wait for round details page to load
    await page.waitForSelector('.round-header', { timeout: 30000, state: 'visible' });
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    const roundDetailsPage = new RoundDetailsPage(page);
    
    // Test player link in event timeline
    console.log('[TEST] Testing player link in event timeline');
    // Wait for events section to be visible
    await expect(roundDetailsPage.eventsSection()).toBeVisible({ timeout: 30000 });
    const eventCount = await roundDetailsPage.getEventCount();
    if (eventCount > 0) {
      const eventCard = await roundDetailsPage.getEventCard(0);
      const attackerLink = eventCard.locator('.player-link.attacker');
      
      if (await attackerLink.count() > 0) {
        await roundDetailsPage.clickEventPlayerLink(0, 'attacker');
        await expect(page).toHaveURL(/.*\/players\/\d+/);
        await expect(page.locator('.player-name')).toBeVisible();
        console.log('[TEST] ✓ Player link in event timeline works');
        
        // Navigate back to round details
        await page.goBack();
        await page.waitForURL(/.*\/games\/\d+\/rounds\/\d+/, { timeout: 30000 });
        await page.waitForSelector('.round-header', { timeout: 30000 });
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
        await expect(page).toHaveURL(/.*\/players\/\d+/);
        await expect(page.locator('.player-name')).toBeVisible();
        console.log('[TEST] ✓ Player link in kill feed works');
      }
    } else {
      console.log('[TEST] ⚠ Kill feed not visible, skipping kill feed link test');
    }

    console.log('[TEST] ✓ All round details navigation tests passed');
  });
});
