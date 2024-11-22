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
import com.rankforge.core.events.GameActionEvent;
import com.rankforge.core.events.GameEvent;
import com.rankforge.core.stores.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO layer for events
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class DBBasedEventStore implements EventStore {
    private static final String TABLE_NAME = "GameEvent";
    private static final Logger logger = LoggerFactory.getLogger(DBBasedEventStore.class);
    private final PersistenceLayer persistenceLayer;
    private final ObjectMapper objectMapper;

    public DBBasedEventStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        this.persistenceLayer = persistenceLayer;
        this.objectMapper = new ObjectMapper();
        this.createTable();
    }

    private void createTable() {

        ColumnDefinition[] columns = {
                new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                        .primaryKey()
                        .autoIncrement()
                        .build(),
                new ColumnDefinition.Builder("event", ColumnType.TEXT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("at", ColumnType.TEXT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("created_at", ColumnType.TEXT)
                        .defaultValue("CURRENT_TIMESTAMP")
                        .build(),
                new ColumnDefinition.Builder("gameEventType", ColumnType.TEXT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("player1", ColumnType.TEXT)
                        .build(),
                new ColumnDefinition.Builder("player2", ColumnType.TEXT)
                        .build()
        };
        try {
            persistenceLayer.createTable(TABLE_NAME, columns, null, true);
        } catch (SQLException e) {
            logger.error("Unable to create table {}", TABLE_NAME);
        }
    }

    @Override
    public void store(GameEvent event) {

        try {
            Map<String, Object> values = new HashMap<>();
            values.put("event", objectMapper.writeValueAsString(event));
            values.put("at", event.getTimestamp());
            values.put("gameEventType", event.getGameEventType());
            if (event instanceof GameActionEvent gameActionEvent) {
                values.put("player1", gameActionEvent.getPlayer1().getSteamId());
                values.put("player2", gameActionEvent.getPlayer2().getSteamId());
            }
            persistenceLayer.insert(TABLE_NAME, values);
            //logger.debug("Stored event: {}", event);
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to store event", e);
        }
    }


    // TODO implement methods
    @Override
    public List<GameEvent> getEventsBetween(Instant start, Instant end) {
        return List.of();
    }

    @Override
    public List<GameEvent> getEventsForPlayer(String playerId, Instant start, Instant end) {
        return List.of();
    }

    @Override
    public void cleanup(Instant before) {

    }

    // Implementation of other methods...
}
