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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DAO layer for PlayerStats with batching support
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class DBBasedPlayerStatsStore implements PlayerStatsStore {
    private static final Logger logger = LoggerFactory.getLogger(DBBasedPlayerStatsStore.class);
    private static final String TABLE_NAME = "PlayerStats";
    private static final String ARCHIVE_TABLE_NAME = "PlayerStatsArchive";
    private static final int DEFAULT_BATCH_SIZE = 50;

    private final PersistenceLayer persistenceLayer;
    private final ObjectMapper objectMapper;
    private final List<Map<String, Object>> insertBatch;
    private final List<Map<String, Object>> upsertBatch;
    private final int batchSize;
    private final ReentrantLock batchLock = new ReentrantLock();

    public DBBasedPlayerStatsStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        this(persistenceLayer, objectMapper, DEFAULT_BATCH_SIZE);
    }
    
    public DBBasedPlayerStatsStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper, int batchSize) {
        this.persistenceLayer = persistenceLayer;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.insertBatch = new ArrayList<>(batchSize);
        this.upsertBatch = new ArrayList<>(batchSize);
        this.createTable();
    }

    private void createTable() {
        ColumnDefinition[] columns = {
                new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                        .primaryKey()
                        .autoIncrement()
                        .build(),
                new ColumnDefinition.Builder("playerId", ColumnType.TEXT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("playerStats", ColumnType.TEXT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("createdAt", ColumnType.TEXT)
                        .defaultValue("CURRENT_TIMESTAMP")
                        .build()
        };
        try {
            persistenceLayer.createTable(TABLE_NAME, columns, new String[]{"playerId"}, true);
            persistenceLayer.createTable(ARCHIVE_TABLE_NAME, columns, null, true);
        } catch (SQLException e) {
            logger.error("Unable to create table {}", TABLE_NAME);
        }
    }

    @Override
    public void store(PlayerStats stat, boolean archive) {
        try {
            Map<String, Object> values = new HashMap<>();
            values.put("playerId", stat.getPlayerId());
            values.put("playerStats", objectMapper.writeValueAsString(stat));

            batchLock.lock();
            try {
                if (archive) {
                    insertBatch.add(new HashMap<>(values));
                    
                    // If insert batch is full, flush it
                    if (insertBatch.size() >= batchSize) {
                        flushInsertBatch();
                    }
                }

                upsertBatch.add(values);
                
                // If upsert batch is full, flush it
                if (upsertBatch.size() >= batchSize) {
                    flushUpsertBatch();
                }
            } finally {
                batchLock.unlock();
            }

            logger.debug("Stored/updated playerstat for player: {} (archive={})", stat.getPlayerId(), archive);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize PlayerStats", e);
        }
    }
    
    /**
     * Stores multiple player stats in batches
     */
    public void storeBatch(List<PlayerStats> stats, boolean archive) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        
        try {
            List<Map<String, Object>> insertDataList = new ArrayList<>();
            List<Map<String, Object>> upsertDataList = new ArrayList<>();
            
            for (PlayerStats stat : stats) {
                Map<String, Object> values = new HashMap<>();
                values.put("playerId", stat.getPlayerId());
                values.put("playerStats", objectMapper.writeValueAsString(stat));
                
                if (archive) {
                    insertDataList.add(new HashMap<>(values));
                }
                upsertDataList.add(values);
            }
            
            if (archive && !insertDataList.isEmpty()) {
                persistenceLayer.batchInsert(ARCHIVE_TABLE_NAME, insertDataList);
                logger.debug("Batch archived {} player stats", insertDataList.size());
            }
            
            if (!upsertDataList.isEmpty()) {
                persistenceLayer.batchUpsert(TABLE_NAME, upsertDataList, 
                    new String[]{"playerId"}, new String[]{"playerStats"});
                logger.debug("Batch upserted {} player stats", upsertDataList.size());
            }
            
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to batch store PlayerStats", e);
        }
    }
    
    /**
     * Flushes pending insert operations (for archival)
     */
    public void flushInsertBatch() {
        batchLock.lock();
        try {
            if (!insertBatch.isEmpty()) {
                List<Map<String, Object>> toFlush = new ArrayList<>(insertBatch);
                insertBatch.clear();
                
                try {
                    persistenceLayer.batchInsert(ARCHIVE_TABLE_NAME, toFlush);
                    logger.debug("Flushed insert batch of {} player stats", toFlush.size());
                } catch (SQLException e) {
                    logger.error("Failed to flush insert batch", e);
                    // Add back to batch on failure
                    insertBatch.addAll(0, toFlush);
                }
            }
        } finally {
            batchLock.unlock();
        }
    }
    
    /**
     * Flushes pending upsert operations
     */
    public void flushUpsertBatch() {
        batchLock.lock();
        try {
            if (!upsertBatch.isEmpty()) {
                List<Map<String, Object>> toFlush = new ArrayList<>(upsertBatch);
                upsertBatch.clear();
                
                try {
                    persistenceLayer.batchUpsert(TABLE_NAME, toFlush, 
                        new String[]{"playerId"}, new String[]{"playerStats"});
                    logger.debug("Flushed upsert batch of {} player stats", toFlush.size());
                } catch (SQLException e) {
                    logger.error("Failed to flush upsert batch", e);
                    // Add back to batch on failure
                    upsertBatch.addAll(0, toFlush);
                }
            }
        } finally {
            batchLock.unlock();
        }
    }
    
    /**
     * Flushes all pending batches
     */
    public void flushAllBatches() {
        flushInsertBatch();
        flushUpsertBatch();
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(String playerSteamId) {
        // Flush batches before reading to ensure data consistency
        // TODO this design has to change, we are doing DB read/writes for player stats at every game event
        flushAllBatches();
        
        try {
            try (ResultSet queried = persistenceLayer.query(TABLE_NAME,
                    new String[]{"playerStats", "createdAt"}, "playerId = ?", playerSteamId)) {
                if (queried.next()) {
                    PlayerStats result = objectMapper.readValue(queried.getString("playerStats"), PlayerStats.class);
                    return Optional.of(result);
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to get PlayerStats", e);
        }

        return Optional.empty();
    }
}