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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.GameEvent;
import com.rankforge.core.events.GameEventType;
import com.rankforge.core.events.GameOverEvent;
import com.rankforge.core.events.RoundEndEvent;
import com.rankforge.pipeline.persistence.PersistenceLayer;
import com.rankforge.server.dto.GameDTO;
import com.rankforge.server.dto.GameDetailsDTO;
import com.rankforge.server.dto.PlayerStatsDTO;
import com.rankforge.server.dto.RoundResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for managing processed games data
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class GameService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);
    
    private final ObjectMapper objectMapper;
    private final PersistenceLayer persistenceLayer;
    
    @Autowired
    public GameService(ObjectMapper objectMapper, PersistenceLayer persistenceLayer) {
        this.objectMapper = objectMapper;
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Get all processed games with their details
     */
    public List<GameDTO> getAllGames() {
        try {
            List<GameOverEvent> gameOverEvents = getGameOverEventsFromDatabase();
            Map<String, String> playerIdToNameCache = new HashMap<>();
            
            List<GameDTO> games = new ArrayList<>();
            LOGGER.info("Processing {} GameOver events from database", gameOverEvents.size());
            
            for (GameOverEvent gameOverEvent : gameOverEvents) {
                long start = System.currentTimeMillis();
                // Use temporal boundaries to find players for this game
                List<String> players = getPlayersForGame(gameOverEvent, playerIdToNameCache);
                
                // Extract duration from additional data if available
                String duration = gameOverEvent.getAdditionalData().get("duration");
                
                // Generate a unique identifier for this game based on timestamp and map
                String gameId = gameOverEvent.getTimestamp().toEpochMilli() + "_" + gameOverEvent.getMap();
                
                GameDTO gameDTO = new GameDTO(
                        gameId,
                        gameOverEvent.getTimestamp(),
                        gameOverEvent.getMap(),
                        gameOverEvent.getMode(),
                        gameOverEvent.getTeam1Score(),
                        gameOverEvent.getTeam2Score(),
                        players,
                        duration
                );
                games.add(gameDTO);
                LOGGER.info("Game processing {} took {}ms", gameId, System.currentTimeMillis() - start);
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
     * Get top N most recent games
     */
    public List<GameDTO> getRecentGames(int limit) {
        return getAllGames().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get players for a specific game by finding the closest ROUND_END events to the GameOver timestamp
     * Uses database timestamps since Event timestamps appear to be null
     */
    private List<String> getPlayersForGame(GameOverEvent gameOverEvent, Map<String, String> playerIdToNameCache) {
        Set<String> uniquePlayers = new HashSet<>();
        
        Instant gameEndTime = gameOverEvent.getTimestamp();
        int totalRounds = gameOverEvent.getTeam1Score() + gameOverEvent.getTeam2Score();
        
        LOGGER.info("Looking for players for game ending at {} on map: {} ({} total rounds)", 
                gameEndTime, gameOverEvent.getMap(), totalRounds);
        
        // NEW APPROACH: Find ROUND_END events closest to the GameOver timestamp
        // Since Event timestamps are null, use database timestamps and look for temporal clusters
        
        // First, get all ROUND_END events from the day and find the cluster closest to game end time
        String dayStart = gameEndTime.truncatedTo(java.time.temporal.ChronoUnit.DAYS).toString();
        String dayEnd = gameEndTime.truncatedTo(java.time.temporal.ChronoUnit.DAYS).plusSeconds(86400).toString();
        
        LOGGER.info("Searching for ROUND_END events on day {} to group by temporal proximity", dayStart);
        
        try (ResultSet resultSet = persistenceLayer.query("GameEvent",
                new String[]{"at", "event"}, 
                "gameEventType = ? AND at BETWEEN ? AND ? ORDER BY at ASC", 
                GameEventType.ROUND_END.name(), dayStart, dayEnd)) {
            
            List<RoundEndEventWithTime> allRounds = new ArrayList<>();
            
            // Collect all round events with their database timestamps
            while (resultSet.next()) {
                try {
                    String dbTimestamp = resultSet.getString("at");
                    Instant roundTime = parseTimestamp(dbTimestamp);
                    GameEvent event = objectMapper.readValue(resultSet.getString("event"), GameEvent.class);
                    
                    if (event instanceof RoundEndEvent roundEndEvent) {
                        allRounds.add(new RoundEndEventWithTime(roundEndEvent, roundTime));
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse RoundEndEvent", e);
                }
            }
            
            LOGGER.info("Found {} total ROUND_END events on this day", allRounds.size());
            
            if (allRounds.isEmpty()) {
                LOGGER.warn("No ROUND_END events found for game ending at {}", gameEndTime);
                return new ArrayList<>();
            }
            
            // Group rounds into games by temporal proximity (gaps > 5 minutes = new game)
            List<List<RoundEndEventWithTime>> gameGroups = groupRoundsByTemporalProximity(allRounds);
            LOGGER.info("Grouped rounds into {} potential games", gameGroups.size());
            
            // Find the game group closest to our GameOver timestamp
            List<RoundEndEventWithTime> bestMatch = findClosestGameGroup(gameGroups, gameEndTime);
            
            if (bestMatch == null || bestMatch.isEmpty()) {
                LOGGER.warn("No round group found close to game end time {}", gameEndTime);
                return new ArrayList<>();
            }
            
            LOGGER.info("Selected game group with {} rounds spanning {} to {}", 
                    bestMatch.size(), 
                    bestMatch.get(0).timestamp(), 
                    bestMatch.get(bestMatch.size() - 1).timestamp());
            
            // Extract players from the selected game group
            for (RoundEndEventWithTime roundWithTime : bestMatch) {
                roundWithTime.event().getPlayers().forEach(playerId -> {
                    if (!"0".equals(playerId)) { // Exclude bots
                        String playerName = getPlayerNameById(playerIdToNameCache, playerId);
                        if (playerName != null) {
                            uniquePlayers.add(playerName);
                        }
                    }
                });
            }
            
        } catch (SQLException e) {
            LOGGER.error("Failed to get players for game ending at {}", gameEndTime, e);
        }
        
        List<String> playerList = new ArrayList<>(uniquePlayers);
        LOGGER.info("Found {} unique players for game ending at {} on map {}: {}", 
                playerList.size(), gameEndTime, gameOverEvent.getMap(), playerList);
        
        return playerList;
    }
    
    /**
     * Helper record to store RoundEndEvent with its database timestamp
     */
    private record RoundEndEventWithTime(RoundEndEvent event, Instant timestamp) {}
    
    /**
     * Group rounds by temporal proximity - rounds with gaps > 5 minutes are considered separate games
     */
    private List<List<RoundEndEventWithTime>> groupRoundsByTemporalProximity(List<RoundEndEventWithTime> allRounds) {
        List<List<RoundEndEventWithTime>> groups = new ArrayList<>();
        List<RoundEndEventWithTime> currentGroup = new ArrayList<>();
        
        for (RoundEndEventWithTime round : allRounds) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(round);
            } else {
                // If gap > 5 minutes, start new group
                Instant lastRoundTime = currentGroup.get(currentGroup.size() - 1).timestamp();
                long gapSeconds = round.timestamp().getEpochSecond() - lastRoundTime.getEpochSecond();
                
                if (gapSeconds > 300) { // 5 minutes
                    groups.add(new ArrayList<>(currentGroup));
                    currentGroup.clear();
                }
                currentGroup.add(round);
            }
        }
        
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        
        return groups;
    }
    
    /**
     * Find the game group whose end time is closest to the GameOver timestamp
     */
    private List<RoundEndEventWithTime> findClosestGameGroup(List<List<RoundEndEventWithTime>> gameGroups, Instant gameEndTime) {
        List<RoundEndEventWithTime> bestMatch = null;
        long bestDistance = Long.MAX_VALUE;
        
        for (List<RoundEndEventWithTime> group : gameGroups) {
            if (group.isEmpty()) continue;
            
            // Use the last round's timestamp as the game end time
            Instant groupEndTime = group.get(group.size() - 1).timestamp();
            long distance = Math.abs(groupEndTime.getEpochSecond() - gameEndTime.getEpochSecond());
            
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = group;
            }
        }
        
        LOGGER.info("Best match: {} rounds, time distance: {} seconds", 
                bestMatch != null ? bestMatch.size() : 0, bestDistance);
        
        return bestMatch;
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
            
            try (ResultSet resultSet = persistenceLayer.query("PlayerStats", 
                    new String[]{"playerStats"}, "playerId = ?", fullSteamId)) {
                
                if (resultSet.next()) {
                    String playerStatsJson = resultSet.getString("playerStats");
                    // Parse the JSON to extract the lastSeenNickname
                    var playerStats = objectMapper.readTree(playerStatsJson);
                    String nick = playerStats.get("lastSeenNickname").asText();
                    playerIdToNameCache.put(playerId, nick);
                    return nick;
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            LOGGER.warn("Could not find player name for ID {}: {}", playerId, e.getMessage());
        }
        
        // Return the player ID as fallback
        return "Player " + playerId;
    }
    
    /**
     * Get detailed game information by game ID
     */
    public GameDTO getGameById(String gameId) {
        // TODO this is quite un-optimal
        List<GameDTO> allGames = getAllGames();
        return allGames.stream()
                .filter(game -> gameId.equals(game.getGameId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get detailed game statistics and round information
     */
    public GameDetailsDTO getGameDetails(String gameId) {
        GameDTO game = getGameById(gameId);
        if (game == null) {
            return null;
        }
        
        // Parse game ID to get timestamp and map
        String[] parts = gameId.split("_");
        if (parts.length < 2) {
            return null;
        }
        
        try {
            long timestamp = Long.parseLong(parts[0]);
            Instant gameEndTime = Instant.ofEpochMilli(timestamp);
            
            // Calculate game start time (same logic as in getPlayersForGame)
            Instant gameStartTime = gameEndTime.minusSeconds(7200); // Look back 2 hours max
            
            // Get round data to calculate actual game duration
            List<RoundResultDTO> rounds = getRoundResults(gameStartTime, gameEndTime);
            
            if (!rounds.isEmpty()) {
                // Adjust start time based on first round
                gameStartTime = rounds.get(0).getRoundEndTime().minusSeconds(120); // 2 minutes before first round end
            }
            
            // Create game details with scores from the game
            String[] scoreParts = game.getScore().split(" - ");
            int score1 = Integer.parseInt(scoreParts[0].trim());
            int score2 = Integer.parseInt(scoreParts[1].trim());
            
            GameDetailsDTO details = new GameDetailsDTO(score2, score1, score1 + score2); // CT, T, Total
            details.setRounds(rounds);
            
            // Get player statistics (placeholder for now)
            List<PlayerStatsDTO> playerStats = getPlayerStatistics(gameStartTime, gameEndTime, game.getPlayers());
            details.setPlayerStats(playerStats);
            
            return details;
            
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse game ID: {}", gameId, e);
            return null;
        }
    }
    
    /**
     * Get round-by-round results for a game
     */
    private List<RoundResultDTO> getRoundResults(Instant gameStartTime, Instant gameEndTime) {
        List<RoundResultDTO> rounds = new ArrayList<>();
        String startTimeStr = gameStartTime.toString();
        String endTimeStr = gameEndTime.toString();
        
        try (ResultSet resultSet = persistenceLayer.query("GameEvent", 
                new String[]{"event", "at"}, "gameEventType = ? AND at BETWEEN ? AND ? ORDER BY at ASC",
                GameEventType.ROUND_END.name(), startTimeStr, endTimeStr)) {
            
            int roundNumber = 1;
            while (resultSet.next()) {
                try {
                    GameEvent event = objectMapper.readValue(resultSet.getString("event"), GameEvent.class);
                    event.setTimestamp(parseTimestamp(resultSet.getString("at")));
                    if (event instanceof RoundEndEvent roundEndEvent) {
                        // Determine winner team based on score progression
                        String winnerTeam = determineWinnerTeam(roundEndEvent);
                        
                        // Extract scores from additionalData if available
                        int ctScore = 0;
                        int tScore = 0;
                        Map<String, String> additionalData = roundEndEvent.getAdditionalData();
                        if (additionalData != null) {
                            try {
                                ctScore = Integer.parseInt(additionalData.getOrDefault("ct_score", "0"));
                                tScore = Integer.parseInt(additionalData.getOrDefault("t_score", "0"));
                            } catch (NumberFormatException e) {
                                // Use default values if parsing fails
                            }
                        }
                        
                        RoundResultDTO round = new RoundResultDTO(
                            roundNumber++,
                            winnerTeam,
                            "unknown", // Win condition not available in current data
                            roundEndEvent.getTimestamp(),
                            ctScore, // CT score
                            tScore   // T score
                        );
                        rounds.add(round);
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse round end event", e);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get round results for game between {} and {}", gameStartTime, gameEndTime, e);
        }
        
        return rounds;
    }
    
    /**
     * Determine the winner team for a round based on score changes
     */
    private String determineWinnerTeam(RoundEndEvent roundEndEvent) {
        // This is a simplified approach - in a real implementation,
        // you'd compare with the previous round's scores
        // For now, we'll alternate based on round number for demo purposes
        Map<String, String> additionalData = roundEndEvent.getAdditionalData();
        if (additionalData != null) {
            try {
                int ctScore = Integer.parseInt(additionalData.getOrDefault("ct_score", "0"));
                int tScore = Integer.parseInt(additionalData.getOrDefault("t_score", "0"));
                return (ctScore + tScore) % 2 == 0 ? "CT" : "T";
            } catch (NumberFormatException e) {
                // Fall back to default logic
            }
        }
        return "CT"; // Default to CT if no data available
    }
    
    /**
     * Get player statistics for a game (placeholder implementation)
     */
    private List<PlayerStatsDTO> getPlayerStatistics(Instant gameStartTime, Instant gameEndTime, List<String> players) {
        List<PlayerStatsDTO> playerStats = new ArrayList<>();
        
        // This is a placeholder implementation
        // In a real implementation, you'd query KillEvent, AssistEvent, etc. to get actual stats
        for (String player : players) {
            PlayerStatsDTO stats = new PlayerStatsDTO(
                player,
                0, // kills - would be calculated from KillEvent
                0, // deaths - would be calculated from KillEvent where this player is victim
                0, // assists - would be calculated from AssistEvent
                0.0, // rating - would be calculated based on performance
                "Unknown" // team - would be determined from game events
            );
            playerStats.add(stats);
        }
        
        return playerStats;
    }

    /**
     * Parse timestamp from database format to Instant
     * Handles both ISO-8601 format and SQL Server datetime format
     * Database timestamps are stored in local server time, treat as UTC for consistency
     */
    private Instant parseTimestamp(String timestampStr) {
        try {
            // First try standard ISO-8601 format
            return Instant.parse(timestampStr);
        } catch (Exception e) {
            try {
                // Handle SQL Server datetime format: "2025-08-03 22:59:08.0651720"
                // Define the formatter for SQL Server datetime format
                DateTimeFormatter sqlServerFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnn");
                LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, sqlServerFormatter);
                // Treat database timestamps as UTC (no timezone conversion)
                // This ensures GameOver and RoundEnd events use the same timezone interpretation
                return localDateTime.toInstant(ZoneOffset.UTC);
            } catch (Exception ex) {
                LOGGER.warn("Failed to parse timestamp '{}', using current time as fallback", timestampStr, ex);
                return Instant.now();
            }
        }
    }

    /**
     * Retrieves all GameOver events from the database
     */
    private List<GameOverEvent> getGameOverEventsFromDatabase() throws SQLException {
        List<GameOverEvent> gameOverEvents = new ArrayList<>();
        
        try (ResultSet resultSet = persistenceLayer.query("GameEvent", 
                new String[]{"event", "at"}, "gameEventType = ?", GameEventType.GAME_OVER.name())) {
            
            while (resultSet.next()) {
                try {
                    String rawTimestamp = resultSet.getString("at");
                    GameEvent event = objectMapper.readValue(resultSet.getString("event"), GameEvent.class);
                    Instant parsedTimestamp = parseTimestamp(rawTimestamp);
                    event.setTimestamp(parsedTimestamp);
                    
                    if (event instanceof GameOverEvent gameOverEvent) {
                        gameOverEvents.add(gameOverEvent);
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse GameOver event", e);
                }
            }
        }

        LOGGER.info("Retrieved {} GameOver events from database", gameOverEvents.size());
        return gameOverEvents;
    }
}