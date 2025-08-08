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
import com.rankforge.core.events.GameOverEvent;
import com.rankforge.core.events.GameProcessedEvent;
import com.rankforge.core.events.RoundEndEvent;
import com.rankforge.core.events.RoundStartEvent;
import com.rankforge.core.interfaces.GameEventListener;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DAO layer for PlayerStats with batching support
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class DBBasedPlayerStatsStore implements PlayerStatsStore, GameEventListener {
    private static final Logger logger = LoggerFactory.getLogger(DBBasedPlayerStatsStore.class);
    private static final String TABLE_NAME = "PlayerStats";
    private static final String ARCHIVE_TABLE_NAME = "PlayerStatsArchive";
    private static final int DEFAULT_BATCH_SIZE = 50;

    private final PersistenceLayer persistenceLayer;
    private final ObjectMapper objectMapper;
    private final Map<String, PlayerStats> playerStatsMap;
    private final ReentrantLock batchLock = new ReentrantLock();

    
    public DBBasedPlayerStatsStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        this.persistenceLayer = persistenceLayer;
        this.objectMapper = objectMapper;
        this.playerStatsMap = new ConcurrentHashMap<>();
        this.createTable();
    }

    private void createTable() {
        ColumnDefinition[] columns = {
                new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                        .primaryKey()
                        .autoIncrement()
                        .build(),
                new ColumnDefinition.Builder("playerId", ColumnType.TEXT_SHORT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("playerStats", ColumnType.TEXT_LONG)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("createdAt", ColumnType.TEXT_SHORT)
                        .defaultValue("CURRENT_TIMESTAMP")
                        .build()
        };
        try {
            persistenceLayer.createTable(TABLE_NAME, columns, new String[]{"playerId"}, true);
            persistenceLayer.createTable(ARCHIVE_TABLE_NAME, columns, null, true);
        } catch (SQLException e) {
            logger.error("Unable to create table {}", TABLE_NAME, e);
            System.exit(1);
        }
    }

    @Override
    public void store(PlayerStats stat, boolean archive) {
        playerStatsMap.put(stat.getPlayerId(), stat);
    }
    
    /**
     * Stores multiple player stats in batches
     */
    private void storeBatch(Collection<PlayerStats> stats) {
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

                insertDataList.add(new HashMap<>(values));
                upsertDataList.add(values);
            }

            persistenceLayer.batchInsert(ARCHIVE_TABLE_NAME, insertDataList);
            logger.info("Batch archived {} player stats", insertDataList.size());

            persistenceLayer.batchUpsert(TABLE_NAME, upsertDataList,
                    new String[]{"playerId"}, new String[]{"playerStats"});
            logger.info("Batch upserted {} player stats", upsertDataList.size());

        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to batch store PlayerStats", e);
        }
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(String playerSteamId) {

        if (playerSteamId == null) {
            return Optional.empty();
        }

        return playerStatsMap.containsKey(playerSteamId) ? Optional.of(playerStatsMap.get(playerSteamId))
                : Optional.empty();
    }

    @Override
    public void onGameStarted(GameOverEvent event) {
        // no-op
    }

    @Override
    public void onGameEnded(GameProcessedEvent event) {
        storeBatch(playerStatsMap.values());
    }

    @Override
    public void onRoundStarted(RoundStartEvent event) {
        // no-op
    }

    @Override
    public void onRoundEnded(RoundEndEvent event) {
        // no-op
    }
}