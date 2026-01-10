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
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Store for game accolades (player achievements/awards).
 * Uses EventProcessingContext for deferred persistence with game entity reference.
 * Author bageshwar.pn
 * Date 2024
 */
public class AccoladeStore {
    private static final Logger logger = LoggerFactory.getLogger(AccoladeStore.class);
    
    private final AccoladeRepository repository;
    private final EventProcessingContext context;
    
    public AccoladeStore(AccoladeRepository repository, EventProcessingContext context) {
        this.repository = repository;
        this.context = context;
    }
    
    /**
     * Represents an accolade parsed from log
     */
    public static class Accolade {
        private final String type;
        private final String playerName;
        private final String playerId;
        private final double value;
        private final int position;
        private final double score;
        
        public Accolade(String type, String playerName, String playerId, double value, int position, double score) {
            this.type = type;
            this.playerName = playerName;
            this.playerId = playerId;
            this.value = value;
            this.position = position;
            this.score = score;
        }
        
        public String getType() { return type; }
        public String getPlayerName() { return playerName; }
        public String getPlayerId() { return playerId; }
        public double getValue() { return value; }
        public int getPosition() { return position; }
        public double getScore() { return score; }
    }
    
    /**
     * Convert Accolade domain object to AccoladeEntity.
     * Game reference will be set later when GameEntity is available.
     */
    private AccoladeEntity convertToEntity(Accolade accolade) {
        AccoladeEntity entity = new AccoladeEntity();
        entity.setType(accolade.getType());
        entity.setPlayerName(accolade.getPlayerName());
        entity.setPlayerId(accolade.getPlayerId());
        entity.setValue(accolade.getValue());
        entity.setPosition(accolade.getPosition());
        entity.setScore(accolade.getScore());
        return entity;
    }
    
    /**
     * Convert AccoladeEntity to Accolade domain object
     */
    private Accolade convertToDomain(AccoladeEntity entity) {
        return new Accolade(
                entity.getType(),
                entity.getPlayerName(),
                entity.getPlayerId(),
                entity.getValue(),
                entity.getPosition(),
                entity.getScore()
        );
    }
    
    /**
     * Queue accolades for deferred persistence.
     * Accolades are parsed before GameEntity exists, so we add them to context
     * and set the game reference when GameEntity is created.
     * 
     * Important: Accolades in CS2 logs use local session indices (like "0", "1") instead of
     * Steam IDs. We resolve the actual Steam ID using the player name -> Steam ID mapping
     * that was built during event processing.
     */
    public void queueAccolades(List<Accolade> accolades) {
        if (accolades == null || accolades.isEmpty()) {
            return;
        }
        
        int resolvedCount = 0;
        int unresolvedCount = 0;
        
        for (Accolade accolade : accolades) {
            AccoladeEntity entity = convertToEntity(accolade);
            
            // Try to resolve the Steam ID from player name
            // The log format only gives us local session index (e.g., "0", "1"), not Steam ID
            String resolvedSteamId = context.getSteamIdByPlayerName(accolade.getPlayerName());
            if (resolvedSteamId != null) {
                entity.setPlayerId(resolvedSteamId);
                resolvedCount++;
                logger.debug("Resolved accolade player '{}' to Steam ID: {}", 
                        accolade.getPlayerName(), resolvedSteamId);
            } else {
                unresolvedCount++;
                logger.warn("Could not resolve Steam ID for accolade player: '{}' (keeping original ID: {})", 
                        accolade.getPlayerName(), accolade.getPlayerId());
            }
            
            context.addAccolade(entity);
        }
        
        logger.info("Queued {} accolades for deferred persistence (resolved: {}, unresolved: {})", 
                accolades.size(), resolvedCount, unresolvedCount);
    }
    
    /**
     * Create accolade entities with game reference set.
     * Used when game entity is already available.
     */
    public List<AccoladeEntity> createAccoladeEntities(GameEntity game, List<Accolade> accolades) {
        if (accolades == null || accolades.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AccoladeEntity> entities = new ArrayList<>();
        for (Accolade accolade : accolades) {
            AccoladeEntity entity = convertToEntity(accolade);
            entity.setGame(game);
            entities.add(entity);
        }
        
        return entities;
    }
    
    /**
     * Get accolades for a specific game by game ID
     */
    public List<Accolade> getAccoladesForGame(Long gameId) {
        List<Accolade> accolades = new ArrayList<>();
        
        try {
            List<AccoladeEntity> entities = repository.findByGameId(gameId);
            
            for (AccoladeEntity entity : entities) {
                Accolade accolade = convertToDomain(entity);
                accolades.add(accolade);
            }
        } catch (Exception e) {
            logger.error("Failed to get accolades for game {}", gameId, e);
        }
        
        return accolades;
    }
}
