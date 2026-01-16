import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add JWT token to requests if available
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle 401 errors (unauthorized) - clear token and redirect to login
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      // Clear token and trigger auth state update
      localStorage.removeItem('authToken');
      localStorage.removeItem('authUser');
      // Dispatch custom event for AuthContext to handle
      window.dispatchEvent(new Event('auth:logout'));
    }
    return Promise.reject(error);
  }
);

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
  damage: number;
  headshotKills: number;
  headshotPercentage: number;
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
  player1Team?: string;
  player2Id?: string;
  player2Name?: string;
  player2Team?: string;
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
  pastNicks?: string[];
}

// Leaderboard response with summary stats
export interface LeaderboardResponseDTO {
  rankings: PlayerRankingDTO[];
  totalGames: number;
  totalRounds: number;
  totalPlayers: number;
}

// Clan types
export interface ClanDTO {
  id: number;
  appServerId?: number | null; // Nullable - only set after step 2
  name?: string;
  telegramChannelId?: string;
  adminUserId: number;
  createdAt: number;
  updatedAt: number;
  apiKey?: string; // Only populated when creating new clan (shown once)
  hasApiKey?: boolean; // Indicates if key exists (but not the value)
  status?: string; // PENDING or ACTIVE
}

export interface ClanMembershipDTO {
  id: number;
  clanId: number;
  userId: number;
  joinedAt: number;
}

export interface CreateClanRequest {
  name?: string;
  telegramChannelId?: string;
  // appServerId is NOT in step 1 - it's configured in step 2
}

export interface ConfigureAppServerRequest {
  appServerId: number;
}

export interface RegenerateApiKeyResponse {
  apiKey: string;
  rotatedAt: number;
}

export interface ApiKeyStatus {
  hasApiKey: boolean;
  apiKeyCreatedAt?: number;
  apiKeyRotatedAt?: number;
}

export interface TransferAdminRequest {
  newAdminUserId: number;
}

// Rankings API
export const rankingsApi = {
  getAllWithStats: async (clanId: number): Promise<LeaderboardResponseDTO> => {
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings/stats', {
      params: { clanId },
    });
    return response.data;
  },

  getTopWithStats: async (limit: number = 10, clanId: number): Promise<LeaderboardResponseDTO> => {
    const response = await apiClient.get<LeaderboardResponseDTO>('/rankings/top/stats', {
      params: { limit, clanId },
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
    clanId: number,
    year?: number,
    month?: number,
    limit?: number,
    offset?: number
  ): Promise<LeaderboardResponseDTO> => {
    const params: Record<string, number> = { clanId };
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
  getAll: async (clanId: number): Promise<GameDTO[]> => {
    const response = await apiClient.get<GameDTO[]>('/games', {
      params: { clanId },
    });
    return response.data;
  },

  getRecent: async (limit: number = 10, clanId: number): Promise<GameDTO[]> => {
    const response = await apiClient.get<GameDTO[]>('/games/recent', {
      params: { limit, clanId },
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

// Auth API Types
export interface UserDTO {
  id: number;
  steamId64: string;
  steamId3: string;
  personaName: string;
  avatarUrl?: string;
  avatarMediumUrl?: string;
  avatarSmallUrl?: string;
  profileUrl?: string;
  accountCreated?: number;
  vacBanned: boolean;
  country?: string;
  createdAt?: number;
  lastLogin?: number;
  defaultClanId?: number;
}

export interface AuthResponseDTO {
  token: string;
  user: UserDTO;
  expiresAt: number;
}

export interface CurrentUserResponse {
  user: UserDTO;
  stats?: {
    currentRank: number;
    totalKills: number;
    totalDeaths: number;
    totalAssists: number;
    killDeathRatio: number;
    totalGamesPlayed: number;
    totalRoundsPlayed: number;
  };
}

// Auth API
export const authApi = {
  getLoginUrl: async (): Promise<string> => {
    const response = await apiClient.get<{ loginUrl: string }>('/auth/login');
    return response.data.loginUrl;
  },

  getMe: async (): Promise<CurrentUserResponse> => {
    const response = await apiClient.get<CurrentUserResponse>('/users/me');
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
  },

  refreshToken: async (): Promise<AuthResponseDTO> => {
    const response = await apiClient.post<AuthResponseDTO>('/auth/refresh');
    return response.data;
  },
};

// Users API
export const usersApi = {
  getAvatar: async (steamId: string): Promise<string> => {
    try {
      const response = await apiClient.get<{ avatarUrl: string }>(`/users/${steamId}/avatar`);
      return response.data.avatarUrl;
    } catch (error) {
      return '/default-avatar.png';
    }
  },

  updateDefaultClan: async (clanId: number | null): Promise<void> => {
    await apiClient.put('/users/me/default-clan', { clanId });
  },
};

// Clans API
export const clansApi = {
  create: async (clanData: CreateClanRequest): Promise<ClanDTO> => {
    const response = await apiClient.post<ClanDTO>('/clans', clanData);
    return response.data;
  },

  configureAppServerId: async (clanId: number, appServerId: number): Promise<ClanDTO> => {
    const response = await apiClient.put<ClanDTO>(`/clans/${clanId}/configure-app-server`, {
      appServerId,
    });
    return response.data;
  },

  regenerateApiKey: async (clanId: number): Promise<RegenerateApiKeyResponse> => {
    const response = await apiClient.post<RegenerateApiKeyResponse>(`/clans/${clanId}/regenerate-api-key`);
    return response.data;
  },

  getApiKeyStatus: async (clanId: number): Promise<ApiKeyStatus> => {
    const response = await apiClient.get<ApiKeyStatus>(`/clans/${clanId}/api-key-status`);
    return response.data;
  },

  getMyClans: async (): Promise<ClanDTO[]> => {
    const response = await apiClient.get<ClanDTO[]>('/clans/my');
    return response.data;
  },

  getClan: async (id: number): Promise<ClanDTO | null> => {
    try {
      const response = await apiClient.get<ClanDTO>(`/clans/${id}`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  getClanMembers: async (id: number): Promise<ClanMembershipDTO[]> => {
    const response = await apiClient.get<ClanMembershipDTO[]>(`/clans/${id}/members`);
    return response.data;
  },

  transferAdmin: async (clanId: number, newAdminId: number): Promise<ClanDTO> => {
    const response = await apiClient.put<ClanDTO>(`/clans/${clanId}/admin`, {
      newAdminUserId: newAdminId,
    });
    return response.data;
  },

  checkAppServer: async (appServerId: number): Promise<{ claimed: boolean }> => {
    const response = await apiClient.get<{ claimed: boolean }>(`/clans/check-app-server/${appServerId}`);
    return response.data;
  },

  getClanByAppServerId: async (appServerId: number): Promise<ClanDTO | null> => {
    try {
      const response = await apiClient.get<ClanDTO>(`/clans/by-app-server/${appServerId}`);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  health: async (): Promise<string> => {
    const response = await apiClient.get<string>('/clans/health');
    return response.data;
  },
};

export default apiClient;
