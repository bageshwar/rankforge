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
import com.rankforge.pipeline.persistence.entity.*;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * JPA-based implementation of EventStore
 * Author bageshwar.pn
 * Date 2026
 */
public class JpaEventStore implements EventStore, GameEventListener {
    private static final Logger logger = LoggerFactory.getLogger(JpaEventStore.class);
    
    private final GameEventRepository repository;
    private final ObjectMapper objectMapper;
    private final List<GameEvent> gameEvents;
    
    public JpaEventStore(GameEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.gameEvents = new LinkedList<>();
    }

    @Override
    public void store(GameEvent event) {
        gameEvents.add(event);
        logger.debug("Stored event in-mem: {} at {}", event.getGameEventType(), event.getTimestamp());
    }
    
    /**
     * Stores multiple events in a batch
     */
    @Transactional
    private void storeBatch(List<GameEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        try {
            List<GameEventEntity> entities = new ArrayList<>();
            for (GameEvent event : events) {
                GameEventEntity entity = convertToEntity(event);
                entities.add(entity);
            }
            
            repository.saveAll(entities);
            logger.debug("Batch stored {} events", events.size());
            
        } catch (Exception e) {
            logger.error("Failed to batch store events", e);
            throw new RuntimeException("Failed to batch store events", e);
        }
    }
    
    /**
     * Convert GameEvent domain object to GameEventEntity
     */
    private GameEventEntity convertToEntity(GameEvent event) throws JsonProcessingException {
        GameEventEntity entity;
        Instant timestamp = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
        
        // Create appropriate entity subclass based on event type
        switch (event.getGameEventType()) {
            case KILL:
                KillEventEntity killEntity = new KillEventEntity(timestamp);
                if (event instanceof KillEvent killEvent) {
                    killEntity.setIsHeadshot(killEvent.isHeadshot());
                    killEntity.setWeapon(killEvent.getWeapon());
                    if (killEvent.getPlayer1() != null) {
                        killEntity.setPlayer1(killEvent.getPlayer1().getSteamId());
                    }
                    if (killEvent.getPlayer2() != null) {
                        killEntity.setPlayer2(killEvent.getPlayer2().getSteamId());
                    }
                }
                entity = killEntity;
                break;
            case ASSIST:
                AssistEventEntity assistEntity = new AssistEventEntity(timestamp);
                if (event instanceof AssistEvent assistEvent) {
                    assistEntity.setAssistType(assistEvent.getAssistType() != null ? assistEvent.getAssistType().name() : null);
                    assistEntity.setWeapon(assistEvent.getWeapon());
                    if (assistEvent.getPlayer1() != null) {
                        assistEntity.setPlayer1(assistEvent.getPlayer1().getSteamId());
                    }
                    if (assistEvent.getPlayer2() != null) {
                        assistEntity.setPlayer2(assistEvent.getPlayer2().getSteamId());
                    }
                }
                entity = assistEntity;
                break;
            case ATTACK:
                AttackEventEntity attackEntity = new AttackEventEntity(timestamp);
                if (event instanceof AttackEvent attackEvent) {
                    attackEntity.setDamage(attackEvent.getDamage());
                    attackEntity.setArmorDamage(attackEvent.getArmorDamage());
                    attackEntity.setHitGroup(attackEvent.getHitGroup());
                    attackEntity.setWeapon(attackEvent.getWeapon());
                    if (attackEvent.getPlayer1() != null) {
                        attackEntity.setPlayer1(attackEvent.getPlayer1().getSteamId());
                    }
                    if (attackEvent.getPlayer2() != null) {
                        attackEntity.setPlayer2(attackEvent.getPlayer2().getSteamId());
                    }
                }
                entity = attackEntity;
                break;
            case ROUND_END:
                RoundEndEventEntity roundEndEntity = new RoundEndEventEntity(timestamp);
                if (event instanceof RoundEndEvent roundEndEvent) {
                    if (roundEndEvent.getPlayers() != null) {
                        roundEndEntity.setPlayersJson(objectMapper.writeValueAsString(roundEndEvent.getPlayers()));
                    }
                }
                entity = roundEndEntity;
                break;
            case ROUND_START:
                entity = new RoundStartEventEntity(timestamp);
                break;
            case GAME_OVER:
                GameOverEventEntity gameOverEntity = new GameOverEventEntity(timestamp);
                if (event instanceof GameOverEvent gameOverEvent) {
                    gameOverEntity.setMap(gameOverEvent.getMap());
                    gameOverEntity.setMode(gameOverEvent.getMode());
                    gameOverEntity.setTeam1Score(gameOverEvent.getTeam1Score());
                    gameOverEntity.setTeam2Score(gameOverEvent.getTeam2Score());
                    gameOverEntity.setDuration(gameOverEvent.getDuration());
                }
                entity = gameOverEntity;
                break;
            case BOMB_EVENT:
                BombEventEntity bombEntity = new BombEventEntity(timestamp);
                if (event instanceof BombEvent bombEvent) {
                    bombEntity.setPlayer(bombEvent.getPlayer());
                    bombEntity.setEventType(bombEvent.getEventType() != null ? bombEvent.getEventType().name() : null);
                    bombEntity.setTimeRemaining(bombEvent.getTimeRemaining());
                }
                entity = bombEntity;
                break;
            case GAME_PROCESSED:
                entity = new GameProcessedEventEntity(timestamp);
                break;
            default:
                // Fallback - create a generic entity (should not happen in practice)
                // Use RoundStartEventEntity as a safe default since it has no special fields
                entity = new RoundStartEventEntity(timestamp);
                entity.setGameEventType(event.getGameEventType());
        }
        
        // Store JSON for backward compatibility
        entity.setEventJson(objectMapper.writeValueAsString(event));
        
        return entity;
    }
    
