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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DAO layer for PlayerStats
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class DBBasedPlayerStatsStore implements PlayerStatsStore {
    private static final Logger logger = LoggerFactory.getLogger(DBBasedPlayerStatsStore.class);
    private static final String TABLE_NAME = "PlayerStats";
    private static final String ARCHIVE_TABLE_NAME = "PlayerStatsArchive";


    private final PersistenceLayer persistenceLayer;
    private final ObjectMapper objectMapper;

    public DBBasedPlayerStatsStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        this.persistenceLayer = persistenceLayer;
        this.objectMapper = objectMapper;
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

            if (archive) {
                persistenceLayer.insert(ARCHIVE_TABLE_NAME, values);
                logger.debug("Archived playerstat for player: {}", stat.getPlayerId());
            }

            persistenceLayer.upsert(TABLE_NAME, values, new String[]{"playerId"}, new String[]{"playerStats"});

            logger.debug("Stored/updated playerstat for player: {} (archive={})", stat.getPlayerId(), archive);
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to store PlayerStats", e);
        }
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(String playerSteamId) {
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
