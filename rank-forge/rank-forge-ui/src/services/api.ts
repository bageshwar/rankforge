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
}

export interface GameDTO {
  gameId: string;
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
  kills: number;
  deaths: number;
  assists: number;
  rating: number;
}

export interface AccoladeDTO {
  typeDescription: string;
  position: number;
  playerName: string;
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

// Rankings API
export const rankingsApi = {
  getAll: async (): Promise<PlayerRankingDTO[]> => {
    const response = await apiClient.get<PlayerRankingDTO[]>('/rankings');
    return response.data;
  },

  getTop: async (limit: number = 10): Promise<PlayerRankingDTO[]> => {
    const response = await apiClient.get<PlayerRankingDTO[]>('/rankings/top', {
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

  health: async (): Promise<string> => {
    const response = await apiClient.get<string>('/games/health');
    return response.data;
  },
};

export default apiClient;
