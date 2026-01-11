/**
 * Test Data Fixtures
 * 
 * This file contains expected test data for E2E tests.
 * Update this file after running the API capture script against your staging instance.
 */

export interface TestGame {
  id: number;
  map: string;
  score: string;
  gameDate: string;
  formattedDuration: string;
}

export interface TestPlayer {
  playerId: string;
  playerName: string;
  rank: number;
  killDeathRatio: number;
  kills: number;
  deaths: number;
  assists: number;
  headshotPercentage: number;
}

export interface TestGameDetails {
  gameId: number;
  ctScore: number;
  tScore: number;
  totalRounds: number;
  playerStats: Array<{
    playerName: string;
    playerId: string;
    kills: number;
    deaths: number;
    assists: number;
  }>;
  rounds: Array<{
    roundNumber: number;
    winnerTeam: 'CT' | 'T';
  }>;
  accolades: Array<{
    typeDescription: string;
    playerName: string;
    playerId: string;
    value: number;
  }>;
}

export interface TestRoundDetails {
  gameId: number;
  roundNumber: number;
  winnerTeam: 'CT' | 'T';
  totalKills: number;
  headshotKills: number;
  totalAssists: number;
  bombPlanted: boolean;
  bombDefused: boolean;
  bombExploded: boolean;
  events: Array<{
    eventType: 'KILL' | 'ASSIST' | 'BOMB_EVENT' | 'ATTACK';
    player1Name?: string;
    player2Name?: string;
  }>;
}

export interface TestPlayerProfile {
  playerId: string;
  playerName: string;
  currentRank: number;
  killDeathRatio: number;
  totalGamesPlayed: number;
  totalKills: number;
  totalDeaths: number;
  totalAssists: number;
  headshotPercentage: number;
  clutchesWon: number;
}

// TODO: Update these values after running the API capture script
export const testData = {
  // Expected games on the games page
  games: [
    // Add game data here after capturing from staging
  ] as TestGame[],

  // Expected players in rankings
  players: [
    // Add player data here after capturing from staging
  ] as TestPlayer[],

  // Expected game details (for first game)
  gameDetails: null as TestGameDetails | null,

  // Expected round details (for first round of first game)
  roundDetails: null as TestRoundDetails | null,

  // Expected player profile (for first player)
  playerProfile: null as TestPlayerProfile | null,
};
