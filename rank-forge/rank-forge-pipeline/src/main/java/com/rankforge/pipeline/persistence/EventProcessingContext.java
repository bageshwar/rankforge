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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final List<GameEventEntity> pendingEntities = new ArrayList<>();
    private final List<AccoladeEntity> pendingAccolades = new ArrayList<>();
    
    // Tracking for debugging round linking
    private int roundNumber = 0;
    private int eventsInCurrentRound = 0;
    private final Map<RoundStartEventEntity, Integer> roundEventCounts = new HashMap<>();
    private int eventsWithoutRound = 0;
    
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
     */
    public void linkAccoladesToGame() {
        if (currentGame != null) {
            for (AccoladeEntity accolade : pendingAccolades) {
                accolade.setGame(currentGame);
            }
            logger.debug("GAME_CONTEXT: Linked {} accolades to game", pendingAccolades.size());
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
        roundNumber = 0;
        eventsInCurrentRound = 0;
        eventsWithoutRound = 0;
    }
    
    /**
     * Logs a summary of round linking for debugging.
     */
    private void logRoundSummary() {
        String map = currentGame != null ? currentGame.getMap() : "unknown";
        logger.info("=== ROUND SUMMARY for game on {} ===", map);
        logger.info("Total rounds: {}", roundNumber);
        logger.info("Events without round reference: {}", eventsWithoutRound);
        
        int totalWithRound = 0;
        for (Map.Entry<RoundStartEventEntity, Integer> entry : roundEventCounts.entrySet()) {
            totalWithRound += entry.getValue();
        }
        logger.info("Total events with round reference: {}", totalWithRound);
        logger.info("Total pending entities: {}", pendingEntities.size());
        
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
        
        logger.info("Events WITH roundStart reference: {}", withRoundRef);
        logger.info("Events WITHOUT roundStart reference: {}", withoutRoundRef);
        logger.info("Event type breakdown: {}", typeCounts);
        logger.info("=== END ROUND SUMMARY ===");
    }
    
    // Getters
    
    public GameEntity getCurrentGame() {
        return currentGame;
    }
    
    public RoundStartEventEntity getCurrentRoundStart() {
        return currentRoundStart;
    }
    
    public List<GameEventEntity> getPendingEntities() {
        return pendingEntities;
    }
    
    public List<AccoladeEntity> getPendingAccolades() {
        return pendingAccolades;
    }
}
