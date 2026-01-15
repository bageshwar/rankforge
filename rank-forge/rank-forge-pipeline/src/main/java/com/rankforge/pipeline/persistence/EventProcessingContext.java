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

package com.rankforge.pipeline.persistence;

import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.entity.RoundStartEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maintains processing context for a single game being processed.
 * 
 * Key insight from CS2LogParser: The parser sees GAME_OVER first, then rewinds to process rounds.
 * This means:
 * - When ROUND_START is processed → GameEntity already exists (from GAME_OVER)
 * - When KILL is processed → RoundStartEventEntity already exists
 * 
 * References are set directly as events arrive - no pending list updates needed for game events.
 * 
 * Exception: Accolades are parsed BEFORE GameEntity is created (in parser before GAME_OVER is returned),
 * so they are queued and linked to the game when setCurrentGame is called.
 * 
 * Author bageshwar.pn
 * Date 2026
 */
public class EventProcessingContext {
    
    private static final Logger logger = LoggerFactory.getLogger(EventProcessingContext.class);
    
    private GameEntity currentGame;
    private RoundStartEventEntity currentRoundStart;
    private Instant lastRoundEndTimestamp; // Track last ROUND_END timestamp to detect events between rounds
    private final List<GameEventEntity> pendingEntities = new ArrayList<>();
    private final List<AccoladeEntity> pendingAccolades = new ArrayList<>();
    
    // Tracking for debugging round linking
    private int roundNumber = 0;
    private int eventsInCurrentRound = 0;
    private final Map<RoundStartEventEntity, Integer> roundEventCounts = new HashMap<>();
    private int eventsWithoutRound = 0;
    
    // Player name to Steam ID mapping for resolving accolade player IDs
    private final Map<String, String> playerNameToSteamId = new HashMap<>();
    
    /**
     * Called when GAME_OVER is processed (happens FIRST due to parser rewind).
     * Sets the current game context that all subsequent events will reference.
     */
    public void setCurrentGame(GameEntity game) {
        this.currentGame = game;
        // Reset round tracking for new game
        roundNumber = 0;
        eventsInCurrentRound = 0;
        eventsWithoutRound = 0;
        lastRoundEndTimestamp = null;
        roundEventCounts.clear();
        logger.info("GAME_CONTEXT: Set current game - map: {}", game != null ? game.getMap() : "null");
    }
    
    /**
     * Called when ROUND_START is processed.
     * The game already exists at this point.
     */
    public void onRoundStart(RoundStartEventEntity entity) {
        // Log previous round stats before starting new round
        if (currentRoundStart != null && eventsInCurrentRound > 0) {
            roundEventCounts.put(currentRoundStart, eventsInCurrentRound);
            logger.warn("ROUND_CONTEXT: Previous round {} had {} events but no ROUND_END received!", 
                    roundNumber, eventsInCurrentRound);
        }
        
        roundNumber++;
        eventsInCurrentRound = 0;
        lastRoundEndTimestamp = null; // Clear when new round starts
        
        entity.setGame(currentGame);  // Game already exists!
        this.currentRoundStart = entity;
        pendingEntities.add(entity);
        
        logger.info("ROUND_CONTEXT: Round {} started at {}", roundNumber, entity.getTimestamp());
    }
    
    /**
     * Called for in-round events: KILL, ASSIST, ATTACK, BOMB.
     * Both game and round already exist at this point.
     */
    public void addEvent(GameEventEntity entity) {
        entity.setGame(currentGame);           // Game already exists!
        entity.setRoundStart(currentRoundStart); // Round already exists (may be null if outside round)
        pendingEntities.add(entity);
        
        if (currentRoundStart != null) {
            eventsInCurrentRound++;
        } else {
            eventsWithoutRound++;
            logger.debug("ROUND_CONTEXT: Event {} added WITHOUT round context at {}", 
                    entity.getGameEventType(), entity.getTimestamp());
        }
    }
    
    // Pattern to extract numeric Steam ID from full format [U:1:XXXXXXXX]
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("\\[U:1:(\\d+)\\]");
    