    /**
     * Convert GameEventEntity to GameEvent domain object
     */
    private GameEvent convertToDomain(GameEventEntity entity) {
        try {
            // Try to deserialize from JSON first (for backward compatibility)
            if (entity.getEventJson() != null && !entity.getEventJson().isEmpty()) {
                return objectMapper.readValue(entity.getEventJson(), GameEvent.class);
            }
            
            // If no JSON, reconstruct from entity fields (for new data)
            // This is a fallback - ideally all data should have JSON
            logger.warn("Entity {} has no JSON, attempting to reconstruct from fields", entity.getId());
            return null; // Would need complex reconstruction logic
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize event from JSON", e);
            return null;
        }
    }

    @Override
    public Optional<GameEvent> getGameEvent(GameEventType eventType, Instant timestamp) {
        try {
            List<GameEventEntity> entities = repository.findByGameEventTypeAndTimestamp(eventType, timestamp);
            if (!entities.isEmpty()) {
                GameEvent event = convertToDomain(entities.get(0));
                return Optional.ofNullable(event);
            }
        } catch (Exception e) {
            logger.error("Failed to get GameEvent", e);
        }
        return Optional.empty();
    }

    @Override
    public List<GameEvent> getGameOverEvents() {
        List<GameEvent> gameOverEvents = new ArrayList<>();
        try {
            List<GameEventEntity> entities = repository.findByGameEventType(GameEventType.GAME_OVER);
            for (GameEventEntity entity : entities) {
                GameEvent event = convertToDomain(entity);
                if (event != null) {
                    gameOverEvents.add(event);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get GameOver events", e);
        }
        return gameOverEvents;
    }

    @Override
    public List<GameEvent> getEventsBetween(GameEventType eventType, Instant startTime, Instant endTime) {
        List<GameEvent> events = new ArrayList<>();
        try {
            List<GameEventEntity> entities = repository.findByGameEventTypeAndTimestampBetween(eventType, startTime, endTime);
            for (GameEventEntity entity : entities) {
                GameEvent event = convertToDomain(entity);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get events of type {} between {} and {}", eventType, startTime, endTime, e);
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
    @Transactional
    public void onGameEnded(GameProcessedEvent event) {
        storeBatch(gameEvents);
        gameEvents.clear();
    }

    @Override
    public void onRoundStarted(RoundStartEvent event) {
        // do nothing
    }

    @Override
    public void onRoundEnded(RoundEndEvent event) {
        // do nothing
    }
}
