import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Types
export interface PlayerRankingDTO {
  rank: number;
  playerId: string;
  playerName: string;
  killDeathRatio: number;
  kills: number;
  deaths: number;
  assists: number;
  headshotPercentage: number;
  roundsPlayed: number;
  clutchesWon: number;
  damageDealt: number;
  gamesPlayed: number;
}

export interface GameDTO {
  id: number;  // Database ID from Game table
  gameDate: string;
  map: string;
  mode: string;
  score: string;
  formattedDuration: string;
  players?: string[];
}

export interface RoundDTO {
  roundNumber: number;
  winnerTeam: 'CT' | 'T';
  winCondition?: string;
}

export interface PlayerStatDTO {
  playerName: string;
  playerId: string;
  kills: number;
  deaths: number;
  assists: number;
  rating: number;
}

export interface AccoladeDTO {
  typeDescription: string;
  position: number;
  playerName: string;
  playerId: string;
  value: number;
  score: number;
}

export interface GameDetailsDTO {
  gameId: string;
  ctScore: number;
  tScore: number;
  totalRounds: number;
  playerStats?: PlayerStatDTO[];
  rounds?: RoundDTO[];
  accolades?: AccoladeDTO[];
}

export interface RoundEventDTO {
  id: number;
  eventType: 'KILL' | 'ASSIST' | 'ATTACK' | 'BOMB_EVENT';
  timestamp: string;
  timeOffsetMs: number;
  player1Id?: string;
  player1Name?: string;
  player2Id?: string;
  player2Name?: string;
  weapon?: string;
  isHeadshot?: boolean;
  damage?: number;
  armorDamage?: number;
  hitGroup?: string;
  bombEventType?: string;
  assistType?: string;
}

export interface RoundDetailsDTO {
  gameId: number;
  roundNumber: number;
  winnerTeam: 'CT' | 'T';
  roundStartTime: string;
  roundEndTime: string;
  durationMs: number;
  events: RoundEventDTO[];
  totalKills: number;
  totalAssists: number;
  headshotKills: number;
  bombPlanted: boolean;
  bombDefused: boolean;
  bombExploded: boolean;
}

// Player Profile Types
export interface RatingHistoryPoint {
  gameDate: string;
  rank: number;
  killDeathRatio: number;
  kills: number;
  deaths: number;
  assists: number;
  gameNumber: number;
}

export interface PlayerAccoladeDTO {
  type: string;
  typeDescription: string;
  value: number;
  position: number;
  score: number;
  gameDate: string;
  gameId: number;
}

export interface PlayerProfileDTO {
  playerId: string;
  playerName: string;
  currentRank: number;
  totalKills: number;
  totalDeaths: number;
  totalAssists: number;
  killDeathRatio: number;
  headshotKills: number;
  headshotPercentage: number;
  totalRoundsPlayed: number;
  clutchesWon: number;
  totalDamageDealt: number;
  totalGamesPlayed: number;
  ratingHistory: RatingHistoryPoint[];
  accolades: PlayerAccoladeDTO[];
  accoladesByType: Record<string, number>;
  mostFrequentAccolade: string;
  totalAccolades: number;
}

// Leaderboard response with summary stats
export interface LeaderboardResponseDTO {
  rankings: PlayerRankingDTO[];
  totalGames: number;
  totalRounds: number;
  totalPlayers: number;
}

// Rankings API
export const rankingsApi = {
  /**
   * Get all player rankings with summary statistics
   * @deprecated Use getAllWithStats() for consistency. This method is kept for backward compatibility.
   */
  getAll: async (): Promise<LeaderboardResponseDTO> => {
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings');
    return response.data;
  },

  getAllWithStats: async (): Promise<LeaderboardResponseDTO> => {
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings/stats');
    return response.data;
  },

  /**
   * Get top N player rankings with summary statistics
   * @deprecated Use getTopWithStats() for consistency. This method is kept for backward compatibility.
   */
  getTop: async (limit: number = 10): Promise<LeaderboardResponseDTO> => {
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings/top', {
      params: { limit },
    });
    return response.data;
  },

  getTopWithStats: async (limit: number = 10): Promise<LeaderboardResponseDTO> => {
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings/top/stats', {
      params: { limit },
    });
    return response.data;
  },

  getPlayer: async (playerId: string): Promise<PlayerRankingDTO | null> => {
    try {
      const response = await apiClient.get<PlayerRankingDTO>(`/rankings/player/${playerId}`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  health: async (): Promise<string> => {
    const response = await apiClient.get<string>('/rankings/health');
    return response.data;
  },

  getMonthlyLeaderboard: async (
    year?: number,
    month?: number,
    limit?: number,
    offset?: number
  ): Promise<LeaderboardResponseDTO> => {
    const params: Record<string, number> = {};
    if (year !== undefined) params.year = year;
    if (month !== undefined) params.month = month;
    if (limit !== undefined) params.limit = limit;
    if (offset !== undefined) params.offset = offset;
    
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings/leaderboard/monthly', {
      params,
    });
    return response.data;
  },
};

// Games API
export const gamesApi = {
  getAll: async (): Promise<GameDTO[]> => {
    const response = await apiClient.get<GameDTO[]>('/games');
    return response.data;
  },

  getRecent: async (limit: number = 10): Promise<GameDTO[]> => {
    const response = await apiClient.get<GameDTO[]>('/games/recent', {
      params: { limit },
    });
    return response.data;
  },

  getById: async (gameId: string): Promise<GameDTO | null> => {
    try {
      const response = await apiClient.get<GameDTO>(`/games/${gameId}`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  getDetails: async (gameId: string): Promise<GameDetailsDTO | null> => {
    try {
      const response = await apiClient.get<GameDetailsDTO>(`/games/${gameId}/details`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  getRoundDetails: async (gameId: string, roundNumber: number): Promise<RoundDetailsDTO | null> => {
    try {
      const response = await apiClient.get<RoundDetailsDTO>(`/games/${gameId}/rounds/${roundNumber}`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  health: async (): Promise<string> => {
    const response = await apiClient.get<string>('/games/health');
    return response.data;
  },
};

// Players API
export const playersApi = {
  getProfile: async (playerId: string): Promise<PlayerProfileDTO | null> => {
    try {
      const encodedId = encodeURIComponent(playerId);
      const response = await apiClient.get<PlayerProfileDTO>(`/players/${encodedId}`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  getAll: async (): Promise<PlayerProfileDTO[]> => {
    const response = await apiClient.get<PlayerProfileDTO[]>('/players');
    return response.data;
  },

  health: async (): Promise<string> => {
    const response = await apiClient.get<string>('/players/health');
    return response.data;
  },
};

export default apiClient;