    /**
     * Gets the list of player Steam IDs (numeric part only) who participated in the current round.
     * Extracts player IDs from all pending entities that reference the current round start.
     * This is used for roundsPlayed counting, especially for the last round where
     * the parser may not have the player list from JSON stats.
     * 
     * @return Set of numeric Steam IDs (e.g., "1090227400" from "[U:1:1090227400]")
     */
    public Set<String> getPlayersInCurrentRound() {
        Set<String> playerIds = new HashSet<>();
        
        if (currentRoundStart == null) {
            logger.debug("ROUND_CONTEXT: No current round start, returning empty player set");
            return playerIds;
        }
        
        for (GameEventEntity entity : pendingEntities) {
            // Only consider events that belong to the current round
            if (entity.getRoundStart() != null && entity.getRoundStart().equals(currentRoundStart)) {
                // Extract numeric Steam ID from player1
                String numericId1 = extractNumericSteamId(entity.getPlayer1());
                if (numericId1 != null && !"0".equals(numericId1)) {
                    playerIds.add(numericId1);
                }
                
                // Extract numeric Steam ID from player2
                String numericId2 = extractNumericSteamId(entity.getPlayer2());
                if (numericId2 != null && !"0".equals(numericId2)) {
                    playerIds.add(numericId2);
                }
            }
        }
        
        logger.debug("ROUND_CONTEXT: Found {} players in current round from pending entities: {}", 
                playerIds.size(), playerIds);
        return playerIds;
    }
    
    /**
     * Extracts the numeric part from a Steam ID in format [U:1:XXXXXXXX].
     * Returns the numeric ID or the original string if not in expected format.
     */
    private String extractNumericSteamId(String steamId) {
        if (steamId == null || steamId.isEmpty()) {
            return null;
        }
        
        Matcher matcher = STEAM_ID_PATTERN.matcher(steamId);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // If not in [U:1:X] format, return as-is (might already be numeric)
        return steamId;
    }
    
    /**
     * Called when ROUND_END is processed.
     * Links to current game and round, then clears the round context.
     */
    public void onRoundEnd(RoundEndEventEntity entity) {
        entity.setGame(currentGame);
        entity.setRoundStart(currentRoundStart);
        pendingEntities.add(entity);
        
        // Track this round's event count
        if (currentRoundStart != null) {
            eventsInCurrentRound++; // Count the ROUND_END itself
            roundEventCounts.put(currentRoundStart, eventsInCurrentRound);
            logger.info("ROUND_CONTEXT: Round {} ended with {} events at {}", 
                    roundNumber, eventsInCurrentRound, entity.getTimestamp());
        } else {
            logger.warn("ROUND_CONTEXT: ROUND_END received but no currentRoundStart! timestamp: {}", 
                    entity.getTimestamp());
        }
        
        // Track the round end timestamp to detect events that occur after round end
        this.lastRoundEndTimestamp = entity.getTimestamp();
        eventsInCurrentRound = 0;
        this.currentRoundStart = null;  // Round is complete
    }
    
    /**
     * Called to queue accolades before GameEntity exists.
     * Accolades are parsed in the parser before GAME_OVER is returned.
     */
    public void addAccolade(AccoladeEntity accolade) {
        pendingAccolades.add(accolade);
    }
    
    /**
     * Register a player name to Steam ID mapping.
     * This is built up as kill/assist/attack events are processed.
     * Used later to resolve accolade player IDs from player names.
     */
    public void registerPlayer(String playerName, String steamId) {
        if (playerName != null && !playerName.isEmpty() && steamId != null && !steamId.isEmpty()) {
            playerNameToSteamId.put(playerName, steamId);
            logger.debug("PLAYER_MAP: Registered {} -> {}", playerName, steamId);
        }
    }
    
