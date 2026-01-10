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
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.AccoladeDTO;
import com.rankforge.server.dto.GameDTO;
import com.rankforge.server.dto.GameDetailsDTO;
import com.rankforge.server.dto.PlayerStatsDTO;
import com.rankforge.server.dto.RoundResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
     */
    private List<String> getPlayersForGame(Long gameId, Map<String, String> playerIdToNameCache) {
        Map<String, String> normalizedToOriginal = new HashMap<>();
        
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
                                    String playerName = getPlayerNameById(playerIdToNameCache, playerId);
                                    if (playerName != null && !playerName.trim().isEmpty()) {
                                        String trimmedName = playerName.trim();
                                        String normalizedKey = trimmedName.toLowerCase();
                                        if (!normalizedToOriginal.containsKey(normalizedKey)) {
                                            normalizedToOriginal.put(normalizedKey, trimmedName);
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
            
            List<String> playerList = new ArrayList<>(normalizedToOriginal.values());
            playerList.sort(String.CASE_INSENSITIVE_ORDER);
            
            LOGGER.debug("Found {} unique players for game {}", playerList.size(), gameId);
            return playerList;
            
        } catch (Exception e) {
            LOGGER.error("Failed to get players for game {}", gameId, e);
            return new ArrayList<>();
        }
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
            
            // Use game entity's start and end times
            Instant gameEndTime = gameEntity.getGameOverTimestamp();
            Instant gameStartTime = gameEntity.getStartTime();
            if (gameStartTime == null) {
                gameStartTime = gameEndTime.minusSeconds(7200); // Fallback: assume 2 hour game
            }
            
            // Create game details with scores from the game entity
            GameDetailsDTO details = new GameDetailsDTO(
                    gameEntity.getTeam1Score(), 
                    gameEntity.getTeam2Score(), 
                    gameEntity.getTeam1Score() + gameEntity.getTeam2Score()
            );
            details.setRounds(rounds);
            
            // Get players for the game
            Map<String, String> playerIdToNameCache = new HashMap<>();
            List<String> players = getPlayersForGame(gameId, playerIdToNameCache);
            
            // Get player statistics
            List<PlayerStatsDTO> playerStats = getPlayerStatistics(gameStartTime, gameEndTime, players);
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
     */
    private List<PlayerStatsDTO> getPlayerStatistics(Instant gameStartTime, Instant gameEndTime, List<String> players) {
        // Create a map to track stats for each player
        Map<String, PlayerStatsDTO> statsMap = new HashMap<>();
        for (String player : players) {
            String normalizedName = player.trim().toLowerCase();
            statsMap.put(normalizedName, new PlayerStatsDTO(
                player.trim(),
                0, 0, 0, 0.0, "Unknown"
            ));
        }
        
        // Query KillEvent to count kills and deaths
        try {
            List<GameEventEntity> killEntities = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                    GameEventType.KILL, gameStartTime, gameEndTime);
            
            for (GameEventEntity entity : killEntities) {
                Instant eventTime = entity.getTimestamp();
                
                if ((eventTime.equals(gameStartTime) || eventTime.isAfter(gameStartTime)) &&
                    (eventTime.equals(gameEndTime) || eventTime.isBefore(gameEndTime))) {
                    
                    String killerSteamId = entity.getPlayer1();
                    String victimSteamId = entity.getPlayer2();
                    
                    if (killerSteamId != null) {
                        for (Map.Entry<String, PlayerStatsDTO> entry : statsMap.entrySet()) {
                            PlayerStatsDTO stats = entry.getValue();
                            if (stats != null && killerSteamId.contains(entry.getKey().substring(0, Math.min(3, entry.getKey().length())))) {
                                stats.setKills(stats.getKills() + 1);
                                break;
                            }
                        }
                    }
                    
                    if (victimSteamId != null) {
                        for (Map.Entry<String, PlayerStatsDTO> entry : statsMap.entrySet()) {
                            PlayerStatsDTO stats = entry.getValue();
                            if (stats != null && victimSteamId.contains(entry.getKey().substring(0, Math.min(3, entry.getKey().length())))) {
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
            LOGGER.error("Failed to get kill events for game between {} and {}", gameStartTime, gameEndTime, e);
        }
        
        // Query AssistEvent to count assists
        try {
            List<GameEventEntity> assistEntities = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                    GameEventType.ASSIST, gameStartTime, gameEndTime);
            
            for (GameEventEntity entity : assistEntities) {
                Instant eventTime = entity.getTimestamp();
                
                if ((eventTime.equals(gameStartTime) || eventTime.isAfter(gameStartTime)) &&
                    (eventTime.equals(gameEndTime) || eventTime.isBefore(gameEndTime))) {
                    
                    String assisterSteamId = entity.getPlayer1();
                    
                    if (assisterSteamId != null) {
                        for (Map.Entry<String, PlayerStatsDTO> entry : statsMap.entrySet()) {
                            PlayerStatsDTO stats = entry.getValue();
                            if (stats != null && assisterSteamId.contains(entry.getKey().substring(0, Math.min(3, entry.getKey().length())))) {
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
            LOGGER.error("Failed to get assist events for game between {} and {}", gameStartTime, gameEndTime, e);
        }
        
        // Calculate rating (K/D ratio)
        for (PlayerStatsDTO stats : statsMap.values()) {
            double rating = stats.getDeaths() > 0 
                ? (double) stats.getKills() / stats.getDeaths() 
                : (stats.getKills() > 0 ? stats.getKills() : 0.0);
            stats.setRating(rating);
        }
        
        // Return stats in the same order as the players list
        List<PlayerStatsDTO> playerStats = new ArrayList<>();
        for (String player : players) {
            String normalizedName = player.trim().toLowerCase();
            PlayerStatsDTO stats = statsMap.get(normalizedName);
            if (stats != null) {
                playerStats.add(stats);
            } else {
                playerStats.add(new PlayerStatsDTO(
                    player.trim(),
                    0, 0, 0, 0.0, "Unknown"
                ));
            }
        }
        
        LOGGER.debug("Calculated statistics for {} players", playerStats.size());
        
        return playerStats;
    }
    
    /**
     * Get accolades for a game
     */
    private List<AccoladeDTO> getAccolades(Long gameId) {
        List<AccoladeDTO> accolades = new ArrayList<>();
        
        try {
            List<AccoladeEntity> accoladeEntities = accoladeRepository.findByGameId(gameId);
            
            for (AccoladeEntity entity : accoladeEntities) {
                AccoladeDTO accoladeDTO = new AccoladeDTO(
                        entity.getType(),
                        entity.getPlayerName(),
                        entity.getPlayerId(),
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
}
