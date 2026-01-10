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
import com.rankforge.core.events.GameEvent;
import com.rankforge.core.events.GameEventType;
import com.rankforge.core.events.GameOverEvent;
import com.rankforge.core.events.RoundEndEvent;
import com.rankforge.pipeline.persistence.AccoladeStore;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
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
 * Service layer for managing processed games data
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class GameService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);
    
    private final ObjectMapper objectMapper;
    private final GameEventRepository gameEventRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final AccoladeStore accoladeStore;
    
    @Autowired
    public GameService(ObjectMapper objectMapper, 
                       GameEventRepository gameEventRepository,
                       PlayerStatsRepository playerStatsRepository,
                       AccoladeStore accoladeStore) {
        this.objectMapper = objectMapper;
        this.gameEventRepository = gameEventRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.accoladeStore = accoladeStore;
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
                
                // Extract duration from GameOverEvent field
                String duration = gameOverEvent.getDuration() != null 
                    ? String.valueOf(gameOverEvent.getDuration()) 
                    : null;
                
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
                LOGGER.info("Game processing {} took {}ms, duration: {} min", gameId, System.currentTimeMillis() - start, duration);
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
        // Use a map to track normalized names for case-insensitive deduplication
        Map<String, String> normalizedToOriginal = new HashMap<>();
        
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
        
        List<RoundEndEventWithTime> allRounds = new ArrayList<>();
        
        try {
            Instant dayStartInstant = Instant.parse(dayStart);
            Instant dayEndInstant = Instant.parse(dayEnd);
            
            List<GameEventEntity> entities = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                    GameEventType.ROUND_END, dayStartInstant, dayEndInstant);
            
            // Collect all round events with their database timestamps
            for (GameEventEntity entity : entities) {
                try {
                    Instant roundTime = entity.getTimestamp();
                    if (entity.getEventJson() != null) {
                        GameEvent event = objectMapper.readValue(entity.getEventJson(), GameEvent.class);
                        
                        if (event instanceof RoundEndEvent roundEndEvent) {
                            allRounds.add(new RoundEndEventWithTime(roundEndEvent, roundTime));
                        }
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse RoundEndEvent", e);
                }
            }
            
            LOGGER.info("Found {} total ROUND_END events on this day", allRounds.size());
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Table doesn't exist yet - this is expected for empty databases
            LOGGER.debug("GameEvent table does not exist yet, returning empty list", e);
            return new ArrayList<>();
        } catch (org.springframework.dao.DataAccessException e) {
            // Other database access exceptions
            LOGGER.warn("Database access error while retrieving ROUND_END events", e);
            return new ArrayList<>();
        }
        
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
                if (playerId != null && !playerId.isEmpty() && !"0".equals(playerId)) { // Exclude bots and invalid IDs
                    String playerName = getPlayerNameById(playerIdToNameCache, playerId);
                    if (playerName != null && !playerName.trim().isEmpty()) {
                        String trimmedName = playerName.trim();
                        // Use normalized (lowercase) key for case-insensitive deduplication
                        // Keep the first occurrence's original casing
                        String normalizedKey = trimmedName.toLowerCase();
                        if (!normalizedToOriginal.containsKey(normalizedKey)) {
                            normalizedToOriginal.put(normalizedKey, trimmedName);
                        }
                    }
                }
            });
        }
        
        // Convert to sorted list for consistent display
        List<String> playerList = new ArrayList<>(normalizedToOriginal.values());
        playerList.sort(String.CASE_INSENSITIVE_ORDER);
        
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
            // Table doesn't exist yet - this is expected for empty databases
            LOGGER.debug("PlayerStats table does not exist yet for player ID {}", playerId, e);
        } catch (org.springframework.dao.DataAccessException e) {
            // Other database access exceptions
            LOGGER.warn("Database access error while retrieving player name for ID {}", playerId, e);
        } catch (Exception e) {
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
            
            // Get final scores from the game
            String[] scoreParts = game.getScore().split(" - ");
            int finalCtScore = Integer.parseInt(scoreParts[1].trim()); // CT score is second
            int finalTScore = Integer.parseInt(scoreParts[0].trim());  // T score is first
            
            // Use the same temporal grouping logic as getPlayersForGame to find the specific game's rounds
            List<RoundResultDTO> rounds = getRoundResultsForGame(gameEndTime, finalCtScore, finalTScore);
            
            // Calculate game start time from rounds
            Instant gameStartTime = gameEndTime.minusSeconds(7200); // Default fallback
            if (!rounds.isEmpty()) {
                gameStartTime = rounds.get(0).getRoundEndTime().minusSeconds(120); // 2 minutes before first round end
            }
            
            // Create game details with scores from the game (already parsed above)
            GameDetailsDTO details = new GameDetailsDTO(finalCtScore, finalTScore, finalCtScore + finalTScore); // CT, T, Total
            details.setRounds(rounds);
            
            // Get player statistics (placeholder for now)
            List<PlayerStatsDTO> playerStats = getPlayerStatistics(gameStartTime, gameEndTime, game.getPlayers());
            details.setPlayerStats(playerStats);
            
            // Extract accolades from GameOverEvent
            List<AccoladeDTO> accolades = extractAccolades(gameEndTime);
            details.setAccolades(accolades);
            
            return details;
            
        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse game ID: {}", gameId, e);
            return null;
        }
    }
    
    /**
     * Get round-by-round results for a specific game using temporal grouping
     * Uses the same logic as getPlayersForGame to find the correct round group
     * @param gameEndTime The end time of the game
     * @param finalCtScore The final CT score from the game
     * @param finalTScore The final T score from the game
     */
    private List<RoundResultDTO> getRoundResultsForGame(Instant gameEndTime, int finalCtScore, int finalTScore) {
        List<RoundResultDTO> rounds = new ArrayList<>();
        
        // Use the same approach as getPlayersForGame - find rounds on the same day
        String dayStart = gameEndTime.truncatedTo(java.time.temporal.ChronoUnit.DAYS).toString();
        String dayEnd = gameEndTime.truncatedTo(java.time.temporal.ChronoUnit.DAYS).plusSeconds(86400).toString();
        
        LOGGER.info("Searching for ROUND_END events on day {} to find rounds for game ending at {}", dayStart, gameEndTime);
        
        List<RoundEndEventWithTime> allRounds = new ArrayList<>();
        Map<Instant, JsonNode> roundJsonMap = new HashMap<>();
        
        try {
            Instant dayStartInstant = Instant.parse(dayStart);
            Instant dayEndInstant = Instant.parse(dayEnd);
            
            List<GameEventEntity> entities = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                    GameEventType.ROUND_END, dayStartInstant, dayEndInstant);
            
            // Collect all round events with their database timestamps and raw JSON
            for (GameEventEntity entity : entities) {
                try {
                    Instant roundTime = entity.getTimestamp();
                    String eventJson = entity.getEventJson();
                    
                    if (eventJson != null) {
                        // Store the raw JSON for score extraction
                        JsonNode eventNode = objectMapper.readTree(eventJson);
                        roundJsonMap.put(roundTime, eventNode);
                        
                        // Also parse as GameEvent for grouping logic
                        GameEvent event = objectMapper.readValue(eventJson, GameEvent.class);
                        
                        if (event instanceof RoundEndEvent roundEndEvent) {
                            allRounds.add(new RoundEndEventWithTime(roundEndEvent, roundTime));
                        }
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse RoundEndEvent", e);
                }
            }
            
            LOGGER.info("Found {} total ROUND_END events on this day", allRounds.size());
            
            if (allRounds.isEmpty()) {
                LOGGER.warn("No ROUND_END events found for game ending at {}", gameEndTime);
                return rounds;
            }
            
            // Group rounds into games by temporal proximity (gaps > 5 minutes = new game)
            List<List<RoundEndEventWithTime>> gameGroups = groupRoundsByTemporalProximity(allRounds);
            LOGGER.info("Grouped rounds into {} potential games", gameGroups.size());
            
            // Find the game group closest to our GameOver timestamp
            List<RoundEndEventWithTime> bestMatch = findClosestGameGroup(gameGroups, gameEndTime);
            
            if (bestMatch == null || bestMatch.isEmpty()) {
                LOGGER.warn("No round group found close to game end time {}", gameEndTime);
                return rounds;
            }
            
            LOGGER.info("Selected game group with {} rounds for game ending at {}", 
                    bestMatch.size(), gameEndTime);
            
            // Convert to RoundResultDTO
            // Work backwards from final scores to determine round winners
            // We know the final scores, so we can work backwards to determine each round's winner
            int currentCtScore = finalCtScore;
            int currentTScore = finalTScore;
            
            // Build rounds list working backwards
            List<RoundResultDTO> tempRounds = new ArrayList<>();
            
            for (int i = bestMatch.size() - 1; i >= 0; i--) {
                RoundEndEventWithTime roundWithTime = bestMatch.get(i);
                Instant roundTime = roundWithTime.timestamp();
                int roundNumber = i + 1;
                
                // Determine winner: work backwards from final scores
                // If a team has more remaining wins than remaining rounds, they must have won this round
                String winnerTeam;
                int roundsRemaining = i + 1;
                int ctWinsRemaining = currentCtScore;
                int tWinsRemaining = currentTScore;
                
                if (ctWinsRemaining > roundsRemaining) {
                    // CT must have won this round
                    winnerTeam = "CT";
                    currentCtScore--;
                } else if (tWinsRemaining > roundsRemaining) {
                    // T must have won this round
                    winnerTeam = "T";
                    currentTScore--;
                } else {
                    // Distribute wins proportionally to match final score
                    double ctRatio = (double) ctWinsRemaining / roundsRemaining;
                    double tRatio = (double) tWinsRemaining / roundsRemaining;
                    winnerTeam = (ctRatio >= tRatio) ? "CT" : "T";
                    if (winnerTeam.equals("CT") && currentCtScore > 0) {
                        currentCtScore--;
                    } else if (winnerTeam.equals("T") && currentTScore > 0) {
                        currentTScore--;
                    }
                }
                
                // Calculate scores for this round
                int ctScore = currentCtScore;
                int tScore = currentTScore;
                
                RoundResultDTO round = new RoundResultDTO(
                    roundNumber,
                    winnerTeam,
                    "unknown", // Win condition not available in current data
                    roundTime,
                    ctScore, // CT score
                    tScore   // T score
                );
                tempRounds.add(0, round); // Add at beginning to reverse order
            }
            
            rounds.addAll(tempRounds);
            
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Table doesn't exist yet - this is expected for empty databases
            LOGGER.debug("GameEvent table does not exist yet, returning empty rounds list", e);
        } catch (org.springframework.dao.DataAccessException e) {
            // Other database access exceptions
            LOGGER.warn("Database access error while retrieving round results", e);
        } catch (Exception e) {
            LOGGER.error("Failed to get round results for game ending at {}", gameEndTime, e);
        }
        
        return rounds;
    }
    
    
    /**
     * Get player statistics for a game by querying KillEvent and AssistEvent
     */
    private List<PlayerStatsDTO> getPlayerStatistics(Instant gameStartTime, Instant gameEndTime, List<String> players) {
        // Create a map to track stats for each player (case-insensitive matching)
        Map<String, PlayerStatsDTO> statsMap = new HashMap<>();
        for (String player : players) {
            String normalizedName = player.trim().toLowerCase();
            statsMap.put(normalizedName, new PlayerStatsDTO(
                player.trim(),
                0, // kills
                0, // deaths
                0, // assists
                0.0, // rating
                "Unknown" // team
            ));
        }
        
        // Query KillEvent to count kills and deaths
        try {
            List<GameEventEntity> killEntities = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                    GameEventType.KILL, gameStartTime, gameEndTime);
            
            for (GameEventEntity entity : killEntities) {
                try {
                    Instant eventTime = entity.getTimestamp();
                    String eventJson = entity.getEventJson();
                    
                    if (eventJson != null && 
                        (eventTime.equals(gameStartTime) || eventTime.isAfter(gameStartTime)) &&
                        (eventTime.equals(gameEndTime) || eventTime.isBefore(gameEndTime))) {
                        
                        // Parse JSON manually to extract player names
                        JsonNode eventNode = objectMapper.readTree(eventJson);
                        JsonNode player1Node = eventNode.get("player1");
                        JsonNode player2Node = eventNode.get("player2");
                        
                        // Extract killer name
                        if (player1Node != null && !player1Node.isNull()) {
                            JsonNode botNode = player1Node.get("bot");
                            if (botNode == null || !botNode.asBoolean()) {
                                JsonNode nameNode = player1Node.get("name");
                                if (nameNode != null && !nameNode.isNull()) {
                                    String killerName = nameNode.asText().trim().toLowerCase();
                                    PlayerStatsDTO stats = statsMap.get(killerName);
                                    if (stats != null) {
                                        stats.setKills(stats.getKills() + 1);
                                    }
                                }
                            }
                        }
                        
                        // Extract victim name
                        if (player2Node != null && !player2Node.isNull()) {
                            JsonNode botNode = player2Node.get("bot");
                            if (botNode == null || !botNode.asBoolean()) {
                                JsonNode nameNode = player2Node.get("name");
                                if (nameNode != null && !nameNode.isNull()) {
                                    String victimName = nameNode.asText().trim().toLowerCase();
                                    PlayerStatsDTO stats = statsMap.get(victimName);
                                    if (stats != null) {
                                        stats.setDeaths(stats.getDeaths() + 1);
                                    }
                                }
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse KillEvent", e);
                }
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Table doesn't exist yet - this is expected for empty databases
            LOGGER.debug("GameEvent table does not exist yet, skipping kill events", e);
        } catch (org.springframework.dao.DataAccessException e) {
            // Other database access exceptions
            LOGGER.warn("Database access error while retrieving kill events", e);
        } catch (Exception e) {
            LOGGER.error("Failed to get kill events for game between {} and {}", gameStartTime, gameEndTime, e);
        }
        
        // Query AssistEvent to count assists
        try {
            List<GameEventEntity> assistEntities = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                    GameEventType.ASSIST, gameStartTime, gameEndTime);
            
            for (GameEventEntity entity : assistEntities) {
                try {
                    Instant eventTime = entity.getTimestamp();
                    String eventJson = entity.getEventJson();
                    
                    if (eventJson != null &&
                        (eventTime.equals(gameStartTime) || eventTime.isAfter(gameStartTime)) &&
                        (eventTime.equals(gameEndTime) || eventTime.isBefore(gameEndTime))) {
                        
                        // Parse JSON manually to extract player name
                        JsonNode eventNode = objectMapper.readTree(eventJson);
                        JsonNode player1Node = eventNode.get("player1");
                        
                        // Extract assister name
                        if (player1Node != null && !player1Node.isNull()) {
                            JsonNode botNode = player1Node.get("bot");
                            if (botNode == null || !botNode.asBoolean()) {
                                JsonNode nameNode = player1Node.get("name");
                                if (nameNode != null && !nameNode.isNull()) {
                                    String assisterName = nameNode.asText().trim().toLowerCase();
                                    PlayerStatsDTO stats = statsMap.get(assisterName);
                                    if (stats != null) {
                                        stats.setAssists(stats.getAssists() + 1);
                                    }
                                }
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse AssistEvent", e);
                }
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Table doesn't exist yet - this is expected for empty databases
            LOGGER.debug("GameEvent table does not exist yet, skipping assist events", e);
        } catch (org.springframework.dao.DataAccessException e) {
            // Other database access exceptions
            LOGGER.warn("Database access error while retrieving assist events", e);
        } catch (Exception e) {
            LOGGER.error("Failed to get assist events for game between {} and {}", gameStartTime, gameEndTime, e);
        }
        
        // Calculate rating (simple K/D ratio, or 0 if no deaths)
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
                // If player not found in events, add with zero stats
                playerStats.add(new PlayerStatsDTO(
                    player.trim(),
                    0, 0, 0, 0.0, "Unknown"
                ));
            }
        }
        
        LOGGER.info("Calculated statistics for {} players in game between {} and {}", 
                playerStats.size(), gameStartTime, gameEndTime);
        
        // Log summary of stats found
        int totalKills = playerStats.stream().mapToInt(PlayerStatsDTO::getKills).sum();
        int totalDeaths = playerStats.stream().mapToInt(PlayerStatsDTO::getDeaths).sum();
        int totalAssists = playerStats.stream().mapToInt(PlayerStatsDTO::getAssists).sum();
        LOGGER.debug("Total stats found: {} kills, {} deaths, {} assists", totalKills, totalDeaths, totalAssists);
        
        return playerStats;
    }


    /**
     * Retrieves all GameOver events from the database
     */
    private List<GameOverEvent> getGameOverEventsFromDatabase() {
        List<GameOverEvent> gameOverEvents = new ArrayList<>();
        
        try {
            List<GameEventEntity> entities = gameEventRepository.findByGameEventType(GameEventType.GAME_OVER);
            
            for (GameEventEntity entity : entities) {
                try {
                    if (entity.getEventJson() != null) {
                        GameEvent event = objectMapper.readValue(entity.getEventJson(), GameEvent.class);
                        // Use entity timestamp if event timestamp is null
                        if (event.getTimestamp() == null && entity.getTimestamp() != null) {
                            event.setTimestamp(entity.getTimestamp());
                        }
                        
                        if (event instanceof GameOverEvent gameOverEvent) {
                            gameOverEvents.add(gameOverEvent);
                        }
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse GameOver event", e);
                }
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // Table doesn't exist yet - this is expected for empty databases
            LOGGER.debug("GameEvent table does not exist yet, returning empty list", e);
            return gameOverEvents;
        } catch (org.springframework.dao.DataAccessException e) {
            // Other database access exceptions
            LOGGER.warn("Database access error while retrieving GameOver events, returning empty list", e);
            return gameOverEvents;
        } catch (Exception e) {
            LOGGER.error("Unexpected error while retrieving GameOver events", e);
            return gameOverEvents;
        }

        LOGGER.info("Retrieved {} GameOver events from database", gameOverEvents.size());
        return gameOverEvents;
    }
    
    /**
     * Extract accolades from Accolade table for a specific game
     */
    private List<AccoladeDTO> extractAccolades(Instant gameEndTime) {
        List<AccoladeDTO> accolades = new ArrayList<>();
        
        try {
            List<AccoladeStore.Accolade> storedAccolades = accoladeStore.getAccoladesForGame(gameEndTime);
            
            for (AccoladeStore.Accolade storedAccolade : storedAccolades) {
                AccoladeDTO accoladeDTO = new AccoladeDTO(
                        storedAccolade.getType(),
                        storedAccolade.getPlayerName(),
                        storedAccolade.getPlayerId(),
                        storedAccolade.getValue(),
                        storedAccolade.getPosition(),
                        storedAccolade.getScore()
                );
                accolades.add(accoladeDTO);
            }
            
            LOGGER.info("Extracted {} accolades for game ending at {}", accolades.size(), gameEndTime);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to extract accolades", e);
        }
        
        return accolades;
    }
    
}