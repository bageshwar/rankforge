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
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * JPA-based implementation of EventStore
 * Uses EventProcessingContext for direct entity reference linking.
 * Author bageshwar.pn
 * Date 2026
 */
public class JpaEventStore implements EventStore, GameEventListener {
    private static final Logger logger = LoggerFactory.getLogger(JpaEventStore.class);
    
    private final GameEventRepository repository;
    private final AccoladeRepository accoladeRepository;
    private final ObjectMapper objectMapper; // Used for reading legacy data and RoundEndEvent players
    private final EventProcessingContext context;
    
    public JpaEventStore(GameEventRepository repository, AccoladeRepository accoladeRepository, 
                         ObjectMapper objectMapper, EventProcessingContext context) {
        this.repository = repository;
        this.accoladeRepository = accoladeRepository;
        this.objectMapper = objectMapper;
        this.context = context;
    }

    @Override
    public void store(GameEvent event) {
        try {
            GameEventEntity entity = convertToEntity(event);
            
            // Use context to set references and add to pending list based on event type
            switch (event.getGameEventType()) {
                case ROUND_START:
                    context.onRoundStart((RoundStartEventEntity) entity);
                    break;
                case ROUND_END:
                    context.onRoundEnd((RoundEndEventEntity) entity);
                    break;
                case GAME_OVER:
                    context.addGameOverEvent(entity);
                    break;
                case GAME_PROCESSED:
                    // Don't add to pending - this triggers persistence
                    break;
                default:
                    // KILL, ASSIST, ATTACK, BOMB_EVENT
                    context.addEvent(entity);
            }
            
            logger.debug("Stored event in context: {} at {}", event.getGameEventType(), event.getTimestamp());
        } catch (Exception e) {
            logger.error("Failed to store event: {}", event.getGameEventType(), e);
            throw new RuntimeException("Failed to store event", e);
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
                entity = new RoundStartEventEntity(timestamp);
                entity.setGameEventType(event.getGameEventType());
        }
        
        return entity;
    }
    
    /**
     * Convert GameEventEntity to GameEvent domain object (for legacy data with JSON)
     */
    private GameEvent convertToDomain(GameEventEntity entity) {
        // Reconstruct from entity fields based on type
        // This is needed for queries that return data persisted without JSON
        try {
            // For now, return null if we can't convert - caller should handle
            // Future: implement full reconstruction from entity fields
            logger.debug("Converting entity {} to domain", entity.getId());
            return null;
        } catch (Exception e) {
            logger.error("Failed to convert entity to domain", e);
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
        context.clear();
    }

    @Override
    @Transactional
    public void onGameEnded(GameProcessedEvent event) {
        try {
            // Persist all pending game events
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            if (!pendingEntities.isEmpty()) {
                repository.saveAll(pendingEntities);
                logger.info("Persisted {} game events", pendingEntities.size());
            }
            
            // Persist all pending accolades
            List<AccoladeEntity> pendingAccolades = context.getPendingAccolades();
            if (!pendingAccolades.isEmpty()) {
                accoladeRepository.saveAll(pendingAccolades);
                logger.info("Persisted {} accolades", pendingAccolades.size());
            }
            
            context.clear();
        } catch (Exception e) {
            logger.error("Failed to persist game data", e);
            throw new RuntimeException("Failed to persist game data", e);
        }
    }

    @Override
    public void onRoundStarted(RoundStartEvent event) {
        // Events are handled via store() method
    }

    @Override
    public void onRoundEnded(RoundEndEvent event) {
        // Events are handled via store() method
    }
    
    /**
     * Get the processing context (for use by other components like AccoladeStore)
     */
    public EventProcessingContext getContext() {
        return context;
    }
}
