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

import java.util.ArrayList;
import java.util.List;

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
    
    private GameEntity currentGame;
    private RoundStartEventEntity currentRoundStart;
    private final List<GameEventEntity> pendingEntities = new ArrayList<>();
    private final List<AccoladeEntity> pendingAccolades = new ArrayList<>();
    
    /**
     * Called when GAME_OVER is processed (happens FIRST due to parser rewind).
     * Sets the current game context that all subsequent events will reference.
     */
    public void setCurrentGame(GameEntity game) {
        this.currentGame = game;
    }
    
    /**
     * Called when ROUND_START is processed.
     * The game already exists at this point.
     */
    public void onRoundStart(RoundStartEventEntity entity) {
        entity.setGame(currentGame);  // Game already exists!
        this.currentRoundStart = entity;
        pendingEntities.add(entity);
    }
    
    /**
     * Called for in-round events: KILL, ASSIST, ATTACK, BOMB.
     * Both game and round already exist at this point.
     */
    public void addEvent(GameEventEntity entity) {
        entity.setGame(currentGame);           // Game already exists!
        entity.setRoundStart(currentRoundStart); // Round already exists (may be null if outside round)
        pendingEntities.add(entity);
    }
    
    /**
     * Called when ROUND_END is processed.
     * Links to current game and round, then clears the round context.
     */
    public void onRoundEnd(RoundEndEventEntity entity) {
        entity.setGame(currentGame);
        entity.setRoundStart(currentRoundStart);
        pendingEntities.add(entity);
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
        }
    }
    
    /**
     * Clears all context after GAME_PROCESSED is received.
     * Called after batch persisting all pending entities.
     */
    public void clear() {
        currentRoundStart = null;
        currentGame = null;
        pendingEntities.clear();
        pendingAccolades.clear();
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
