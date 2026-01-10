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
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Store for game accolades (player achievements/awards)
 * Author bageshwar.pn
 * Date 2024
 */
public class AccoladeStore {
    private static final Logger logger = LoggerFactory.getLogger(AccoladeStore.class);
    
    private final AccoladeRepository repository;
    
    public AccoladeStore(AccoladeRepository repository) {
        this.repository = repository;
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
     * Convert Accolade domain object to AccoladeEntity
     */
    private AccoladeEntity convertToEntity(Accolade accolade, Instant gameTimestamp) {
        AccoladeEntity entity = new AccoladeEntity();
        entity.setGameTimestamp(gameTimestamp);
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
     * Store accolades for a game
     */
    @Transactional
    public void storeAccolades(Instant gameTimestamp, List<Accolade> accolades) {
        if (accolades == null || accolades.isEmpty()) {
            return;
        }
        
        try {
            List<AccoladeEntity> entities = new ArrayList<>();
            for (Accolade accolade : accolades) {
                AccoladeEntity entity = convertToEntity(accolade, gameTimestamp);
                entities.add(entity);
            }
            
            repository.saveAll(entities);
            logger.debug("Stored {} accolades for game at {}", accolades.size(), gameTimestamp);
            
        } catch (Exception e) {
            logger.error("Failed to store accolades", e);
        }
    }
    
    /**
     * Get accolades for a specific game
     */
    public List<Accolade> getAccoladesForGame(Instant gameTimestamp) {
        List<Accolade> accolades = new ArrayList<>();
        
        // Use a tighter time window to find the exact game (2 seconds on each side)
        Instant startTime = gameTimestamp.minusSeconds(2);
        Instant endTime = gameTimestamp.plusSeconds(2);
        
        try {
            List<AccoladeEntity> entities = repository.findByGameTimestampBetween(startTime, endTime);
            
            for (AccoladeEntity entity : entities) {
                // Only include accolades that match the exact timestamp (within 1 second tolerance)
                long timeDiff = Math.abs(entity.getGameTimestamp().toEpochMilli() - gameTimestamp.toEpochMilli());
                if (timeDiff <= 1000) {
                    Accolade accolade = convertToDomain(entity);
                    accolades.add(accolade);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get accolades for game at {}", gameTimestamp, e);
        }
        
        return accolades;
    }
}
