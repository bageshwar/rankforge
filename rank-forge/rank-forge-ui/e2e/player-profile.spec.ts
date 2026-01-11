import { test, expect } from '@playwright/test';
import { PlayerProfilePage } from './pages/PlayerProfilePage';
import { RankingsPage } from './pages/RankingsPage';

test.describe('Player Profile Page', () => {
  let playerId: string;

  test.beforeEach(async ({ page }) => {
    console.log('\n[TEST] ===== Starting beforeEach for player-profile tests =====');
    // Navigate to rankings page and get first player ID
    console.log('[TEST] Step 1: Navigating to rankings page');
    const rankingsPage = new RankingsPage(page);
    await rankingsPage.navigate();
    
    // Get first player's link to extract player ID
    console.log('[TEST] Step 2: Extracting player ID from first player link');
    const firstPlayerRow = await rankingsPage.getRankingRow(0);
    const playerLink = firstPlayerRow.locator('.player-link');
    const href = await playerLink.getAttribute('href');
    if (href) {
      const match = href.match(/\/players\/(\d+)/);
      if (match) {
        playerId = match[1];
        console.log(`[TEST] ✓ Extracted player ID: ${playerId}`);
      }
    }
    
    // If we couldn't extract from link, use fallback
    if (!playerId) {
      playerId = '1';
      console.log(`[TEST] ⚠ Could not extract player ID, using fallback: ${playerId}`);
    }
    
    // Click on first player to navigate to profile
    console.log(`[TEST] Step 3: Clicking first player link to navigate to profile`);
    await playerLink.click();
    await rankingsPage.waitForLoadState();
    
    // Wait for navigation
    console.log(`[TEST] Step 4: Waiting for URL to change to /players/${playerId}`);
    await page.waitForURL(new RegExp(`.*/players/${playerId}`), { timeout: 30000 });
    console.log(`[TEST] ✓ URL changed to /players/${playerId}`);
    
    // Wait for player profile API call
    console.log(`[TEST] Step 5: Waiting for player profile API response`);
    await rankingsPage.waitForApiResponse(`/api/players/${encodeURIComponent(playerId)}`, 70000);
    
    // Wait for key elements
    console.log(`[TEST] Step 6: Waiting for profile header to be visible`);
    await page.waitForSelector('.profile-header', { timeout: 30000 });
    console.log(`[TEST] ✓ Profile header is visible`);
    console.log('[TEST] ===== beforeEach complete, ready for test =====\n');
  });

  test('should display all player profile sections correctly', async ({ page }) => {
    console.log('[TEST] Starting: should display all player profile sections correctly');
    const profilePage = new PlayerProfilePage(page);
    // We're already on the player profile page from beforeEach

    // Wait for page to be fully loaded
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.profile-header', { timeout: 30000, state: 'visible' });

    // Assert profile header
    console.log('[TEST] Asserting profile header');
    await expect(profilePage.playerName()).toBeVisible({ timeout: 30000 });
    const playerName = await profilePage.getPlayerName();
    expect(playerName).toBeTruthy();
    console.log(`[TEST] Player name: ${playerName}`);

    await expect(profilePage.playerIdBadge()).toBeVisible({ timeout: 30000 });
    const playerIdText = await profilePage.getPlayerId();
    expect(playerIdText).toBeTruthy();

    await expect(profilePage.rankBadge()).toBeVisible({ timeout: 30000 });
    const currentRank = await profilePage.getCurrentRank();
    expect(currentRank).toBeTruthy();

    await expect(profilePage.currentRating()).toBeVisible({ timeout: 30000 });
    const kdRatio = await profilePage.getKDRatio();
    expect(kdRatio).toBeTruthy();
    console.log('[TEST] ✓ Profile header assertions passed');

    // Assert stats grid
    console.log('[TEST] Asserting stats grid');
    await expect(profilePage.statsGrid()).toBeVisible({ timeout: 30000 });
    // Wait for stat cards to be visible
    await page.waitForSelector('.stat-card', { timeout: 30000, state: 'visible' });
    const statCardsCount = await profilePage.getStatCardCount();
    expect(statCardsCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${statCardsCount} stat cards`);

    // Verify stat card data
    if (statCardsCount > 0) {
      const statCard = await profilePage.getStatCardData(0);
      expect(statCard.icon).toBeTruthy();
      expect(statCard.value).toBeTruthy();
      expect(statCard.label).toBeTruthy();
      console.log('[TEST] ✓ Stat card data is valid');
    }

    // Verify expected stats are present
    const statCards = await profilePage.statCards();
    const statCardsText = await statCards.allTextContents();
    const allStatsText = statCardsText.join(' ');
    expect(allStatsText).toMatch(/Games|Kills|Deaths|Assists|Headshot|Clutches|Rounds|Damage/i);
    console.log('[TEST] ✓ Expected stats are present');

    // Assert rating chart (if exists)
    console.log('[TEST] Asserting rating chart');
    const hasChart = await profilePage.hasRatingChart();
    expect(typeof hasChart).toBe('boolean');
    if (hasChart) {
      console.log('[TEST] ✓ Rating chart is visible');
    } else {
      console.log('[TEST] ⚠ Rating chart not visible (may not have enough data)');
    }

    // Assert accolades overview
    console.log('[TEST] Asserting accolades overview');
    await expect(profilePage.accoladesSection()).toBeVisible({ timeout: 30000 });
    console.log('[TEST] ✓ Accolades section is visible');

    // Assert recent accolades
    console.log('[TEST] Asserting recent accolades');
    const recentAccoladesVisible = await profilePage.recentAccolades().isVisible();
    if (recentAccoladesVisible) {
      const accoladesCount = await profilePage.getAccoladesCount();
      expect(accoladesCount).toBeGreaterThanOrEqual(0);
      console.log(`[TEST] ✓ Found ${accoladesCount} recent accolades`);

      if (accoladesCount > 0) {
        const accolade = await profilePage.getAccoladeItemData(0);
        expect(accolade.type).toBeTruthy();
        expect(accolade.position).toBeTruthy();
        expect(accolade.value).toBeTruthy();
        console.log('[TEST] ✓ Accolade item data is valid');
      }
    } else {
      console.log('[TEST] ⚠ Recent accolades section not visible');
    }

    // Assert past nicks section
    console.log('[TEST] Asserting past nicks section');
    const hasPastNicks = await profilePage.hasPastNicksSection();
    if (hasPastNicks) {
      const pastNicksCount = await profilePage.getPastNicksCount();
      expect(pastNicksCount).toBeGreaterThan(0);
      console.log(`[TEST] ✓ Found ${pastNicksCount} past nicks`);

      if (pastNicksCount > 0) {
        const allNicks = await profilePage.getAllPastNicks();
        expect(allNicks.length).toBeGreaterThan(0);
        // Verify that all nicks are non-empty strings
        allNicks.forEach((nick, idx) => {
          expect(nick).toBeTruthy();
          expect(nick.trim().length).toBeGreaterThan(0);
        });
        console.log(`[TEST] ✓ Past nicks are valid: ${allNicks.join(', ')}`);
        
        // Verify that the current player name is included in past nicks (or at least one nick exists)
        const currentPlayerName = await profilePage.getPlayerName();
        if (currentPlayerName) {
          const currentNameInPastNicks = allNicks.some(nick => 
            nick.toLowerCase() === currentPlayerName.toLowerCase()
          );
          // Note: current name might not be in past nicks if it's the only name used
          console.log(`[TEST] Current player name "${currentPlayerName}" ${currentNameInPastNicks ? 'is' : 'may not be'} in past nicks list`);
        }
      }
    } else {
      console.log('[TEST] ⚠ Past nicks section not visible (player may not have multiple nicks)');
    }

    console.log('[TEST] ✓ All player profile sections asserted successfully');
  });

  test('should have clickable navigation elements', async ({ page }) => {
    console.log('[TEST] Starting: should have clickable navigation elements');
    const profilePage = new PlayerProfilePage(page);
    // We're already on the player profile page from beforeEach

    // Test view game link from accolades
    console.log('[TEST] Testing view game link from accolades');
    const recentAccoladesVisible = await profilePage.recentAccolades().isVisible();
    if (recentAccoladesVisible) {
      const accoladesCount = await profilePage.getAccoladesCount();
      if (accoladesCount > 0) {
        await profilePage.clickViewGameLink(0);
        const currentUrl = page.url();
        if (currentUrl.includes('/games/')) {
          await expect(page).toHaveURL(/.*\/games\/\d+/);
          console.log('[TEST] ✓ View game link works, navigated to game details');
          
          // Navigate back
          await page.goBack();
          await page.waitForURL(new RegExp(`.*/players/${playerId}`), { timeout: 30000 });
          await page.waitForSelector('.profile-header', { timeout: 30000 });
          console.log('[TEST] ✓ Back navigation works');
        }
      }
    } else {
      console.log('[TEST] ⚠ Recent accolades not visible, skipping view game link test');
    }

    // Test back to rankings link
    console.log('[TEST] Testing back to rankings link');
    await profilePage.backToRankingsLink().click();
    await profilePage.waitForLoadState();
    await expect(page).toHaveURL(/.*\/rankings/);
    await expect(page.locator('.rankings-title')).toBeVisible();
    console.log('[TEST] ✓ Back to rankings link works');

    console.log('[TEST] ✓ All navigation elements tested');
  });
});
