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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.GameEventType;
import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.AssistEventEntity;
import com.rankforge.pipeline.persistence.entity.AttackEventEntity;
import com.rankforge.pipeline.persistence.entity.BombEventEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.KillEventEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.entity.RoundStartEventEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.AccoladeDTO;
import com.rankforge.server.dto.GameDTO;
import com.rankforge.server.dto.GameDetailsDTO;
import com.rankforge.server.dto.PlayerStatsDTO;
import com.rankforge.server.dto.RoundDetailsDTO;
import com.rankforge.server.dto.RoundEventDTO;
import com.rankforge.server.dto.RoundResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * Service layer for managing processed games data.
 * Uses the Game table as the source of truth for game definitions.
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class GameService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);
    
    private final ObjectMapper objectMapper;
    private final GameEventRepository gameEventRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final GameRepository gameRepository;
    private final AccoladeRepository accoladeRepository;
    
    @Autowired
    public GameService(ObjectMapper objectMapper, 
                       GameEventRepository gameEventRepository,
                       PlayerStatsRepository playerStatsRepository,
                       GameRepository gameRepository,
                       AccoladeRepository accoladeRepository) {
        this.objectMapper = objectMapper;
        this.gameEventRepository = gameEventRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.gameRepository = gameRepository;
        this.accoladeRepository = accoladeRepository;
    }

    /**
     * Get all processed games from the Game table
     */
    public List<GameDTO> getAllGames() {
        try {
            List<GameEntity> gameEntities = gameRepository.findAll();
            Map<String, String> playerIdToNameCache = new HashMap<>();
            
            List<GameDTO> games = new ArrayList<>();
            LOGGER.info("Processing {} games from database", gameEntities.size());
            
            for (GameEntity gameEntity : gameEntities) {
                long start = System.currentTimeMillis();
                
                // Get players for this game using gameId
                List<String> players = getPlayersForGame(gameEntity.getId(), playerIdToNameCache);
                
                // Extract duration
                String duration = gameEntity.getDuration() != null 
                    ? String.valueOf(gameEntity.getDuration()) 
                    : null;
                
                GameDTO gameDTO = new GameDTO(
                        gameEntity.getId(),
                        gameEntity.getGameOverTimestamp(),
                        gameEntity.getMap(),
                        gameEntity.getMode(),
                        gameEntity.getTeam1Score(),
                        gameEntity.getTeam2Score(),
                        players,
                        duration
                );
                games.add(gameDTO);
                LOGGER.info("Game processing id={} took {}ms, duration: {} min", 
                        gameEntity.getId(), System.currentTimeMillis() - start, duration);
            }
            
            // Sort by game date descending (most recent first)
            games.sort((g1, g2) -> g2.getGameDate().compareTo(g1.getGameDate()));
            
            return games;
                    
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve games", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get players for a game using the game's database ID
     * Returns a map of steam ID -> player name
     */
    private Map<String, String> getPlayersForGameWithIds(Long gameId, Map<String, String> playerIdToNameCache) {
        Map<String, String> steamIdToName = new LinkedHashMap<>();
        
        try {
            // Get all rounds for this game
            List<RoundEndEventEntity> rounds = gameEventRepository.findRoundEndEventsByGameId(gameId);
            
            LOGGER.debug("Found {} rounds for game {}", rounds.size(), gameId);
            
            // Extract players from rounds
            for (RoundEndEventEntity roundEntity : rounds) {
                String playersJson = roundEntity.getPlayersJson();
                if (playersJson != null && !playersJson.isEmpty()) {
                    try {
                        JsonNode playersNode = objectMapper.readTree(playersJson);
                        // Players JSON is stored as an array of player IDs
                        if (playersNode.isArray()) {
                            for (JsonNode playerIdNode : playersNode) {
                                String playerId = playerIdNode.asText();
                                if (playerId != null && !playerId.isEmpty() && !"0".equals(playerId)) {
                                    String fullSteamId = "[U:1:" + playerId + "]";
                                    String playerName = getPlayerNameById(playerIdToNameCache, playerId);
                                    if (playerName != null && !playerName.trim().isEmpty()) {
                                        String trimmedName = playerName.trim();
                                        if (!steamIdToName.containsKey(fullSteamId)) {
                                            steamIdToName.put(fullSteamId, trimmedName);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        LOGGER.warn("Failed to parse players JSON for round {}", roundEntity.getId(), e);
                    }
                }
            }
            
            LOGGER.debug("Found {} unique players for game {}", steamIdToName.size(), gameId);
            return steamIdToName;
            
        } catch (Exception e) {
            LOGGER.error("Failed to get players for game {}", gameId, e);
            return new LinkedHashMap<>();
        }
    }
    
    /**
     * Get players for a game using the game's database ID
     */
    private List<String> getPlayersForGame(Long gameId, Map<String, String> playerIdToNameCache) {
        Map<String, String> steamIdToName = getPlayersForGameWithIds(gameId, playerIdToNameCache);
        List<String> playerList = new ArrayList<>(steamIdToName.values());
        playerList.sort(String.CASE_INSENSITIVE_ORDER);
        return playerList;
    }

    /**
     * Get top N most recent games
     */
    public List<GameDTO> getRecentGames(int limit) {
        return getAllGames().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve player name by ID from PlayerStats table
     */
    private String getPlayerNameById(Map<String, String> playerIdToNameCache, String playerId) {
        if (playerIdToNameCache.containsKey(playerId)) {
            return playerIdToNameCache.get(playerId);
        }

        try {
            // Convert the player ID format from round end events to full steam ID
            String fullSteamId = "[U:1:" + playerId + "]";
            
            Optional<PlayerStatsEntity> entityOpt = playerStatsRepository.findByPlayerId(fullSteamId);
            if (entityOpt.isPresent()) {
                PlayerStatsEntity entity = entityOpt.get();
                String nick = entity.getLastSeenNickname();
                if (nick != null && !nick.isEmpty()) {
                    playerIdToNameCache.put(playerId, nick);
                    return nick;
                }
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            LOGGER.debug("PlayerStats table does not exist yet for player ID {}", playerId);
        } catch (org.springframework.dao.DataAccessException e) {
            LOGGER.warn("Database access error while retrieving player name for ID {}", playerId);
        } catch (Exception e) {
            LOGGER.warn("Could not find player name for ID {}: {}", playerId, e.getMessage());
        }
        
        // Return the player ID as fallback
        return "Player " + playerId;
    }
    
    /**
     * Get game by database ID
     */
    public GameDTO getGameById(String gameIdStr) {
        try {
            Long gameId = Long.parseLong(gameIdStr);
            Optional<GameEntity> gameEntityOpt = gameRepository.findById(gameId);
            
            if (gameEntityOpt.isEmpty()) {
                return null;
            }
            
            GameEntity gameEntity = gameEntityOpt.get();
            Map<String, String> playerIdToNameCache = new HashMap<>();
            List<String> players = getPlayersForGame(gameId, playerIdToNameCache);
            
            String duration = gameEntity.getDuration() != null 
                ? String.valueOf(gameEntity.getDuration()) 
                : null;
            
            return new GameDTO(
                    gameId,
                    gameEntity.getGameOverTimestamp(),
                    gameEntity.getMap(),
                    gameEntity.getMode(),
                    gameEntity.getTeam1Score(),
                    gameEntity.getTeam2Score(),
                    players,
                    duration
            );
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse game ID: {}", gameIdStr, e);
            return null;
        }
    }
    
    /**
     * Get detailed game statistics and round information
     */
    public GameDetailsDTO getGameDetails(String gameIdStr) {
        try {
            Long gameId = Long.parseLong(gameIdStr);
            
            Optional<GameEntity> gameEntityOpt = gameRepository.findById(gameId);
            
            if (gameEntityOpt.isEmpty()) {
                LOGGER.warn("GameEntity not found for id {}", gameId);
                return null;
            }
            
            GameEntity gameEntity = gameEntityOpt.get();
            
            // Get rounds using gameId foreign key
            List<RoundResultDTO> rounds = getRoundResults(gameId, 
                    gameEntity.getTeam1Score(), gameEntity.getTeam2Score());
            
            // Create game details with scores from the game entity
            GameDetailsDTO details = new GameDetailsDTO(
                    gameEntity.getTeam1Score(), 
                    gameEntity.getTeam2Score(), 
                    gameEntity.getTeam1Score() + gameEntity.getTeam2Score()
            );
            details.setRounds(rounds);
            
            // Get players for the game (steamId -> playerName)
            Map<String, String> playerIdToNameCache = new HashMap<>();
            Map<String, String> steamIdToName = getPlayersForGameWithIds(gameId, playerIdToNameCache);
            
            // Get player statistics
            List<PlayerStatsDTO> playerStats = getPlayerStatistics(gameId, steamIdToName);
            details.setPlayerStats(playerStats);
            
            // Get accolades
            List<AccoladeDTO> accolades = getAccolades(gameId);
            details.setAccolades(accolades);
            
            return details;
            
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse game ID: {}", gameIdStr, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to get game details for game ID: {}", gameIdStr, e);
            return null;
        }
    }
    
    /**
     * Get round-by-round results for a specific game
     */
    private List<RoundResultDTO> getRoundResults(Long gameId, int finalCtScore, int finalTScore) {
        List<RoundResultDTO> rounds = new ArrayList<>();
        
        try {
            List<RoundEndEventEntity> roundEntities = gameEventRepository.findRoundEndEventsByGameId(gameId);
            
            LOGGER.debug("Found {} rounds for game {}", roundEntities.size(), gameId);
            
            if (roundEntities.isEmpty()) {
                return rounds;
            }
            
            // Sort rounds by timestamp
            roundEntities.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            
            // Work backwards from final scores to determine round winners
            int currentCtScore = finalCtScore;
            int currentTScore = finalTScore;
            
            List<RoundResultDTO> tempRounds = new ArrayList<>();
            
            for (int i = roundEntities.size() - 1; i >= 0; i--) {
                RoundEndEventEntity roundEntity = roundEntities.get(i);
                Instant roundTime = roundEntity.getTimestamp();
                int roundNumber = i + 1;
                
                // Determine winner by working backwards from final scores
                String winnerTeam;
                int roundsRemaining = i + 1;
                int ctWinsRemaining = currentCtScore;
                int tWinsRemaining = currentTScore;
                
                if (ctWinsRemaining > roundsRemaining) {
                    winnerTeam = "CT";
                    currentCtScore--;
                } else if (tWinsRemaining > roundsRemaining) {
                    winnerTeam = "T";
                    currentTScore--;
                } else {
                    double ctRatio = (double) ctWinsRemaining / roundsRemaining;
                    double tRatio = (double) tWinsRemaining / roundsRemaining;
                    winnerTeam = (ctRatio >= tRatio) ? "CT" : "T";
                    if (winnerTeam.equals("CT") && currentCtScore > 0) {
                        currentCtScore--;
                    } else if (winnerTeam.equals("T") && currentTScore > 0) {
                        currentTScore--;
                    }
                }
                
                RoundResultDTO round = new RoundResultDTO(
                    roundNumber,
                    winnerTeam,
                    "unknown",
                    roundTime,
                    currentCtScore,
                    currentTScore
                );
                tempRounds.add(0, round);
            }
            
            rounds.addAll(tempRounds);
            
        } catch (Exception e) {
            LOGGER.error("Failed to get round results for game ID {}", gameId, e);
        }
        
        return rounds;
    }
    
    /**
     * Get player statistics for a game by querying KillEvent and AssistEvent
     * @param gameId The game ID to query events for
     * @param steamIdToName Map of Steam ID -> player name
     */
    private List<PlayerStatsDTO> getPlayerStatistics(Long gameId, Map<String, String> steamIdToName) {
        // Create a map to track stats for each player (keyed by steamId)
        Map<String, PlayerStatsDTO> statsMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : steamIdToName.entrySet()) {
            String steamId = entry.getKey();
            String playerName = entry.getValue();
            statsMap.put(steamId, new PlayerStatsDTO(
                playerName.trim(),
                steamId,
                0, 0, 0, 0.0, "Unknown"
            ));
        }
        
        // Query KillEvent to count kills and deaths using gameId
        try {
            List<GameEventEntity> killEntities = gameEventRepository.findByGameIdAndGameEventType(
                    gameId, GameEventType.KILL);
            
            LOGGER.debug("Found {} kill events for game {}", killEntities.size(), gameId);
            
            for (GameEventEntity entity : killEntities) {
                String killerSteamId = entity.getPlayer1();
                String victimSteamId = entity.getPlayer2();
                
                // Check if it's a headshot kill
                boolean isHeadshot = false;
                if (entity instanceof com.rankforge.pipeline.persistence.entity.KillEventEntity killEvent) {
                    isHeadshot = Boolean.TRUE.equals(killEvent.getIsHeadshot());
                }
                
                if (killerSteamId != null) {
                    // Try exact match first
                    if (statsMap.containsKey(killerSteamId)) {
                        PlayerStatsDTO stats = statsMap.get(killerSteamId);
                        stats.setKills(stats.getKills() + 1);
                        if (isHeadshot) {
                            stats.setHeadshotKills(stats.getHeadshotKills() + 1);
                        }
                    } else {
                        // Fallback to partial match
                        for (Map.Entry<String, PlayerStatsDTO> mapEntry : statsMap.entrySet()) {
                            PlayerStatsDTO stats = mapEntry.getValue();
                            String normalizedName = stats.getPlayerName().toLowerCase();
                            if (stats != null && killerSteamId.contains(normalizedName.substring(0, Math.min(3, normalizedName.length())))) {
                                stats.setKills(stats.getKills() + 1);
                                if (isHeadshot) {
                                    stats.setHeadshotKills(stats.getHeadshotKills() + 1);
                                }
                                break;
                            }
                        }
                    }
                }
                
                if (victimSteamId != null) {
                    // Try exact match first
                    if (statsMap.containsKey(victimSteamId)) {
                        PlayerStatsDTO stats = statsMap.get(victimSteamId);
                        stats.setDeaths(stats.getDeaths() + 1);
                    } else {
                        // Fallback to partial match
                        for (Map.Entry<String, PlayerStatsDTO> mapEntry : statsMap.entrySet()) {
                            PlayerStatsDTO stats = mapEntry.getValue();
                            String normalizedName = stats.getPlayerName().toLowerCase();
                            if (stats != null && victimSteamId.contains(normalizedName.substring(0, Math.min(3, normalizedName.length())))) {
                                stats.setDeaths(stats.getDeaths() + 1);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            LOGGER.debug("GameEvent table does not exist yet, skipping kill events");
        } catch (org.springframework.dao.DataAccessException e) {
            LOGGER.warn("Database access error while retrieving kill events");
        } catch (Exception e) {
            LOGGER.error("Failed to get kill events for game {}", gameId, e);
        }
        
        // Query AssistEvent to count assists using gameId
        try {
            List<GameEventEntity> assistEntities = gameEventRepository.findByGameIdAndGameEventType(
                    gameId, GameEventType.ASSIST);
            
            LOGGER.debug("Found {} assist events for game {}", assistEntities.size(), gameId);
            
            for (GameEventEntity entity : assistEntities) {
                String assisterSteamId = entity.getPlayer1();
                
                if (assisterSteamId != null) {
                    // Try exact match first
                    if (statsMap.containsKey(assisterSteamId)) {
                        PlayerStatsDTO stats = statsMap.get(assisterSteamId);
                        stats.setAssists(stats.getAssists() + 1);
                    } else {
                        // Fallback to partial match
                        for (Map.Entry<String, PlayerStatsDTO> mapEntry : statsMap.entrySet()) {
                            PlayerStatsDTO stats = mapEntry.getValue();
                            String normalizedName = stats.getPlayerName().toLowerCase();
                            if (stats != null && assisterSteamId.contains(normalizedName.substring(0, Math.min(3, normalizedName.length())))) {
                                stats.setAssists(stats.getAssists() + 1);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            LOGGER.debug("GameEvent table does not exist yet, skipping assist events");
        } catch (org.springframework.dao.DataAccessException e) {
            LOGGER.warn("Database access error while retrieving assist events");
        } catch (Exception e) {
            LOGGER.error("Failed to get assist events for game {}", gameId, e);
        }
        
        // Query AttackEvent to calculate damage dealt using gameId
        // NOTE: We calculate actual HP damage by tracking victim HP and using healthRemaining from logs
        try {
            List<GameEventEntity> attackEntities = gameEventRepository.findByGameIdAndGameEventType(
                    gameId, GameEventType.ATTACK);
            
            LOGGER.info("Found {} attack events for game {}", attackEntities.size(), gameId);
            
            // CRITICAL: Sort events by timestamp to ensure HP tracking works correctly
            attackEntities.sort(Comparator.comparing(GameEventEntity::getTimestamp));
            
            // Track each victim's HP throughout the game (resets to 100 each round)
            // Key: victimSteamId_roundStartId, Value: current HP
            Map<String, Integer> victimHPTracker = new HashMap<>();
            
            // Debug: Track damage per player
            Map<String, Integer> damageDebug = new HashMap<>();
            int totalHitsProcessed = 0;
            int hitsWithoutHealthData = 0;
            
            for (GameEventEntity entity : attackEntities) {
                if (entity instanceof com.rankforge.pipeline.persistence.entity.AttackEventEntity attackEvent) {
                    String attackerSteamId = entity.getPlayer1();
                    String victimSteamId = entity.getPlayer2();
                    Integer healthRemaining = attackEvent.getHealthRemaining();
                    
                    // Get round ID for proper HP tracking (HP resets each round)
                    Long roundStartId = (entity.getRoundStart() != null) ? entity.getRoundStart().getId() : 0L;
                    
                    // Calculate actual damage using HP tracking
                    Integer actualDamage = null;
                    
                    if (victimSteamId != null && healthRemaining != null) {
                        // Get victim's HP before this hit (100 at start of each round)
                        String victimKey = victimSteamId + "_" + roundStartId;
                        Integer previousHP = victimHPTracker.getOrDefault(victimKey, 100);
                        
                        // Calculate actual damage dealt = previous HP - remaining HP
                        actualDamage = previousHP - healthRemaining;
                        
                        // Update victim's HP for this round
                        victimHPTracker.put(victimKey, healthRemaining);
                        
                        // Sanity check: damage should be positive and reasonable
                        if (actualDamage < 0 || actualDamage > 100) {
                            // This shouldn't happen with proper round tracking, but fallback to safe calculation
                            actualDamage = Math.max(0, Math.min(100, 100 - healthRemaining));
                            LOGGER.warn("Anomaly in damage calculation for victim {} in round {}: calculated {}, using fallback", 
                                victimSteamId, roundStartId, actualDamage);
                        }
                    } else {
                        hitsWithoutHealthData++;
                    }
                    
                    // Add damage to attacker's total
                    if (attackerSteamId != null && actualDamage != null && actualDamage > 0) {
                        totalHitsProcessed++;
                        
                        // Try exact match first
                        if (statsMap.containsKey(attackerSteamId)) {
                            PlayerStatsDTO stats = statsMap.get(attackerSteamId);
                            stats.setDamage(stats.getDamage() + actualDamage);
                            damageDebug.put(attackerSteamId, damageDebug.getOrDefault(attackerSteamId, 0) + actualDamage);
                        } else {
                            // Fallback to partial match
                            for (Map.Entry<String, PlayerStatsDTO> mapEntry : statsMap.entrySet()) {
                                PlayerStatsDTO stats = mapEntry.getValue();
                                String normalizedName = stats.getPlayerName().toLowerCase();
                                if (stats != null && attackerSteamId.contains(normalizedName.substring(0, Math.min(3, normalizedName.length())))) {
                                    stats.setDamage(stats.getDamage() + actualDamage);
                                    damageDebug.put(attackerSteamId, damageDebug.getOrDefault(attackerSteamId, 0) + actualDamage);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            LOGGER.info("Processed {} attack hits for game {} ({} hits without health data), damage per player: {}", 
                totalHitsProcessed, gameId, hitsWithoutHealthData, damageDebug);
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            LOGGER.debug("GameEvent table does not exist yet, skipping attack events");
        } catch (org.springframework.dao.DataAccessException e) {
            LOGGER.warn("Database access error while retrieving attack events");
        } catch (Exception e) {
            LOGGER.error("Failed to get attack events for game {}", gameId, e);
        }
        
        // Calculate rating (K/D ratio) and headshot percentage
        for (PlayerStatsDTO stats : statsMap.values()) {
            double rating = stats.getDeaths() > 0 
                ? (double) stats.getKills() / stats.getDeaths() 
                : (stats.getKills() > 0 ? stats.getKills() : 0.0);
            stats.setRating(rating);
            
            // Calculate headshot percentage
            double headshotPercentage = stats.getKills() > 0
                ? (double) stats.getHeadshotKills() / stats.getKills() * 100.0
                : 0.0;
            stats.setHeadshotPercentage(headshotPercentage);
        }
        
        // Return stats as a list (order preserved by LinkedHashMap)
        List<PlayerStatsDTO> playerStats = new ArrayList<>(statsMap.values());
        
        LOGGER.debug("Calculated statistics for {} players", playerStats.size());
        
        return playerStats;
    }
    
    /**
     * Get accolades for a game
     */
    private List<AccoladeDTO> getAccolades(Long gameId) {
        List<AccoladeDTO> accolades = new ArrayList<>();
        
        // Cache for player name to Steam ID lookup
        Map<String, String> playerNameToSteamId = new HashMap<>();
        
        try {
            List<AccoladeEntity> accoladeEntities = accoladeRepository.findByGameId(gameId);
            
            for (AccoladeEntity entity : accoladeEntities) {
                // Look up actual Steam ID from player name
                String steamId = resolvePlayerSteamId(entity.getPlayerName(), playerNameToSteamId);
                
                AccoladeDTO accoladeDTO = new AccoladeDTO(
                        entity.getType(),
                        entity.getPlayerName(),
                        steamId,
                        entity.getValue(),
                        entity.getPosition(),
                        entity.getScore()
                );
                accolades.add(accoladeDTO);
            }
            
            LOGGER.debug("Extracted {} accolades for game ID {}", accolades.size(), gameId);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to extract accolades for game ID {}", gameId, e);
        }
        
        return accolades;
    }
    
    /**
     * Resolve player Steam ID from player name using PlayerStats table
     */
    private String resolvePlayerSteamId(String playerName, Map<String, String> cache) {
        if (playerName == null || playerName.isEmpty()) {
            return "";
        }
        
        // Check cache first
        String normalizedName = playerName.trim().toLowerCase();
        if (cache.containsKey(normalizedName)) {
            return cache.get(normalizedName);
        }
        
        try {
            // Look up by nickname in PlayerStats
            Optional<PlayerStatsEntity> entityOpt = playerStatsRepository.findByLastSeenNickname(playerName.trim());
            if (entityOpt.isPresent()) {
                String steamId = entityOpt.get().getPlayerId();
                cache.put(normalizedName, steamId);
                return steamId;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not find Steam ID for player: {}", playerName);
        }
        
        cache.put(normalizedName, "");
        return "";
    }
    
    /**
     * Get detailed round information including all events for a specific round
     * @param gameIdStr The game ID
     * @param roundNumber The round number (1-indexed)
     * @return RoundDetailsDTO with all events in the round
     */
    public RoundDetailsDTO getRoundDetails(String gameIdStr, int roundNumber) {
        try {
            Long gameId = Long.parseLong(gameIdStr);
            
            Optional<GameEntity> gameEntityOpt = gameRepository.findById(gameId);
            if (gameEntityOpt.isEmpty()) {
                LOGGER.warn("Game not found for id {}", gameId);
                return null;
            }
            
            GameEntity gameEntity = gameEntityOpt.get();
            
            // Get all round start events for this game to find the specific round
            List<GameEventEntity> roundStartEvents = gameEventRepository.findByGameIdAndGameEventType(
                    gameId, GameEventType.ROUND_START);
            
            // Sort by timestamp to get correct round order
            roundStartEvents.sort(Comparator.comparing(GameEventEntity::getTimestamp));
            
            if (roundNumber < 1 || roundNumber > roundStartEvents.size()) {
                LOGGER.warn("Invalid round number {} for game {} (has {} rounds)", 
                        roundNumber, gameId, roundStartEvents.size());
                return null;
            }
            
            // Get the round start event for the requested round (0-indexed in list)
            GameEventEntity roundStartEvent = roundStartEvents.get(roundNumber - 1);
            Long roundStartId = roundStartEvent.getId();
            Instant roundStartTime = roundStartEvent.getTimestamp();
            
            // Get round end time
            List<RoundEndEventEntity> roundEndEvents = gameEventRepository.findRoundEndEventsByGameId(gameId);
            roundEndEvents.sort(Comparator.comparing(GameEventEntity::getTimestamp));
            
            Instant roundEndTime = null;
            if (roundNumber <= roundEndEvents.size()) {
                roundEndTime = roundEndEvents.get(roundNumber - 1).getTimestamp();
            }
            
            // Determine winner team
            String winnerTeam = determineRoundWinner(gameEntity, roundNumber, roundStartEvents.size());
            
            // Create round details DTO
            RoundDetailsDTO roundDetails = new RoundDetailsDTO(gameId, roundNumber, winnerTeam);
            roundDetails.setRoundStartTime(roundStartTime);
            roundDetails.setRoundEndTime(roundEndTime);
            
            if (roundStartTime != null && roundEndTime != null) {
                roundDetails.setDurationMs(roundEndTime.toEpochMilli() - roundStartTime.toEpochMilli());
            }
            
            // Get all events for this round
            List<GameEventEntity> roundEvents = gameEventRepository.findByRoundStartId(roundStartId);
            
            // Build player name cache
            Map<String, String> playerIdToNameCache = new HashMap<>();
            Map<String, String> steamIdToName = getPlayersForGameWithIds(gameId, playerIdToNameCache);
            
            // Convert events to DTOs
            List<RoundEventDTO> eventDTOs = new ArrayList<>();
            int killCount = 0;
            int assistCount = 0;
            int headshotCount = 0;
            boolean bombPlanted = false;
            boolean bombDefused = false;
            boolean bombExploded = false;
            
            for (GameEventEntity event : roundEvents) {
                // Skip ROUND_START and ROUND_END events themselves
                if (event.getGameEventType() == GameEventType.ROUND_START || 
                    event.getGameEventType() == GameEventType.ROUND_END) {
                    continue;
                }
                
                RoundEventDTO eventDTO = convertToEventDTO(event, roundStartTime, steamIdToName);
                eventDTOs.add(eventDTO);
                
                // Track summary stats
                switch (event.getGameEventType()) {
                    case KILL:
                        killCount++;
                        if (event instanceof KillEventEntity killEvent && 
                            Boolean.TRUE.equals(killEvent.getIsHeadshot())) {
                            headshotCount++;
                        }
                        break;
                    case ASSIST:
                        assistCount++;
                        break;
                    case BOMB_EVENT:
                        if (event instanceof BombEventEntity bombEvent) {
                            String bombType = bombEvent.getEventType();
                            if (bombType != null) {
                                if (bombType.equalsIgnoreCase("planted")) bombPlanted = true;
                                if (bombType.equalsIgnoreCase("defused")) bombDefused = true;
                                if (bombType.equalsIgnoreCase("exploded")) bombExploded = true;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            
            // Sort events by timestamp
            eventDTOs.sort(Comparator.comparing(RoundEventDTO::getTimestamp));
            
            roundDetails.setEvents(eventDTOs);
            roundDetails.setTotalKills(killCount);
            roundDetails.setTotalAssists(assistCount);
            roundDetails.setHeadshotKills(headshotCount);
            roundDetails.setBombPlanted(bombPlanted);
            roundDetails.setBombDefused(bombDefused);
            roundDetails.setBombExploded(bombExploded);
            
            LOGGER.info("Found {} events for game {} round {}", eventDTOs.size(), gameId, roundNumber);
            
            return roundDetails;
            
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse game ID: {}", gameIdStr, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to get round details for game {} round {}", gameIdStr, roundNumber, e);
            return null;
        }
    }
    
    /**
     * Convert a GameEventEntity to RoundEventDTO
     */
    private RoundEventDTO convertToEventDTO(GameEventEntity event, Instant roundStartTime, 
                                            Map<String, String> steamIdToName) {
        RoundEventDTO dto = new RoundEventDTO(
                event.getId(),
                event.getGameEventType().name(),
                event.getTimestamp()
        );
        
        // Calculate time offset from round start
        if (roundStartTime != null && event.getTimestamp() != null) {
            dto.setTimeOffsetMs(event.getTimestamp().toEpochMilli() - roundStartTime.toEpochMilli());
        }
        
        // Set player info
        String player1Id = event.getPlayer1();
        String player2Id = event.getPlayer2();
        
        if (player1Id != null) {
            dto.setPlayer1Id(player1Id);
            dto.setPlayer1Name(resolvePlayerName(player1Id, steamIdToName));
        }
        if (player2Id != null) {
            dto.setPlayer2Id(player2Id);
            dto.setPlayer2Name(resolvePlayerName(player2Id, steamIdToName));
        }
        
        // Set event-specific details
        switch (event.getGameEventType()) {
            case KILL:
                if (event instanceof KillEventEntity killEvent) {
                    dto.setWeapon(killEvent.getWeapon());
                    dto.setIsHeadshot(killEvent.getIsHeadshot());
                }
                break;
            case ASSIST:
                if (event instanceof AssistEventEntity assistEvent) {
                    dto.setWeapon(assistEvent.getWeapon());
                    dto.setAssistType(assistEvent.getAssistType());
                }
                break;
            case ATTACK:
                if (event instanceof AttackEventEntity attackEvent) {
                    dto.setWeapon(attackEvent.getWeapon());
                    dto.setDamage(attackEvent.getDamage());
                    dto.setArmorDamage(attackEvent.getArmorDamage());
                    dto.setHitGroup(attackEvent.getHitGroup());
                }
                break;
            case BOMB_EVENT:
                if (event instanceof BombEventEntity bombEvent) {
                    dto.setBombEventType(bombEvent.getEventType());
                    // For bomb events, player is stored differently
                    if (bombEvent.getPlayer() != null) {
                        dto.setPlayer1Id(bombEvent.getPlayer());
                        dto.setPlayer1Name(resolvePlayerName(bombEvent.getPlayer(), steamIdToName));
                    }
                }
                break;
            default:
                break;
        }
        
        return dto;
    }
    
    /**
     * Resolve player name from steam ID
     */
    private String resolvePlayerName(String steamId, Map<String, String> steamIdToName) {
        if (steamId == null) return null;
        
        // Try exact match first
        if (steamIdToName.containsKey(steamId)) {
            return steamIdToName.get(steamId);
        }
        
        // Try to find a partial match (handle different steam ID formats)
        for (Map.Entry<String, String> entry : steamIdToName.entrySet()) {
            if (entry.getKey().contains(steamId) || steamId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Extract numeric part and try again
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[U:1:(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(steamId);
        if (matcher.find()) {
            String numericId = matcher.group(1);
            for (Map.Entry<String, String> entry : steamIdToName.entrySet()) {
                if (entry.getKey().contains(numericId)) {
                    return entry.getValue();
                }
            }
        }
        
        return steamId; // Return the ID if name not found
    }
    
    /**
     * Determine the winner of a specific round
     */
    private String determineRoundWinner(GameEntity game, int roundNumber, int totalRounds) {
        int finalCtScore = game.getTeam1Score();
        int finalTScore = game.getTeam2Score();
        
        // Work backwards to determine round winners
        int ctScore = finalCtScore;
        int tScore = finalTScore;
        
        // Simple heuristic: distribute wins proportionally
        int ctWinsUpToRound = (int) Math.round((double) roundNumber * finalCtScore / totalRounds);
        int tWinsUpToRound = roundNumber - ctWinsUpToRound;
        
        int ctWinsUpToPrevRound = roundNumber > 1 ? 
                (int) Math.round((double) (roundNumber - 1) * finalCtScore / totalRounds) : 0;
        
        return (ctWinsUpToRound > ctWinsUpToPrevRound) ? "CT" : "T";
    }
}
