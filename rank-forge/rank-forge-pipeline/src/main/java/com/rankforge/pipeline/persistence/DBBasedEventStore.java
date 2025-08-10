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
import com.rankforge.core.events.*;
import com.rankforge.core.interfaces.GameEventListener;

import com.rankforge.core.stores.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;


/**
 * DAO layer for events with batching support
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class DBBasedEventStore implements EventStore, GameEventListener {
    private static final String TABLE_NAME = "GameEvent";
    private static final Logger logger = LoggerFactory.getLogger(DBBasedEventStore.class);
    
    private final PersistenceLayer persistenceLayer;
    private final ObjectMapper objectMapper;
    private final List<GameEvent> gameEvents;
    
    public DBBasedEventStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        this.persistenceLayer = persistenceLayer;
        this.objectMapper = objectMapper;
        this.gameEvents = new LinkedList<>();
        this.createTable();
    }

    private void createTable() {

        ColumnDefinition[] columns = {
                new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                        .primaryKey()
                        .autoIncrement()
                        .build(),
                new ColumnDefinition.Builder("event", ColumnType.TEXT_LONG)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("at", ColumnType.TEXT_SHORT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("created_at", ColumnType.TEXT_SHORT)
                        .defaultValue("CURRENT_TIMESTAMP")
                        .build(),
                new ColumnDefinition.Builder("gameEventType", ColumnType.TEXT_SHORT)
                        .notNull()
                        .build(),
                new ColumnDefinition.Builder("player1", ColumnType.TEXT_SHORT)
                        .build(),
                new ColumnDefinition.Builder("player2", ColumnType.TEXT_SHORT)
                        .build()
        };
        try {
            persistenceLayer.createTable(TABLE_NAME, columns, null, true);
        } catch (SQLException e) {
            logger.error("Unable to create table {}", TABLE_NAME, e);
            System.exit(1);
        }
    }

    @Override
    public void store(GameEvent event) {
        gameEvents.add(event);
        logger.debug("Stored event in-mem: {} at {}", event.getGameEventType(), event.getTimestamp());
    }
    
    /**
     * Stores multiple events in a batch
     */
    private void storeBatch(List<GameEvent> events) throws SQLException, JsonProcessingException {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        try {
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (GameEvent event : events) {
                Map<String, Object> values = new HashMap<>();
                values.put("event", objectMapper.writeValueAsString(event));
                values.put("at", event.getTimestamp().toString()); // Convert Instant to ISO-8601 string
                values.put("gameEventType", event.getGameEventType());
                if (event instanceof GameActionEvent gameActionEvent) {
                    values.put("player1", gameActionEvent.getPlayer1().getSteamId());
                    values.put("player2", gameActionEvent.getPlayer2().getSteamId());
                }
                dataList.add(values);
            }
            
            persistenceLayer.batchInsert(TABLE_NAME, dataList);
            logger.debug("Batch stored {} events", events.size());
            
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to batch store events", e);
            throw e;
        }
    }

    @Override
    public Optional<GameEvent> getGameEvent(GameEventType eventType, Instant timestamp) {
        try (ResultSet queried = persistenceLayer.query(TABLE_NAME,
                new String[]{"event"}, "gameEventType = ? AND at = ?", eventType, timestamp.toString())){
            if (queried.next()) {
                GameEvent result = objectMapper.readValue(queried.getString("event"), GameEvent.class);
                return Optional.of(result);
            }
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to get GameEvent", e);
        }
        return Optional.empty();
    }

    @Override
    public List<GameEvent> getGameOverEvents() {
        List<GameEvent> gameOverEvents = new ArrayList<>();
        try (ResultSet queried = persistenceLayer.query(TABLE_NAME,
                new String[]{"event"}, "gameEventType = ?", GameEventType.GAME_OVER)) {
            while (queried.next()) {
                GameEvent event = objectMapper.readValue(queried.getString("event"), GameEvent.class);
                gameOverEvents.add(event);
            }
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to get GameOver events", e);
        }
        return gameOverEvents;
    }

    @Override
    public List<GameEvent> getEventsBetween(GameEventType eventType, Instant startTime, Instant endTime) {
        List<GameEvent> events = new ArrayList<>();
        String startTimeStr = startTime.toString(); // Convert to ISO-8601 string
        String endTimeStr = endTime.toString();     // Convert to ISO-8601 string
        
        try (ResultSet queried = persistenceLayer.query(TABLE_NAME,
                new String[]{"event"}, "gameEventType = ? AND at BETWEEN ? AND ? ORDER BY at ASC", 
                eventType, startTimeStr, endTimeStr)) {
            while (queried.next()) {
                GameEvent event = objectMapper.readValue(queried.getString("event"), GameEvent.class);
                events.add(event);
            }
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to get events of type {} between {} and {}", eventType, startTimeStr, endTimeStr, e);
        }
        return events;
    }

    @Override
    public List<GameEvent> getRoundEndEventsBetween(Instant gameStartTime, Instant gameEndTime) {
        return getEventsBetween(GameEventType.ROUND_END, gameStartTime, gameEndTime);
    }

    @Override
    public void onGameStarted(GameOverEvent event) {
        gameEvents.clear();
    }

    @Override
    public void onGameEnded(GameProcessedEvent event) {
        try {
            storeBatch(gameEvents);
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to batch store events", e);
        }
    }

    @Override
    public void onRoundStarted(RoundStartEvent event) {
        // do nothing
    }

    @Override
    public void onRoundEnded(RoundEndEvent event) {
        // do nothing
    }

    // Implementation of other methods...
}