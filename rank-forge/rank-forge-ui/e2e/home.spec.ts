import { test, expect } from '@playwright/test';
import { HomePage } from './pages/HomePage';

test.describe('Home Page', () => {
  test('should display rankings and games links', async ({ page }) => {
    console.log('\n[TEST] Starting: should display rankings and games links');
    const homePage = new HomePage(page);
    await homePage.navigate();

    // Assert rankings link is visible
    const rankingsLinkVisible = await homePage.verifyRankingsLinkVisible();
    expect(rankingsLinkVisible).toBe(true);

    // Assert games link is visible
    const gamesLinkVisible = await homePage.verifyGamesLinkVisible();
    expect(gamesLinkVisible).toBe(true);
  });

  test('should navigate to rankings page when rankings link is clicked', async ({ page }) => {
    console.log('\n[TEST] Starting: should navigate to rankings page when rankings link is clicked');
    const homePage = new HomePage(page);
    await homePage.navigate();

    console.log('[TEST] Clicking rankings link');
    await homePage.clickRankingsLink();

    // Verify we're on the rankings page
    await expect(page).toHaveURL(/.*\/rankings/);
    await expect(page.locator('.rankings-title')).toBeVisible();
  });

  test('should navigate to games page when games link is clicked', async ({ page }) => {
    console.log('\n[TEST] Starting: should navigate to games page when games link is clicked');
    const homePage = new HomePage(page);
    await homePage.navigate();

    console.log('[TEST] Clicking games link');
    await homePage.clickGamesLink();

    // Verify we're on the games page
    await expect(page).toHaveURL(/.*\/games/);
    await expect(page.locator('.games-title')).toBeVisible();
  });
});