    /**
     * Get Steam ID for a player name.
     * Returns the Steam ID if found, or null if not found.
     */
    public String getSteamIdByPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }
        return playerNameToSteamId.get(playerName);
    }
    
    /**
     * Get all registered player name to Steam ID mappings.
     */
    public Map<String, String> getPlayerNameToSteamIdMap() {
        return new HashMap<>(playerNameToSteamId);
    }
    
    /**
     * Called when GAME_OVER event is processed.
     * Sets game reference and adds to pending entities.
     */
    public void addGameOverEvent(GameEventEntity gameOverEvent) {
        gameOverEvent.setGame(currentGame);
        pendingEntities.add(gameOverEvent);
        logger.debug("GAME_CONTEXT: GAME_OVER event added at {}", gameOverEvent.getTimestamp());
    }
    
    /**
     * Links all pending accolades to the current game.
     * Should be called after setCurrentGame() when accolades were queued before game existed.
     * Sets the accolade timestamp to the game's end time (when the accolade was actually awarded).
     */
    public void linkAccoladesToGame() {
        if (currentGame != null) {
            // Use game end time as the timestamp when the accolade was awarded
            Instant gameEndTime = currentGame.getEndTime() != null 
                    ? currentGame.getEndTime() 
                    : currentGame.getGameOverTimestamp();
            
            for (AccoladeEntity accolade : pendingAccolades) {
                accolade.setGame(currentGame);
                // Set timestamp to game end time (when accolade was actually awarded)
                if (gameEndTime != null) {
                    accolade.setCreatedAt(gameEndTime);
                }
            }
            logger.debug("GAME_CONTEXT: Linked {} accolades to game with end time {}", 
                    pendingAccolades.size(), gameEndTime);
        }
    }
    
    /**
     * Clears all context after GAME_PROCESSED is received.
     * Called after batch persisting all pending entities.
     */
    public void clear() {
        // Log summary before clearing
        logRoundSummary();
        
        currentRoundStart = null;
        currentGame = null;
        pendingEntities.clear();
        pendingAccolades.clear();
        roundEventCounts.clear();
        playerNameToSteamId.clear();
        roundNumber = 0;
        eventsInCurrentRound = 0;
        eventsWithoutRound = 0;
    }
    
    /**
     * Logs a summary of round linking for debugging.
     */
    private void logRoundSummary() {
        String map = currentGame != null ? currentGame.getMap() : "unknown";
        logger.debug("=== ROUND SUMMARY for game on {} ===", map);
        logger.debug("Total rounds: {}", roundNumber);
        logger.debug("Events without round reference: {}", eventsWithoutRound);
        
        int totalWithRound = 0;
        for (Map.Entry<RoundStartEventEntity, Integer> entry : roundEventCounts.entrySet()) {
            totalWithRound += entry.getValue();
        }
        logger.debug("Total events with round reference: {}", totalWithRound);
        logger.debug("Total pending entities: {}", pendingEntities.size());
        
        // Count entities by type
        Map<String, Integer> typeCounts = new HashMap<>();
        int withRoundRef = 0;
        int withoutRoundRef = 0;
        for (GameEventEntity entity : pendingEntities) {
            String type = entity.getGameEventType() != null ? entity.getGameEventType().name() : "UNKNOWN";
            typeCounts.merge(type, 1, Integer::sum);
            
            if (entity.getRoundStart() != null) {
                withRoundRef++;
            } else {
                withoutRoundRef++;
            }
        }
        
        logger.debug("Events WITH roundStart reference: {}", withRoundRef);
        logger.debug("Events WITHOUT roundStart reference: {}", withoutRoundRef);
        logger.debug("Event type breakdown: {}", typeCounts);
        logger.debug("=== END ROUND SUMMARY ===");
    }
    
    // Getters
    
    public GameEntity getCurrentGame() {
        return currentGame;
    }
    
    public RoundStartEventEntity getCurrentRoundStart() {
        return currentRoundStart;
    }
    
    /**
     * Returns the timestamp of the last ROUND_END event, or null if no round has ended yet.
     * Used to detect events that occur between rounds (after ROUND_END but before next ROUND_START).
     */
    public Instant getLastRoundEndTimestamp() {
        return lastRoundEndTimestamp;
    }
    
    public List<GameEventEntity> getPendingEntities() {
        return pendingEntities;
    }
    
    public List<AccoladeEntity> getPendingAccolades() {
        return pendingAccolades;
    }
}
