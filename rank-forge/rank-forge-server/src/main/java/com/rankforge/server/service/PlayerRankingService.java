/*
 *
 *  *Copyright [2024] [Bageshwar Pratap Narain]
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.rankforge.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.interfaces.RankingAlgorithm;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.pipeline.EloBasedRankingAlgorithm;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.LeaderboardResponseDTO;
import com.rankforge.server.dto.PlayerRankingDTO;
import com.rankforge.server.entity.Clan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for managing player rankings
 * Uses Spring Data JPA to provide real player statistics and rankings.
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class PlayerRankingService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRankingService.class);
    
    private final PlayerStatsRepository playerStatsRepository;
    private final GameRepository gameRepository;
    private final GameEventRepository gameEventRepository;
    private final ObjectMapper objectMapper;
    private final RankingAlgorithm rankingAlgorithm;
    private final ClanService clanService;
    private final CacheManager cacheManager;
    
    @Autowired
    public PlayerRankingService(PlayerStatsRepository playerStatsRepository, 
                               GameRepository gameRepository,
                               GameEventRepository gameEventRepository,
                               ObjectMapper objectMapper,
                               RankingAlgorithm rankingAlgorithm,
                               ClanService clanService,
                               CacheManager cacheManager) {
        this.playerStatsRepository = playerStatsRepository;
        this.gameRepository = gameRepository;
        this.gameEventRepository = gameEventRepository;
        this.objectMapper = objectMapper;
        this.rankingAlgorithm = rankingAlgorithm;
        this.clanService = clanService;
        this.cacheManager = cacheManager;
    }

    /**
     * Get all player rankings sorted by existing rank field
     * Fetches real data from the persistence layer and sorts by rank
     * @param clanId Required clan ID to filter rankings by appServerId
     */
    public List<PlayerRankingDTO> getAllPlayerRankings(Long clanId) {
        LOGGER.info("🔍 [ALL-TIME] getAllPlayerRankings called for clanId: {}", clanId);
        try {
            List<PlayerStats> playerStats = getAllPlayerStatsFromDatabase(clanId);
            LOGGER.info("🔍 [ALL-TIME] getAllPlayerStatsFromDatabase returned {} player stats", playerStats.size());
            
            // Sort by existing rank field (descending order - rank 1 is best)
            playerStats.sort((p1, p2) -> Integer.compare(p2.getRank(), p1.getRank()));
            
            List<PlayerRankingDTO> rankings = playerStats.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            LOGGER.info("🔍 [ALL-TIME] Converted to {} PlayerRankingDTOs", rankings.size());
            if (!rankings.isEmpty()) {
                LOGGER.info("🔍 [ALL-TIME] Sample ranking - Player: {}, Rank: {}, Kills: {}, Deaths: {}", 
                        rankings.get(0).getPlayerName(), rankings.get(0).getRank(), 
                        rankings.get(0).getKills(), rankings.get(0).getDeaths());
            }
            
            return rankings;
                    
        } catch (Exception e) {
            LOGGER.error("🔍 [ALL-TIME] Failed to retrieve player rankings for clanId: {}", clanId, e);
            // Return empty list on error instead of crashing
            return new ArrayList<>();
        }
    }

    /**
     * Get top N player rankings
     * @param limit Number of top players
     * @param clanId Required clan ID to filter rankings
     */
    public List<PlayerRankingDTO> getTopPlayerRankings(int limit, Long clanId) {
        return getAllPlayerRankings(clanId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all player rankings with summary statistics
     * Cached for 1 minute as data changes when new games are processed
     * @param clanId Required clan ID to filter rankings
     */
    @Cacheable(value = "allTimeLeaderboard", 
               key = "'clan-' + #clanId",
               condition = "#result != null && #result.totalPlayers > 0")
    public LeaderboardResponseDTO getAllPlayerRankingsWithStats(Long clanId) {
        LOGGER.info("🔍 [ALL-TIME] Starting getAllPlayerRankingsWithStats for clanId: {}", clanId);
        
        // Check cache first (logging will happen in CacheConfig)
        Cache cache = cacheManager.getCache("allTimeLeaderboard");
        if (cache != null) {
            String cacheKey = "clan-" + clanId;
            Cache.ValueWrapper cachedValue = cache.get(cacheKey);
            if (cachedValue != null) {
                LOGGER.info("🔍 [ALL-TIME] Cache HIT for key: {}", cacheKey);
                LeaderboardResponseDTO cached = (LeaderboardResponseDTO) cachedValue.get();
                LOGGER.info("🔍 [ALL-TIME] Cached result - players: {}, games: {}, rounds: {}", 
                        cached.getTotalPlayers(), cached.getTotalGames(), cached.getTotalRounds());
                return cached;
            } else {
                LOGGER.info("🔍 [ALL-TIME] Cache MISS for key: {}", cacheKey);
            }
        } else {
            LOGGER.warn("🔍 [ALL-TIME] Cache 'allTimeLeaderboard' not found!");
        }
        
        LOGGER.info("🔍 [ALL-TIME] Querying database for clanId: {}", clanId);
        List<PlayerRankingDTO> rankings = getAllPlayerRankings(clanId);
        LOGGER.info("🔍 [ALL-TIME] Retrieved {} rankings from getAllPlayerRankings", rankings.size());
        
        // Filter by clan's appServerId (required)
        Optional<Clan> clanOpt = clanService.getClanById(clanId);
        if (clanOpt.isEmpty()) {
            LOGGER.warn("🔍 [ALL-TIME] Clan not found: {}", clanId);
            return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
        }
        
        Clan clan = clanOpt.get();
        Long appServerId = clan.getAppServerId();
        LOGGER.info("🔍 [ALL-TIME] Clan found - ID: {}, name: {}, appServerId: {}", 
                clan.getId(), clan.getName(), appServerId);
        
        if (appServerId == null) {
            LOGGER.warn("🔍 [ALL-TIME] Clan {} has null appServerId - cannot query games", clanId);
            return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
        }
        
        List<GameEntity> clanGames = gameRepository.findByAppServerId(appServerId);
        LOGGER.info("🔍 [ALL-TIME] Found {} games for appServerId: {}", clanGames.size(), appServerId);
        
        long totalGames = clanGames.size();
        long totalRounds = clanGames.stream()
            .mapToLong(g -> (g.getTeam1Score() + g.getTeam2Score()))
            .sum();
        
        int totalPlayers = rankings.size();
        LOGGER.info("🔍 [ALL-TIME] Building response - rankings: {}, totalGames: {}, totalRounds: {}, totalPlayers: {}", 
                rankings.size(), totalGames, totalRounds, totalPlayers);
        
        LeaderboardResponseDTO response = new LeaderboardResponseDTO(rankings, totalGames, totalRounds, totalPlayers);
        
        // Log if result will be cached
        if (totalPlayers > 0) {
            LOGGER.info("🔍 [ALL-TIME] Result will be CACHED (totalPlayers > 0)");
        } else {
            LOGGER.info("🔍 [ALL-TIME] Result will NOT be cached (totalPlayers = 0)");
        }
        
        return response;
    }
    
    /**
     * Get top N player rankings with summary statistics
     * Cached for 1 minute as data changes when new games are processed
     * @param limit Number of top players
     * @param clanId Required clan ID to filter rankings
     */
    @Cacheable(value = "topLeaderboard", 
               key = "#limit + '-' + #clanId",
               condition = "#result != null && #result.totalPlayers > 0")
    public LeaderboardResponseDTO getTopPlayerRankingsWithStats(int limit, Long clanId) {
        List<PlayerRankingDTO> rankings = getTopPlayerRankings(limit, clanId);
        
        // Filter by clan's appServerId (required)
        Optional<Clan> clanOpt = clanService.getClanById(clanId);
        if (clanOpt.isEmpty()) {
            LOGGER.warn("Clan not found: {}", clanId);
            return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
        }
        Long appServerId = clanOpt.get().getAppServerId();
        List<GameEntity> clanGames = gameRepository.findByAppServerId(appServerId);
        long totalGames = clanGames.size();
        long totalRounds = clanGames.stream()
            .mapToLong(g -> (g.getTeam1Score() + g.getTeam2Score()))
            .sum();
        List<PlayerRankingDTO> allRankings = getAllPlayerRankings(clanId);
        int totalPlayers = allRankings.size();
        
        return new LeaderboardResponseDTO(rankings, totalGames, totalRounds, totalPlayers);
    }

    /**
     * Convert PlayerStats to PlayerRankingDTO
     */
    private PlayerRankingDTO convertToDTO(PlayerStats stats) {
        // Count distinct games for this player
        long gamesPlayed = playerStatsRepository.countDistinctGamesByPlayerId(stats.getPlayerId());
        return new PlayerRankingDTO(
                stats.getRank(),
                stats.getLastSeenNickname(),
                stats.getPlayerId(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getAssists(),
                stats.getHeadshotKills(),
                stats.getRoundsPlayed(),
                stats.getClutchesWon(),
                stats.getDamageDealt(),
                (int) gamesPlayed
        );
    }
    
    /**
     * Convert PlayerStats to PlayerRankingDTO with custom games played count
     */
    private PlayerRankingDTO convertToDTO(PlayerStats stats, int gamesPlayed) {
        return new PlayerRankingDTO(
                stats.getRank(),
                stats.getLastSeenNickname(),
                stats.getPlayerId(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getAssists(),
                stats.getHeadshotKills(),
                stats.getRoundsPlayed(),
                stats.getClutchesWon(),
                stats.getDamageDealt(),
                gamesPlayed
        );
    }

    /**
     * Convert PlayerStatsEntity to PlayerStats domain object
     */
    private PlayerStats convertToDomain(PlayerStatsEntity entity) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(entity.getPlayerId());
        stats.setKills(entity.getKills());
        stats.setDeaths(entity.getDeaths());
        stats.setAssists(entity.getAssists());
        stats.setHeadshotKills(entity.getHeadshotKills());
        stats.setRoundsPlayed(entity.getRoundsPlayed());
        stats.setClutchesWon(entity.getClutchesWon());
        stats.setDamageDealt(entity.getDamageDealt());
        stats.setLastUpdated(entity.getLastUpdated());
        stats.setRank(entity.getRank());
        stats.setLastSeenNickname(entity.getLastSeenNickname());
        return stats;
    }
    
    /**
     * Retrieves all player statistics from the database
     * Gets the latest stats for each player (most recent gameTimestamp)
     */
    private List<PlayerStats> getAllPlayerStatsFromDatabase(Long clanId) {
        LOGGER.info("🔍 [ALL-TIME] getAllPlayerStatsFromDatabase called for clanId: {}", clanId);
        List<PlayerStats> playerStatsList = new ArrayList<>();
        
        try {
            // Get clan's appServerId and find all games (required)
            Optional<Clan> clanOpt = clanService.getClanById(clanId);
            if (clanOpt.isEmpty()) {
                LOGGER.warn("🔍 [ALL-TIME] Clan not found: {}", clanId);
                return new ArrayList<>();
            }
            
            Clan clan = clanOpt.get();
            Long appServerId = clan.getAppServerId();
            LOGGER.info("🔍 [ALL-TIME] Clan lookup - ID: {}, appServerId: {}", clanId, appServerId);
            
            if (appServerId == null) {
                LOGGER.warn("🔍 [ALL-TIME] Clan {} has null appServerId - cannot query games", clanId);
                return new ArrayList<>();
            }
            
            List<GameEntity> clanGames = gameRepository.findByAppServerId(appServerId);
            LOGGER.info("🔍 [ALL-TIME] Found {} games for appServerId: {}", clanGames.size(), appServerId);
            
            List<Long> clanGameIds = clanGames.stream()
                .map(GameEntity::getId)
                .collect(Collectors.toList());
            LOGGER.info("🔍 [ALL-TIME] Clan game IDs list size: {} (IDs: {})", clanGameIds.size(), clanGameIds);
            
            if (clanGameIds.isEmpty()) {
                LOGGER.warn("🔍 [ALL-TIME] No games found for clan {} - returning empty list", clanId);
                return new ArrayList<>();
            }
            
            // Use the same approach as monthly rankings: query all latest stats, then filter by game's appServerId
            // This works because monthly rankings use this pattern successfully
            LOGGER.info("🔍 [ALL-TIME] Querying all latest stats (same pattern as monthly rankings)");
            List<PlayerStatsEntity> allLatestStats = playerStatsRepository.findLatestStatsForAllPlayers();
            LOGGER.info("🔍 [ALL-TIME] Retrieved {} total latest stats from database", allLatestStats.size());
            
            // Get game timestamps for the clan's games to match by timestamp (like monthly does)
            Set<Instant> clanGameTimestamps = clanGames.stream()
                .map(GameEntity::getGameOverTimestamp)
                .filter(ts -> ts != null)
                .collect(Collectors.toSet());
            LOGGER.info("🔍 [ALL-TIME] Clan games have {} unique timestamps: {}", clanGameTimestamps.size(), clanGameTimestamps);
            
            // Filter stats by checking if game's appServerId matches (like monthly rankings do)
            List<PlayerStatsEntity> entities = new ArrayList<>();
            int matchedByAppServerId = 0;
            int matchedByTimestamp = 0;
            int skippedNullGame = 0;
            int skippedWrongAppServerId = 0;
            
            for (PlayerStatsEntity entity : allLatestStats) {
                if (entity.getGame() == null) {
                    skippedNullGame++;
                    // Try matching by timestamp as fallback
                    if (clanGameTimestamps.contains(entity.getGameTimestamp())) {
                        entities.add(entity);
                        matchedByTimestamp++;
                        LOGGER.debug("🔍 [ALL-TIME] Matched by timestamp: playerId={}, gameTimestamp={}", 
                                entity.getPlayerId(), entity.getGameTimestamp());
                    }
                    continue;
                }
                
                Long gameAppServerId = entity.getGame().getAppServerId();
                if (appServerId.equals(gameAppServerId)) {
                    entities.add(entity);
                    matchedByAppServerId++;
                    LOGGER.debug("🔍 [ALL-TIME] Matched by appServerId: playerId={}, gameId={}, appServerId={}", 
                            entity.getPlayerId(), entity.getGame().getId(), gameAppServerId);
                } else {
                    skippedWrongAppServerId++;
                    LOGGER.debug("🔍 [ALL-TIME] Skipped - wrong appServerId: playerId={}, gameAppServerId={}, expected={}", 
                            entity.getPlayerId(), gameAppServerId, appServerId);
                }
            }
            
            LOGGER.info("🔍 [ALL-TIME] Filtered stats - total: {}, matched by appServerId: {}, matched by timestamp: {}, skipped (null game): {}, skipped (wrong appServerId): {}", 
                    entities.size(), matchedByAppServerId, matchedByTimestamp, skippedNullGame, skippedWrongAppServerId);
            
            // Convert to domain objects
            for (PlayerStatsEntity entity : entities) {
                if (entity.getGame() != null) {
                    LOGGER.debug("🔍 [ALL-TIME] Including player: {}, gameId: {}", entity.getPlayerId(), entity.getGame().getId());
                } else {
                    LOGGER.warn("🔍 [ALL-TIME] Entity has null game despite query filter - player: {}", entity.getPlayerId());
                }
                PlayerStats stats = convertToDomain(entity);
                playerStatsList.add(stats);
            }
            
            LOGGER.info("🔍 [ALL-TIME] Converted {} entities to PlayerStats", playerStatsList.size());
        } catch (Exception e) {
            LOGGER.error("🔍 [ALL-TIME] Failed to retrieve player statistics for clanId: {}", clanId, e);
        }

        LOGGER.info("🔍 [ALL-TIME] Returning {} player statistics for clanId: {}", playerStatsList.size(), clanId);
        return playerStatsList;
    }
    

    /**
     * Gets a specific player's ranking and statistics
     */
    public Optional<PlayerRankingDTO> getPlayerRanking(String playerId) {
        return Optional.empty();
    }
    
    /**
     * Get monthly player rankings showing stats accumulated during a specific month
     * @param year The year (e.g., 2026)
     * @param month The month (1-12)
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip for pagination
     * @return List of player rankings for the specified month
     */
    public List<PlayerRankingDTO> getMonthlyPlayerRankings(int year, int month, int limit, int offset, Long clanId) {
        LeaderboardResponseDTO response = getMonthlyPlayerRankingsWithStats(year, month, limit, offset, clanId);
        return response.getRankings();
    }
    
    /**
     * Get monthly player rankings with summary statistics
     * 
     * Caching Strategy:
     * - Past months: Cached indefinitely (data doesn't change)
     * - Current month: Not cached (data changes as new games are processed)
     * 
     * @param year The year (e.g., 2026)
     * @param month The month (1-12)
     * @param limit Maximum number of results to return
     * @param offset Number of results to skip for pagination
     * @param clanId Required clan ID to filter rankings
     * @return LeaderboardResponseDTO with rankings and summary stats
     */
    @Cacheable(value = "monthlyLeaderboard", 
               key = "#year + '-' + #month + '-' + #limit + '-' + #offset + '-' + #clanId",
               condition = "T(java.time.LocalDate).of(#year, #month, 1).isBefore(T(java.time.LocalDate).now().withDayOfMonth(1)) && #result != null && #result.totalPlayers > 0")
    public LeaderboardResponseDTO getMonthlyPlayerRankingsWithStats(int year, int month, int limit, int offset, Long clanId) {
        try {
            // Get clan's appServerId
            Optional<Clan> clanOpt = clanService.getClanById(clanId);
            if (clanOpt.isEmpty()) {
                LOGGER.warn("Clan not found: {}", clanId);
                return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
            }
            Long appServerId = clanOpt.get().getAppServerId();
            
            // Calculate month boundaries in UTC
            LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1); // Last second of the month
            
            Instant startInstant = startOfMonth.toInstant(ZoneOffset.UTC);
            Instant endInstant = endOfMonth.toInstant(ZoneOffset.UTC);
            
            LOGGER.info("Querying monthly leaderboard for {}-{} ({} to {}) for clan {}", year, month, startInstant, endInstant, clanId);
            
            // Get all games in the month, filtered by clan's appServerId
            List<GameEntity> gamesInMonth = gameRepository.findGamesByMonthRange(startInstant, endInstant).stream()
                    .filter(game -> game.getAppServerId().equals(appServerId))
                    .collect(Collectors.toList());
            if (gamesInMonth.isEmpty()) {
                LOGGER.info("No games found for month {}-{}", year, month);
                return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
            }
            
            // Get all game IDs
            List<Long> gameIds = gamesInMonth.stream()
                    .map(GameEntity::getId)
                    .collect(Collectors.toList());
            
            // Get all ROUND_END events for these games
            List<RoundEndEventEntity> roundEndEvents = gameEventRepository.findRoundEndEventsByGameIds(gameIds);
            LOGGER.info("Found {} ROUND_END events for {} games in month {}-{}", roundEndEvents.size(), gameIds.size(), year, month);
            
            // Count rounds per player from ROUND_END events
            Map<String, Integer> roundsPerPlayer = countRoundsPerPlayerFromRoundEndEvents(roundEndEvents);
            LOGGER.info("Calculated rounds for {} players from ROUND_END events", roundsPerPlayer.size());
            
            // Get all records within the month for other stats (kills, deaths, etc.)
            List<PlayerStatsEntity> monthRecords = playerStatsRepository.findStatsByMonthRange(startInstant, endInstant);
            
            if (monthRecords.isEmpty()) {
                LOGGER.info("No player stats records found for month {}-{}", year, month);
                return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
            }
            
            // Group records by player ID
            Map<String, List<PlayerStatsEntity>> playerRecordsMap = monthRecords.stream()
                    .collect(Collectors.groupingBy(PlayerStatsEntity::getPlayerId));
            
            List<PlayerStats> monthlyStatsList = new ArrayList<>();
            
            // For each player, compute month-only stats
            for (Map.Entry<String, List<PlayerStatsEntity>> entry : playerRecordsMap.entrySet()) {
                String playerId = entry.getKey();
                List<PlayerStatsEntity> playerRecords = entry.getValue();
                
                // Sort by gameTimestamp ascending
                playerRecords.sort((a, b) -> a.getGameTimestamp().compareTo(b.getGameTimestamp()));
                
                // Get baseline stats (latest record BEFORE the month started)
                // This is critical: we need the cumulative stats at the START of the month
                PlayerStatsEntity baselineEntity = null;
                List<PlayerStatsEntity> beforeMonth = playerStatsRepository.findLatestStatsBeforeDate(playerId, startInstant);
                if (!beforeMonth.isEmpty()) {
                    baselineEntity = beforeMonth.get(0); // Already sorted DESC, so first is latest
                    // Sanity check: ensure baseline is actually before the month start
                    if (baselineEntity.getGameTimestamp().isAfter(startInstant) || baselineEntity.getGameTimestamp().equals(startInstant)) {
                        LOGGER.warn("Baseline record for player {} has timestamp {} which is not before month start {}", 
                                playerId, baselineEntity.getGameTimestamp(), startInstant);
                        baselineEntity = null; // Don't use invalid baseline
                    }
                }
                
                // Get the latest record in the month by timestamp (this should have the highest cumulative stats)
                // This represents the player's cumulative stats at the END of the month
                // Since records are cumulative and created at game end, the latest timestamp = highest stats
                PlayerStatsEntity latestEntityInMonth = playerRecords.get(playerRecords.size() - 1);
                
                // Also find max by roundsPlayed as a sanity check
                PlayerStatsEntity maxByRoundsEntity = playerRecords.stream()
                        .max((a, b) -> Integer.compare(a.getRoundsPlayed(), b.getRoundsPlayed()))
                        .orElse(latestEntityInMonth);
                
                // Use the one with higher roundsPlayed (should be the latest, but verify)
                PlayerStatsEntity endOfMonthEntity = (maxByRoundsEntity.getRoundsPlayed() >= latestEntityInMonth.getRoundsPlayed()) 
                        ? maxByRoundsEntity 
                        : latestEntityInMonth;
                
                // Always use baseline before month (or null/0 if no pre-month record exists)
                // DO NOT use first record in month as baseline - that would exclude the first game's stats
                // Example: If player had 100 rounds before Nov, and plays 34 games in Nov:
                //   - First game record: 116 rounds (100 + 16)
                //   - Last game record: 644 rounds (100 + 34*16)
                //   - Month-only should be: 644 - 100 = 544 rounds (correct)
                //   - NOT: 644 - 116 = 528 rounds (wrong - missing first game)
                
                // Log for debugging if rounds seem incorrect
                if (baselineEntity != null && endOfMonthEntity.getRoundsPlayed() < baselineEntity.getRoundsPlayed()) {
                    LOGGER.warn("Monthly stats calculation: Player {} has decreasing rounds (baseline: {}, end of month: {})", 
                            playerId, baselineEntity.getRoundsPlayed(), endOfMonthEntity.getRoundsPlayed());
                }
                
                // Log detailed info for debugging - especially for cases with many games but few rounds
                int baselineRounds = (baselineEntity != null) ? baselineEntity.getRoundsPlayed() : 0;
                int endRounds = endOfMonthEntity.getRoundsPlayed();
                int calculatedRounds = endRounds - baselineRounds;
                
                // Get games count for logging (will be used later for DTO conversion)
                long playerGamesInMonth = playerStatsRepository.countDistinctGamesByPlayerIdInMonth(playerId, startInstant, endInstant);
                
                // Warn if games count is high but rounds are suspiciously low (before we override with ROUND_END count)
                if (playerGamesInMonth > 10 && calculatedRounds < playerGamesInMonth * 10) {
                    LOGGER.warn("Monthly stats calculation: Player {} has {} games but calculated difference shows only {} rounds. Baseline: {}, End: {}, Records in month: {}", 
                            playerId, playerGamesInMonth, calculatedRounds, baselineRounds, endRounds, playerRecords.size());
                }
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Player {}: {} games, baseline rounds: {}, end rounds: {}, calculated rounds: {}, records: {}", 
                            playerId, playerGamesInMonth, baselineRounds, endRounds, calculatedRounds, playerRecords.size());
                }
                
                // Compute month-only stats: endOfMonthRecord - baselineBeforeMonth (or 0)
                PlayerStats monthOnlyStats = computeMonthOnlyStats(baselineEntity, endOfMonthEntity);
                
                // Override roundsPlayed with the accurate count from ROUND_END events
                int roundsFromEvents = roundsPerPlayer.getOrDefault(playerId, 0);
                monthOnlyStats.setRoundsPlayed(roundsFromEvents);
                
                // Log if there's a discrepancy between ROUND_END count and calculated difference
                if (roundsFromEvents != calculatedRounds && roundsFromEvents > 0) {
                    LOGGER.debug("Player {}: ROUND_END events show {} rounds, calculated difference shows {} rounds", 
                            playerId, roundsFromEvents, calculatedRounds);
                }
                
                // Only include players with activity in the month
                if (hasActivity(monthOnlyStats)) {
                    // Recalculate rank based on month-only stats
                    int newRank = rankingAlgorithm.calculateRank(monthOnlyStats);
                    monthOnlyStats.setRank(newRank);
                    monthOnlyStats.setPlayerId(playerId);
                    monthOnlyStats.setLastSeenNickname(endOfMonthEntity.getLastSeenNickname());
                    monthlyStatsList.add(monthOnlyStats);
                }
            }
            
            // Sort by rank descending (higher rank = better)
            monthlyStatsList.sort((p1, p2) -> Integer.compare(p2.getRank(), p1.getRank()));
            
            // Apply pagination
            int fromIndex = Math.min(offset, monthlyStatsList.size());
            int toIndex = Math.min(offset + limit, monthlyStatsList.size());
            
            // Handle case where offset is beyond list size
            if (fromIndex >= monthlyStatsList.size()) {
                long totalGamesInMonth = playerStatsRepository.countTotalDistinctGamesInMonth(startInstant, endInstant);
                return new LeaderboardResponseDTO(new ArrayList<>(), totalGamesInMonth, 0, monthlyStatsList.size());
            }
            
            List<PlayerStats> paginatedStats = monthlyStatsList.subList(fromIndex, toIndex);
            
            LOGGER.info("Computed monthly leaderboard: {} players total, returning {} (offset: {}, limit: {})", 
                    monthlyStatsList.size(), paginatedStats.size(), offset, limit);
            
            // Batch query games counts for all players to avoid N+1 query problem
            List<String> playerIds = paginatedStats.stream()
                    .map(PlayerStats::getPlayerId)
                    .collect(Collectors.toList());
            
            Map<String, Long> gamesCountMap = new HashMap<>();
            if (!playerIds.isEmpty()) {
                List<Object[]> gamesCounts = playerStatsRepository.countDistinctGamesByPlayerIdsInMonth(
                        playerIds, startInstant, endInstant);
                for (Object[] result : gamesCounts) {
                    String playerId = (String) result[0];
                    Long gameCount = (Long) result[1];
                    gamesCountMap.put(playerId, gameCount);
                }
            }
            
            // Convert to DTOs with games played count for the month
            List<PlayerRankingDTO> dtos = new ArrayList<>();
            for (PlayerStats stats : paginatedStats) {
                String playerId = stats.getPlayerId();
                // Get games count from batch query result (default to 0 if not found)
                long gamesPlayedInMonth = gamesCountMap.getOrDefault(playerId, 0L);
                PlayerRankingDTO dto = convertToDTO(stats, (int) gamesPlayedInMonth);
                dtos.add(dto);
            }
            
            // Calculate summary statistics for the month
            long totalGamesInMonth = playerStatsRepository.countTotalDistinctGamesInMonth(startInstant, endInstant);
            // Calculate total rounds from games in the month, not from player stats (to avoid double-counting)
            long totalRoundsInMonth = gameRepository.calculateTotalRoundsInMonth(startInstant, endInstant);
            
            return new LeaderboardResponseDTO(dtos, totalGamesInMonth, totalRoundsInMonth, monthlyStatsList.size());
                    
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve monthly player rankings for {}-{}", year, month, e);
            return new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0);
        }
    }
    
    /**
     * Compute month-only stats by subtracting baseline stats from end-of-month stats
     */
    private PlayerStats computeMonthOnlyStats(PlayerStatsEntity baseline, PlayerStatsEntity endOfMonth) {
        PlayerStats monthStats = new PlayerStats();
        
        int baselineKills = (baseline != null) ? baseline.getKills() : 0;
        int baselineDeaths = (baseline != null) ? baseline.getDeaths() : 0;
        int baselineAssists = (baseline != null) ? baseline.getAssists() : 0;
        int baselineHeadshotKills = (baseline != null) ? baseline.getHeadshotKills() : 0;
        int baselineRoundsPlayed = (baseline != null) ? baseline.getRoundsPlayed() : 0;
        int baselineClutchesWon = (baseline != null) ? baseline.getClutchesWon() : 0;
        double baselineDamageDealt = (baseline != null) ? baseline.getDamageDealt() : 0.0;
        
        monthStats.setKills(Math.max(0, endOfMonth.getKills() - baselineKills));
        monthStats.setDeaths(Math.max(0, endOfMonth.getDeaths() - baselineDeaths));
        monthStats.setAssists(Math.max(0, endOfMonth.getAssists() - baselineAssists));
        monthStats.setHeadshotKills(Math.max(0, endOfMonth.getHeadshotKills() - baselineHeadshotKills));
        monthStats.setRoundsPlayed(Math.max(0, endOfMonth.getRoundsPlayed() - baselineRoundsPlayed));
        monthStats.setClutchesWon(Math.max(0, endOfMonth.getClutchesWon() - baselineClutchesWon));
        monthStats.setDamageDealt(Math.max(0.0, endOfMonth.getDamageDealt() - baselineDamageDealt));
        
        return monthStats;
    }
    
    /**
     * Check if player has any activity in the month
     */
    private boolean hasActivity(PlayerStats stats) {
        return stats.getKills() > 0 || stats.getDeaths() > 0 || stats.getRoundsPlayed() > 0;
    }
    
    /**
     * Count rounds per player from ROUND_END events
     * Each ROUND_END event contains a list of players who participated in that round.
     * This method counts how many ROUND_END events each player appears in.
     * 
     * IMPORTANT: ROUND_END events store player IDs in numeric format (e.g., "1090227400"),
     * but PlayerStatsEntity stores them in full format (e.g., "[U:1:1090227400]").
     * This method normalizes IDs to full format to match PlayerStatsEntity format.
     * 
     * @param roundEndEvents List of ROUND_END events for games in the month
     * @return Map of playerId (full format) -> number of rounds played
     */
    private Map<String, Integer> countRoundsPerPlayerFromRoundEndEvents(List<RoundEndEventEntity> roundEndEvents) {
        Map<String, Integer> roundsPerPlayer = new HashMap<>();
        
        for (RoundEndEventEntity event : roundEndEvents) {
            String playersJson = event.getPlayersJson();
            if (playersJson == null || playersJson.trim().isEmpty()) {
                continue;
            }
            
            try {
                // Parse JSON array of player IDs
                // The players field is stored as JSON array of numeric IDs: ["1090227400", "1234567890", ...]
                // OR full format: ["[U:1:1090227400]", "[U:1:1234567890]", ...]
                List<String> players = objectMapper.readValue(playersJson, new TypeReference<List<String>>() {});
                
                // Count this round for each player
                for (String playerId : players) {
                    if (playerId == null || playerId.trim().isEmpty() || "0".equals(playerId)) {
                        continue;
                    }
                    
                    // Normalize player ID to full format to match PlayerStatsEntity format
                    String normalizedPlayerId = normalizePlayerId(playerId);
                    roundsPerPlayer.merge(normalizedPlayerId, 1, Integer::sum);
                }
            } catch (Exception e) {
                // If JSON parsing fails, try CSV format as fallback
                try {
                    String[] players = playersJson.split(",");
                    for (String playerId : players) {
                        playerId = playerId.trim();
                        if (playerId.isEmpty() || "0".equals(playerId)) {
                            continue;
                        }
                        String normalizedPlayerId = normalizePlayerId(playerId);
                        roundsPerPlayer.merge(normalizedPlayerId, 1, Integer::sum);
                    }
                } catch (Exception e2) {
                    LOGGER.warn("Failed to parse players from ROUND_END event {}: {}", event.getId(), playersJson, e);
                }
            }
        }
        
        LOGGER.debug("Counted rounds for {} players from {} ROUND_END events", roundsPerPlayer.size(), roundEndEvents.size());
        return roundsPerPlayer;
    }
    
    /**
     * Normalize player ID to full format [U:1:XXXXX]
     * Handles both numeric format (e.g., "1090227400") and full format (e.g., "[U:1:1090227400]")
     * 
     * @param playerId Player ID in either format
     * @return Player ID in full format [U:1:XXXXX]
     */
    private String normalizePlayerId(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return playerId;
        }
        
        String trimmed = playerId.trim();
        
        // If already in full format, return as-is
        if (trimmed.startsWith("[U:1:") && trimmed.endsWith("]")) {
            return trimmed;
        }
        
        // If numeric format, convert to full format
        // Remove any brackets if present
        String numericId = trimmed.replaceAll("^\\[U:1:|\\]$", "").trim();
        
        // Validate it's numeric
        if (numericId.matches("\\d+")) {
            return "[U:1:" + numericId + "]";
        }
        
        // If it doesn't match expected format, return as-is (might be a name or other format)
        LOGGER.warn("Unexpected player ID format: {}", playerId);
        return trimmed;
    }
    
    /**
     * Evicts all cache entries for a specific clan after new games are processed.
     * This ensures that leaderboard data is refreshed when new games are added.
     * 
     * @param clanId The clan ID whose cache should be evicted
     */
    public void evictCacheForClan(Long clanId) {
        if (clanId == null) {
            LOGGER.warn("🗑️ [CACHE] Cannot evict cache: clanId is null");
            return;
        }
        
        LOGGER.info("🗑️ [CACHE] Starting cache eviction for clan {}", clanId);
        
        // Evict all-time leaderboard cache
        Cache allTimeCache = cacheManager.getCache("allTimeLeaderboard");
        if (allTimeCache != null) {
            String cacheKey = "clan-" + clanId;
            Cache.ValueWrapper beforeEvict = allTimeCache.get(cacheKey);
            if (beforeEvict != null) {
                LOGGER.info("🗑️ [CACHE] Found cached entry for key: {} - evicting", cacheKey);
            } else {
                LOGGER.info("🗑️ [CACHE] No cached entry found for key: {}", cacheKey);
            }
            allTimeCache.evict(cacheKey);
            Cache.ValueWrapper afterEvict = allTimeCache.get(cacheKey);
            if (afterEvict == null) {
                LOGGER.info("🗑️ [CACHE] Successfully evicted all-time leaderboard cache for clan {} (key: {})", clanId, cacheKey);
            } else {
                LOGGER.warn("🗑️ [CACHE] Cache eviction may have failed - entry still exists for key: {}", cacheKey);
            }
        } else {
            LOGGER.warn("🗑️ [CACHE] Cache 'allTimeLeaderboard' not found!");
        }
        
        // Evict top N leaderboard cache for common limits (1-100)
        Cache topCache = cacheManager.getCache("topLeaderboard");
        if (topCache != null) {
            for (int limit = 1; limit <= 100; limit++) {
                topCache.evict(limit + "-" + clanId);
            }
            LOGGER.debug("Evicted top N leaderboard cache for clan {} (limits 1-100)", clanId);
        }
        
        // Evict monthly leaderboard cache for current month and recent months
        // We evict current month and past 3 months to be safe
        Cache monthlyCache = cacheManager.getCache("monthlyLeaderboard");
        if (monthlyCache != null) {
            LocalDate now = LocalDate.now();
            for (int monthOffset = 0; monthOffset <= 3; monthOffset++) {
                LocalDate targetDate = now.minusMonths(monthOffset);
                int year = targetDate.getYear();
                int month = targetDate.getMonthValue();
                
                // Evict for common limit/offset combinations
                for (int limit = 10; limit <= 1000; limit *= 10) {
                    for (int offset = 0; offset <= 1000; offset += 100) {
                        String key = year + "-" + month + "-" + limit + "-" + offset + "-" + clanId;
                        monthlyCache.evict(key);
                    }
                }
            }
            LOGGER.debug("Evicted monthly leaderboard cache for clan {} (current month and past 3 months)", clanId);
        }
        
        LOGGER.info("Completed evicting leaderboard cache for clan {}", clanId);
    }
    
    /**
     * Evicts all cache entries for all clans.
     * Use with caution - this clears all leaderboard caches.
     */
    @CacheEvict(value = {"allTimeLeaderboard", "topLeaderboard", "monthlyLeaderboard"}, allEntries = true)
    public void evictAllLeaderboardCache() {
        LOGGER.info("Evicting all leaderboard caches");
    }
}