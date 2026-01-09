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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Store for game accolades (player achievements/awards)
 * Author bageshwar.pn
 * Date 2024
 */
public class AccoladeStore {
    private static final String TABLE_NAME = "Accolade";
    private static final Logger logger = LoggerFactory.getLogger(AccoladeStore.class);
    
    private final PersistenceLayer persistenceLayer;
    
    public AccoladeStore(PersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
        this.createTable();
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
    
    private void createTable() {
        ColumnDefinition[] columns = {
                new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                        .primaryKey()
                        .autoIncrement()
                        .build(),
                new ColumnDefinition.Builder("gameTimestamp", ColumnType.TEXT_SHORT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("type", ColumnType.TEXT_SHORT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("playerName", ColumnType.TEXT_SHORT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("playerId", ColumnType.TEXT_SHORT)
                        .build(),
                new ColumnDefinition.Builder("value", ColumnType.REAL)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("position", ColumnType.INTEGER)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("score", ColumnType.REAL)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("created_at", ColumnType.TEXT_SHORT)
                        .defaultValue("CURRENT_TIMESTAMP")
                        .build()
        };
        
        try {
            persistenceLayer.createTable(TABLE_NAME, columns, null, true);
            logger.debug("Accolade table created or already exists");
        } catch (SQLException e) {
            logger.error("Unable to create table {}", TABLE_NAME, e);
            System.exit(1);
        }
    }
    
    /**
     * Store accolades for a game
     */
    public void storeAccolades(Instant gameTimestamp, List<Accolade> accolades) {
        if (accolades == null || accolades.isEmpty()) {
            return;
        }
        
        try {
            List<Map<String, Object>> dataList = new ArrayList<>();
            String timestampStr = gameTimestamp.toString();
            
            for (Accolade accolade : accolades) {
                Map<String, Object> values = new HashMap<>();
                values.put("gameTimestamp", timestampStr);
                values.put("type", accolade.getType());
                values.put("playerName", accolade.getPlayerName());
                values.put("playerId", accolade.getPlayerId());
                values.put("value", accolade.getValue());
                values.put("position", accolade.getPosition());
                values.put("score", accolade.getScore());
                dataList.add(values);
            }
            
            persistenceLayer.batchInsert(TABLE_NAME, dataList);
            logger.debug("Stored {} accolades for game at {}", accolades.size(), gameTimestamp);
            
        } catch (SQLException e) {
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
        
        try (ResultSet resultSet = persistenceLayer.query(TABLE_NAME,
                new String[]{"type", "playerName", "playerId", "value", "position", "score", "gameTimestamp"},
                "gameTimestamp BETWEEN ? AND ? ORDER BY gameTimestamp ASC",
                startTime.toString(), endTime.toString())) {
            
            while (resultSet.next()) {
                // Only include accolades that match the exact timestamp (within 1 second tolerance)
                String storedTimestamp = resultSet.getString("gameTimestamp");
                Instant storedInstant = Instant.parse(storedTimestamp);
                
                // Check if this accolade belongs to the requested game (within 1 second)
                long timeDiff = Math.abs(storedInstant.toEpochMilli() - gameTimestamp.toEpochMilli());
                if (timeDiff <= 1000) {
                    Accolade accolade = new Accolade(
                            resultSet.getString("type"),
                            resultSet.getString("playerName"),
                            resultSet.getString("playerId"),
                            resultSet.getDouble("value"),
                            resultSet.getInt("position"),
                            resultSet.getDouble("score")
                    );
                    accolades.add(accolade);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get accolades for game at {}", gameTimestamp, e);
        }
        
        return accolades;
    }
}
