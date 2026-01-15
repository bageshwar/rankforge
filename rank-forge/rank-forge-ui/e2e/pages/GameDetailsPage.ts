import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class GameDetailsPage extends BasePage {
  readonly gameTitle = () => this.page.locator('.game-title');
  readonly headerScore = () => this.page.locator('.header-score');
  readonly headerScoreCT = () => this.page.locator('.header-score-ct');
  readonly headerScoreT = () => this.page.locator('.header-score-t');
  readonly gameMeta = () => this.page.locator('.game-meta');
  readonly backToGamesLink = () => this.page.locator('.back-btn');
  readonly roundTimeline = () => this.page.locator('.rounds-timeline');
  readonly roundBadges = () => this.page.locator('.round-badge');
  readonly playerStatsTable = () => this.page.locator('.stats-table');
  readonly playerStatsRows = () => this.page.locator('.stats-table tbody tr');
  readonly accoladesSection = () => this.page.locator('.accolades-section');
  readonly accoladesGrid = () => this.page.locator('.accolades-grid');
  readonly accoladeCards = () => this.page.locator('.accolade-card');

  constructor(page: Page) {
    super(page);
  }

  async navigate(gameId: number) {
    console.log(`[GameDetailsPage] Navigating to game details for game ${gameId}`);
    
    // Set up response listeners BEFORE navigation
    const gameResponsePromise = this.page.waitForResponse(
      (response) => response.url().includes(`/api/games/${gameId}`) && !response.url().includes('/details') && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log(`[GameDetailsPage] ⚠ Game API response listener timed out`);
      return null;
    });
    
    const detailsResponsePromise = this.page.waitForResponse(
      (response) => response.url().includes(`/api/games/${gameId}/details`) && response.status() === 200,
      { timeout: 70000 }
    ).catch(() => {
      console.log(`[GameDetailsPage] ⚠ Game details API response listener timed out`);
      return null;
    });
    
    await this.goto(`/games/${gameId}`);
    await this.waitForLoadState();
    
    // Wait for either API responses or UI elements
    console.log(`[GameDetailsPage] Waiting for game API: /api/games/${gameId}`);
    try {
      await Promise.race([
        gameResponsePromise,
        this.page.waitForSelector('.game-title', { timeout: 70000, state: 'visible' })
      ]);
      console.log(`[GameDetailsPage] ✓ Game API response received or title visible`);
    } catch (error) {
      console.log(`[GameDetailsPage] ⚠ Wait timed out, checking if data is already loaded`);
    }
    
    console.log(`[GameDetailsPage] Waiting for game details API: /api/games/${gameId}/details`);
    try {
      await Promise.race([
        detailsResponsePromise,
        this.page.waitForSelector('.rounds-timeline, .stats-table', { timeout: 70000, state: 'visible' })
      ]);
      console.log(`[GameDetailsPage] ✓ Game details API response received or content visible`);
    } catch (error) {
      console.log(`[GameDetailsPage] ⚠ Wait timed out, checking if data is already loaded`);
    }
    
    // Wait for key elements to be visible
    console.log(`[GameDetailsPage] Verifying game title is visible`);
    await this.waitForSelector('.game-title', 30000);
    console.log(`[GameDetailsPage] Game details page loaded for game ${gameId}`);
  }

  async getMapName(): Promise<string | null> {
    const title = this.gameTitle();
    await title.scrollIntoViewIfNeeded();
    return await this.safeTextContent(title);
  }

  async getScore(): Promise<{ ct: string | null; t: string | null }> {
    const ctLocator = this.headerScoreCT();
    const tLocator = this.headerScoreT();
    await ctLocator.scrollIntoViewIfNeeded();
    await tLocator.scrollIntoViewIfNeeded();
    const ct = await this.safeTextContent(ctLocator);
    const t = await this.safeTextContent(tLocator);
    return { ct, t };
  }

  async getRoundCount(): Promise<number> {
    console.log('[GameDetailsPage] Getting round count');
    const count = await this.roundBadges().count();
    console.log(`[GameDetailsPage] Found ${count} rounds`);
    return count;
  }

  async clickRound(roundNumber: number) {
    console.log(`[GameDetailsPage] Clicking round ${roundNumber}`);
    const roundBadge = this.roundBadges().nth(roundNumber - 1);
    // Wait for badge to be visible and scroll into view
    await roundBadge.waitFor({ state: 'visible', timeout: 10000 });
    await roundBadge.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    await roundBadge.click();
    console.log(`[GameDetailsPage] Round ${roundNumber} clicked, waiting for navigation`);
    await this.waitForLoadState();
    // Wait for round details API call (with extended timeout for slow DB)
    // Extract gameId from current URL
    const currentUrl = this.page.url();
    const gameIdMatch = currentUrl.match(/\/games\/(\d+)/);
    if (gameIdMatch) {
      const gameId = gameIdMatch[1];
      console.log(`[GameDetailsPage] Waiting for round details API: /api/games/${gameId}/rounds/${roundNumber}`);
      await this.waitForApiResponse(`/api/games/${gameId}/rounds/${roundNumber}`, 70000);
    }
    console.log(`[GameDetailsPage] Navigation to round ${roundNumber} complete`);
  }

  async getRoundBadgeClass(roundNumber: number): Promise<string | null> {
    const roundBadge = this.roundBadges().nth(roundNumber - 1);
    await roundBadge.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    return await roundBadge.getAttribute('class');
  }

  async getPlayerStatsCount(): Promise<number> {
    return await this.playerStatsRows().count();
  }

  async getPlayerStatRow(index: number) {
    const rows = await this.playerStatsRows();
    return rows.nth(index);
  }

  async getPlayerStatData(index: number) {
    const row = await this.getPlayerStatRow(index);
    // Wait for row to be visible and scroll into view
    await row.waitFor({ state: 'visible', timeout: 5000 });
    await row.scrollIntoViewIfNeeded();
    
    // Get player name from the link or the cell (excluding rank icon)
    const playerLink = row.locator('.player-profile-link');
    let playerName: string | null = null;
    if (await this.safeCount(playerLink) > 0) {
      playerName = await this.safeTextContent(playerLink);
    } else {
      // If no link, get text from cell and remove rank icon text
      const cellText = await this.safeTextContent(row.locator('.player-name-cell'));
      playerName = cellText?.replace(/[🥇🥈🥉]/g, '').trim() || null;
    }
    const kills = await this.safeTextContent(row.locator('.kills-cell'));
    const deaths = await this.safeTextContent(row.locator('.deaths-cell'));
    const assists = await this.safeTextContent(row.locator('.assists-cell'));
    const damage = await this.safeTextContent(row.locator('.damage-cell'));
    const headshotPct = await this.safeTextContent(row.locator('.headshot-cell'));
    
    return {
      playerName: playerName?.trim() || '',
      kills: kills || '',
      deaths: deaths || '',
      assists: assists || '',
      damage: damage || '',
      headshotPct: headshotPct || '',
    };
  }

  async getPlayerRowClass(index: number): Promise<string | null> {
    const row = await this.getPlayerStatRow(index);
    return await row.getAttribute('class');
  }

  async getRankIcon(index: number): Promise<string | null> {
    const row = await this.getPlayerStatRow(index);
    await row.waitFor({ state: 'visible', timeout: 5000 });
    await row.scrollIntoViewIfNeeded();
    
    // Rank icon is in: <td class="player-name-cell"><span class="rank-icon rank-gold">🥇</span>...</td>
    const playerNameCell = row.locator('td.player-name-cell').first();
    const rankIconSpan = playerNameCell.locator('span.rank-icon').first();
    
    // Check if rank icon exists
    const count = await this.safeCount(rankIconSpan, 1000);
    if (count > 0) {
      // Try innerText first (better for emojis)
      try {
        const innerText = await rankIconSpan.innerText({ timeout: 2000 });
        if (innerText && innerText.trim()) {
          return innerText.trim();
        }
      } catch (error) {
        // Fall back to textContent
      }
      
      const text = await this.safeTextContent(rankIconSpan, 2000);
      if (text && text.trim()) {
        return text.trim();
      }
    }
    
    // Fallback: extract emoji from player name cell text
    try {
      const cellInnerText = await playerNameCell.innerText({ timeout: 2000 });
      if (cellInnerText) {
        // Extract emoji from text (🥇🥈🥉)
        const emojiMatch = cellInnerText.match(/[🥇🥈🥉]/);
        if (emojiMatch) {
          return emojiMatch[0];
        }
      }
    } catch (error) {
      // Continue to textContent fallback
    }
    
    const cellText = await this.safeTextContent(playerNameCell, 2000);
    if (cellText) {
      // Extract emoji from text (🥇🥈🥉)
      const emojiMatch = cellText.match(/[🥇🥈🥉]/);
      if (emojiMatch) {
        return emojiMatch[0];
      }
    }
    
    return null;
  }

  async clickPlayerLink(index: number) {
    const row = await this.getPlayerStatRow(index);
    await row.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    const playerLink = row.locator('.player-profile-link');
    await this.safeClick(playerLink);
    await this.waitForLoadState();
  }

  async getAccoladesCount(): Promise<number> {
    return await this.accoladeCards().count();
  }

  async getAccoladeCard(index: number) {
    const cards = await this.accoladeCards();
    return cards.nth(index);
  }

  async getAccoladeData(index: number) {
    const card = await this.getAccoladeCard(index);
    // Wait for card to be visible and scroll into view
    await card.waitFor({ state: 'visible', timeout: 5000 });
    await card.scrollIntoViewIfNeeded();
    
    const type = await this.safeTextContent(card.locator('.accolade-type'));
    const playerName = await this.safeTextContent(card.locator('.accolade-player-link, .accolade-player'));
    const value = await this.safeTextContent(card.locator('.accolade-value'));
    
    return {
      type: type || '',
      playerName: playerName || '',
      value: value || '',
    };
  }

  async clickAccoladePlayerLink(index: number) {
    const card = await this.getAccoladeCard(index);
    await card.scrollIntoViewIfNeeded();
    await this.page.waitForTimeout(200);
    const playerLink = card.locator('.accolade-player-link');
    if (await this.safeCount(playerLink) > 0) {
      await this.safeClick(playerLink);
      await this.waitForLoadState();
    }
  }
}
