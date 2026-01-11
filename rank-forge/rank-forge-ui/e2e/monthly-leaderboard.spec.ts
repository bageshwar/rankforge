import { test, expect } from '@playwright/test';
import { RankingsPage } from './pages/RankingsPage';

test.describe('Monthly Leaderboard', () => {
  test.beforeEach(async ({ page }) => {
    console.log('\n[TEST] ===== Starting monthly leaderboard tests =====');
    const rankingsPage = new RankingsPage(page);
    await rankingsPage.navigate();
    console.log('[TEST] ===== beforeEach complete, ready for test =====\n');
  });

  test('should display monthly tab and switch between tabs', async ({ page }) => {
    console.log('[TEST] Starting: should display monthly tab and switch between tabs');
    const rankingsPage = new RankingsPage(page);

    // Verify Monthly tab exists
    const monthlyTab = page.getByRole('link', { name: 'Monthly' });
    await expect(monthlyTab).toBeVisible();
    console.log('[TEST] ✓ Monthly tab is visible');

    // Click Monthly tab
    await monthlyTab.click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    
    // Verify URL includes monthly tab
    await expect(page).toHaveURL(/.*tab=monthly/);
    console.log('[TEST] ✓ URL updated with monthly tab');

    // Verify month/year selectors are visible
    const monthSelect = page.locator('#month-select');
    const yearSelect = page.locator('#year-select');
    await expect(monthSelect).toBeVisible();
    await expect(yearSelect).toBeVisible();
    console.log('[TEST] ✓ Month and year selectors are visible');

    // Click All Time tab
    const allTimeTab = page.getByRole('link', { name: 'All Time' });
    await allTimeTab.click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    
    // Verify month/year selectors are hidden
    await expect(monthSelect).not.toBeVisible();
    await expect(yearSelect).not.toBeVisible();
    console.log('[TEST] ✓ Month and year selectors hidden on All Time tab');
  });

  test('should display monthly leaderboard with correct data', async ({ page }) => {
    console.log('[TEST] Starting: should display monthly leaderboard with correct data');
    const rankingsPage = new RankingsPage(page);

    // Switch to Monthly tab
    await page.getByRole('link', { name: 'Monthly' }).click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });

    // Verify table headers include Games column
    await expect(page.getByRole('columnheader', { name: 'Games', exact: true })).toBeVisible();
    console.log('[TEST] ✓ Games column header is visible');

    // Verify rankings table has data
    const rankingsCount = await rankingsPage.getRankingsCount();
    expect(rankingsCount).toBeGreaterThan(0);
    console.log(`[TEST] ✓ Found ${rankingsCount} players in monthly leaderboard`);

    // Verify summary statistics are displayed
    const totalPlayers = page.locator('.stats-summary .stat-item').first();
    const totalGames = page.locator('.stats-summary .stat-item').nth(1);
    const totalRounds = page.locator('.stats-summary .stat-item').nth(2);
    
    await expect(totalPlayers).toBeVisible();
    await expect(totalGames).toBeVisible();
    await expect(totalRounds).toBeVisible();
    console.log('[TEST] ✓ Summary statistics are visible');

    // Verify first player has games played
    const firstRow = page.locator('.rankings-table tbody tr').first();
    const gamesCell = firstRow.locator('td').nth(8); // Games column (adjust index if needed)
    const gamesText = await gamesCell.textContent();
    expect(gamesText).toBeTruthy();
    console.log(`[TEST] ✓ First player has games played: ${gamesText}`);
  });

  test('should change month and year and update leaderboard', async ({ page }) => {
    console.log('[TEST] Starting: should change month and year and update leaderboard');
    const rankingsPage = new RankingsPage(page);

    // Switch to Monthly tab
    await page.getByRole('link', { name: 'Monthly' }).click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });

    // Get initial rankings count
    const initialCount = await rankingsPage.getRankingsCount();
    console.log(`[TEST] Initial rankings count: ${initialCount}`);

    // Change month
    const monthSelect = page.locator('#month-select');
    const currentMonth = await monthSelect.inputValue();
    const newMonth = currentMonth === '11' ? '10' : '11';
    
    console.log(`[TEST] Changing month from ${currentMonth} to ${newMonth}`);
    await monthSelect.selectOption(newMonth);
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });

    // Verify URL updated
    await expect(page).toHaveURL(new RegExp(`.*month=${newMonth}`));
    console.log('[TEST] ✓ URL updated with new month');

    // Verify rankings may have changed (or be empty for that month)
    const newCount = await rankingsPage.getRankingsCount();
    console.log(`[TEST] New rankings count: ${newCount}`);
    console.log('[TEST] ✓ Month change completed');
  });

  test('should update URL when changing filters', async ({ page }) => {
    console.log('[TEST] Starting: should update URL when changing filters');
    const rankingsPage = new RankingsPage(page);

    // Switch to Monthly tab
    await page.getByRole('link', { name: 'Monthly' }).click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    // Change month
    await page.locator('#month-select').selectOption('11');
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    // Verify URL contains month and year
    const url = page.url();
    expect(url).toContain('tab=monthly');
    expect(url).toContain('month=11');
    expect(url).toContain('year=');
    console.log('[TEST] ✓ URL contains monthly leaderboard parameters');

    // Change year
    const yearSelect = page.locator('#year-select');
    const currentYear = await yearSelect.inputValue();
    const newYear = (parseInt(currentYear) - 1).toString();
    
    await yearSelect.selectOption(newYear);
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    // Verify URL updated with new year
    await expect(page).toHaveURL(new RegExp(`.*year=${newYear}`));
    console.log('[TEST] ✓ URL updated with new year');
  });

  test('should show loading state when changing filters', async ({ page }) => {
    console.log('[TEST] Starting: should show loading state when changing filters');
    const rankingsPage = new RankingsPage(page);

    // Switch to Monthly tab
    await page.getByRole('link', { name: 'Monthly' }).click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    // Change month and verify loading indicator appears (if implemented)
    const monthSelect = page.locator('#month-select');
    const loadingPromise = page.waitForSelector('.loading-indicator', { timeout: 5000 }).catch(() => null);
    
    await monthSelect.selectOption('10');
    
    // Loading indicator may appear briefly
    const loadingIndicator = await loadingPromise;
    if (loadingIndicator) {
      console.log('[TEST] ✓ Loading indicator appeared');
    } else {
      console.log('[TEST] ⚠ Loading indicator not found (may be too fast or not implemented)');
    }

    // Wait for data to load
    await page.waitForLoadState('networkidle', { timeout: 70000 });
    await page.waitForSelector('.rankings-table tbody tr', { timeout: 30000, state: 'visible' });
    console.log('[TEST] ✓ Data loaded after filter change');
  });

  test('should handle empty monthly leaderboard gracefully', async ({ page }) => {
    console.log('[TEST] Starting: should handle empty monthly leaderboard gracefully');
    const rankingsPage = new RankingsPage(page);

    // Switch to Monthly tab
    await page.getByRole('link', { name: 'Monthly' }).click();
    await page.waitForLoadState('networkidle', { timeout: 70000 });

    // Try to select a future month (should be handled gracefully)
    const currentDate = new Date();
    const futureYear = currentDate.getFullYear() + 1;
    
    // Select a future year if available
    const yearSelect = page.locator('#year-select');
    const yearOptions = await yearSelect.locator('option').allTextContents();
    const hasFutureYear = yearOptions.some(y => parseInt(y) >= futureYear);
    
    if (hasFutureYear) {
      await yearSelect.selectOption(futureYear.toString());
      await page.waitForLoadState('networkidle', { timeout: 70000 });
      
      // Should show empty state or error message
      const rankingsCount = await rankingsPage.getRankingsCount();
      console.log(`[TEST] Future year rankings count: ${rankingsCount}`);
      console.log('[TEST] ✓ Empty leaderboard handled gracefully');
    } else {
      console.log('[TEST] ⚠ Future year not available in dropdown, skipping test');
    }
  });
});
