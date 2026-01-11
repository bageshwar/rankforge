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
import com.rankforge.pipeline.persistence.repository.GameRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * JPA-based implementation of EventStore
 * Uses EventProcessingContext for direct entity reference linking.
 * Explicitly saves GameEntity first, then events, then accolades.
 * Author bageshwar.pn
 * Date 2026
 */
public class JpaEventStore implements EventStore, GameEventListener {
    private static final Logger logger = LoggerFactory.getLogger(JpaEventStore.class);
    
    private final GameEventRepository repository;
    private final AccoladeRepository accoladeRepository;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper; // Used for reading legacy data and RoundEndEvent players
    private final EventProcessingContext context;
    private EntityManager entityManager;
    
    public JpaEventStore(GameEventRepository repository, AccoladeRepository accoladeRepository,
                         GameRepository gameRepository, ObjectMapper objectMapper, 
                         EventProcessingContext context) {
        this.repository = repository;
        this.accoladeRepository = accoladeRepository;
        this.gameRepository = gameRepository;
        this.objectMapper = objectMapper;
        this.context = context;
    }
    
    /**
     * Sets the EntityManager for direct persistence operations.
     * Called by Spring via @PersistenceContext or explicitly from config.
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Constructor without GameRepository for backward compatibility.
     * Note: This will not persist GameEntity separately.
     */
    public JpaEventStore(GameEventRepository repository, AccoladeRepository accoladeRepository,
                         ObjectMapper objectMapper, EventProcessingContext context) {
        this(repository, accoladeRepository, null, objectMapper, context);
    }

    @Override
    public void store(GameEvent event) {
        try {
            GameEventEntity entity = convertToEntity(event);
            
            // Register player name -> Steam ID mappings for events with player info
            // This is used later to resolve accolade player IDs
            registerPlayerMappings(event);
            
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
     * Register player name to Steam ID mappings from game action events.
     * This builds a lookup table used to resolve accolade player IDs.
     */
    private void registerPlayerMappings(GameEvent event) {
        if (event instanceof GameActionEvent gameActionEvent) {
            if (gameActionEvent.getPlayer1() != null) {
                context.registerPlayer(
                    gameActionEvent.getPlayer1().getName(),
                    gameActionEvent.getPlayer1().getSteamId()
                );
            }
            if (gameActionEvent.getPlayer2() != null) {
                context.registerPlayer(
                    gameActionEvent.getPlayer2().getName(),
                    gameActionEvent.getPlayer2().getSteamId()
                );
            }
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
                    // Serialize coordinates to JSON
                    killEntity.setCoordinates(serializeCoordinates(killEvent));
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
                    // Serialize coordinates to JSON (may be null if not present in log)
                    assistEntity.setCoordinates(serializeCoordinates(assistEvent));
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
                    // Serialize coordinates to JSON
                    attackEntity.setCoordinates(serializeCoordinates(attackEvent));
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
     * Serialize coordinates from GameActionEvent to JSON string.
     * Format: {"player1": {"x": 100, "y": 200, "z": 50}, "player2": {"x": 150, "y": 250, "z": 60}}
     * Returns null if no coordinates are present.
     */
    private String serializeCoordinates(GameActionEvent event) throws JsonProcessingException {
        // Check if any coordinates are present
        if (event.getPlayer1X() == null && event.getPlayer1Y() == null && event.getPlayer1Z() == null &&
            event.getPlayer2X() == null && event.getPlayer2Y() == null && event.getPlayer2Z() == null) {
            return null; // No coordinates to store
        }
        
        // Build coordinate map
        Map<String, Object> coordinates = new HashMap<>();
        
        // Player1 coordinates
        if (event.getPlayer1X() != null || event.getPlayer1Y() != null || event.getPlayer1Z() != null) {
            Map<String, Integer> player1Coords = new HashMap<>();
            if (event.getPlayer1X() != null) player1Coords.put("x", event.getPlayer1X());
            if (event.getPlayer1Y() != null) player1Coords.put("y", event.getPlayer1Y());
            if (event.getPlayer1Z() != null) player1Coords.put("z", event.getPlayer1Z());
            coordinates.put("player1", player1Coords);
        }
        
        // Player2 coordinates
        if (event.getPlayer2X() != null || event.getPlayer2Y() != null || event.getPlayer2Z() != null) {
            Map<String, Integer> player2Coords = new HashMap<>();
            if (event.getPlayer2X() != null) player2Coords.put("x", event.getPlayer2X());
            if (event.getPlayer2Y() != null) player2Coords.put("y", event.getPlayer2Y());
            if (event.getPlayer2Z() != null) player2Coords.put("z", event.getPlayer2Z());
            coordinates.put("player2", player2Coords);
        }
        
        // Return null if no coordinates were added
        if (coordinates.isEmpty()) {
            return null;
        }
        
        return objectMapper.writeValueAsString(coordinates);
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
    public void onGameStarted(GameOverEvent event) {
        // Note: Do NOT clear context here!
        // The context is set up in EventProcessorImpl.visit(GameOverEvent) BEFORE this is called.
        // If we clear here, we lose the GameEntity reference.
        // Context is cleared in onGameEnded() after batch persistence.
        logger.debug("Game started event received - context ready for round/event processing");
    }

    @Override
    @Transactional
    public void onGameEnded(GameProcessedEvent event) {
        try {
            // Validate EntityManager is available and open
            if (entityManager == null) {
                throw new IllegalStateException("EntityManager is not available. " +
                        "Ensure JpaEventStore is properly configured with an EntityManager.");
            }
            
            if (!entityManager.isOpen()) {
                throw new IllegalStateException("EntityManager is closed. " +
                        "Cannot persist game data with a closed EntityManager.");
            }
            
            // Check if we need to manage the transaction ourselves
            // This happens when JpaEventStore is created manually (not via Spring DI)
            // and the @Transactional annotation is not intercepted by Spring's proxy
            boolean managedTransaction = false;
            try {
                if (!entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().begin();
                    managedTransaction = true;
                    logger.debug("Started manual transaction for game persistence");
                }
            } catch (IllegalStateException e) {
                // EntityManager might be closed or in an invalid state
                logger.error("Cannot begin transaction - EntityManager may be closed", e);
                throw new IllegalStateException("Cannot begin transaction", e);
            }
            
            try {
                persistGameData();
                
                if (managedTransaction) {
                    try {
                        if (entityManager.getTransaction().isActive()) {
                            entityManager.getTransaction().commit();
                            logger.debug("Committed manual transaction for game persistence");
                        }
                    } catch (Exception e) {
                        logger.error("Error committing transaction", e);
                        throw e;
                    }
                }
            } catch (Exception e) {
                if (managedTransaction) {
                    try {
                        if (entityManager.getTransaction().isActive()) {
                            entityManager.getTransaction().rollback();
                            logger.debug("Rolled back manual transaction due to error");
                        }
                    } catch (Exception rollbackException) {
                        logger.error("Error rolling back transaction", rollbackException);
                        // Don't throw - the original exception is more important
                    }
                }
                throw e;
            }
        } catch (Exception e) {
            logger.error("Failed to persist game data", e);
            throw new RuntimeException("Failed to persist game data", e);
        }
    }
    
    /**
     * Internal method that performs the actual persistence of game data.
     * Should be called within a transaction context.
     */
    private void persistGameData() {
        long startTime = System.currentTimeMillis();
        // Use EntityManager directly to ensure all entities stay in the same persistence context
        // This avoids the "detached entity" issue when repository calls create boundaries
        
        // 1. First, persist or merge GameEntity to get its ID
        GameEntity game = context.getCurrentGame();
        
        if (game != null) {
            long gameStartTime = System.currentTimeMillis();
            // Use merge instead of persist to handle both new and detached entities
            // If game.id is null, merge behaves like persist
            // If game.id is set (detached entity), merge re-attaches it
            if (game.getId() == null) {
                entityManager.persist(game);
            } else {
                game = entityManager.merge(game);
            }
            
            entityManager.flush(); // Flush to get the generated ID
            
            long gameTime = System.currentTimeMillis() - gameStartTime;
            logger.info("Persisted GameEntity with ID {} for game on map {} (took {}ms)", 
                    game.getId(), game.getMap(), gameTime);
            
            // Update all references to point to the managed entity
            // Important: must use the returned entity from merge
            updateGameReferences(game);
        }
        
        // 2. Persist all game events using EntityManager (same persistence context)
        // First pass: persist RoundStartEventEntity instances to get their IDs
        // This is needed because other events may reference them
        // Create defensive copy to prevent ConcurrentModificationException
        List<GameEventEntity> entitiesToSave = new ArrayList<>(context.getPendingEntities());
        Map<RoundStartEventEntity, RoundStartEventEntity> roundStartMap = new HashMap<>();
        
        logger.debug("Persisting {} total events for game", entitiesToSave.size());
        
        long roundStartPersistStart = System.currentTimeMillis();
        int roundStartCount = 0;
        
        for (GameEventEntity entity : entitiesToSave) {
            if (entity instanceof RoundStartEventEntity roundStart) {
                // Ensure game reference is managed
                if (game != null && !entityManager.contains(game)) {
                    game = entityManager.merge(game);
                }
                roundStart.setGame(game);
                
                // Check if entity already has an ID (detached entity) - use merge instead of persist
                if (roundStart.getId() == null) {
                    entityManager.persist(roundStart);
                } else {
                    // Entity already has ID, merge it to re-attach to persistence context
                    roundStart = entityManager.merge(roundStart);
                }
                roundStartMap.put(roundStart, roundStart); // Track for reference updates
                roundStartCount++;
            }
        }
        long roundStartPersistTime = System.currentTimeMillis() - roundStartPersistStart;
        logger.debug("Persisted {} RoundStartEventEntity instances (took {}ms)", roundStartCount, roundStartPersistTime);
        
        // Flush to get RoundStartEventEntity IDs assigned
        if (!roundStartMap.isEmpty()) {
            entityManager.flush();
        }
        
        // Build cache of managed roundStart entities by ID (after flush, they're all managed)
        // This avoids expensive contains() checks in the loop
        Map<Long, RoundStartEventEntity> managedRoundStartCache = new HashMap<>();
        for (RoundStartEventEntity roundStart : roundStartMap.keySet()) {
            if (roundStart.getId() != null) {
                managedRoundStartCache.put(roundStart.getId(), roundStart);
            }
        }
        
        // Ensure game is managed (check once, not 744 times)
        if (game != null && !entityManager.contains(game)) {
            game = entityManager.merge(game);
        }
        
        // Second pass: persist all other events
        // With SEQUENCE generation, Hibernate can batch inserts for better performance
        long otherEventsPersistStart = System.currentTimeMillis();
        int otherEventCount = 0;
        int eventsWithRoundRef = 0;
        int eventsWithoutRoundRef = 0;
        Map<Long, Integer> eventsPerRound = new HashMap<>();
        
        final int BATCH_SIZE = 50; // Match hibernate.jdbc.batch_size
        int batchCount = 0;
        
        for (GameEventEntity entity : entitiesToSave) {
            if (!(entity instanceof RoundStartEventEntity)) {
                // Game should already be managed (checked once above)
                entity.setGame(game);
                
                // Ensure roundStart reference is set and managed (if applicable)
                RoundStartEventEntity roundStart = entity.getRoundStart();
                if (roundStart != null) {
                    // roundStart should have been persisted in the first pass
                    // Verify it has an ID and is managed
                    if (roundStart.getId() == null) {
                        logger.warn("PERSIST_ROUND: RoundStart has null ID for event type {} at {}", 
                                entity.getGameEventType(), entity.getTimestamp());
                        eventsWithoutRoundRef++;
                    } else {
                        eventsWithRoundRef++;
                        eventsPerRound.merge(roundStart.getId(), 1, Integer::sum);
                        
                        // Use cached managed entity instead of checking contains()
                        RoundStartEventEntity managedRoundStart = managedRoundStartCache.get(roundStart.getId());
                        if (managedRoundStart != null) {
                            // Use the cached managed entity
                            entity.setRoundStart(managedRoundStart);
                        } else {
                            // Not in cache (shouldn't happen), merge it
                            logger.debug("PERSIST_ROUND: RoundStart ID {} not in cache, merging for event {}", 
                                    roundStart.getId(), entity.getGameEventType());
                            managedRoundStart = entityManager.merge(roundStart);
                            entity.setRoundStart(managedRoundStart);
                            // Add to cache for future use
                            managedRoundStartCache.put(roundStart.getId(), managedRoundStart);
                        }
                    }
                } else {
                    eventsWithoutRoundRef++;
                    // Log which event types don't have round reference
                    if (entity.getGameEventType() != null) {
                        String eventType = entity.getGameEventType().name();
                        // Only warn for event types that SHOULD have a round reference
                        if (!eventType.equals("GAME_OVER") && !eventType.equals("GAME_PROCESSED")) {
                            logger.debug("PERSIST_ROUND: Event {} at {} has NO roundStart reference", 
                                    eventType, entity.getTimestamp());
                        }
                    }
                }
                
                // Check if entity already has an ID (detached entity) - use merge instead of persist
                if (entity.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    // Entity already has ID, merge it to re-attach to persistence context
                    entityManager.merge(entity);
                }
                otherEventCount++;
                batchCount++;
                
                // Flush periodically to reduce memory pressure and enable batch processing
                if (batchCount >= BATCH_SIZE) {
                    entityManager.flush();
                    batchCount = 0;
                }
            }
        }
        
        // Flush any remaining entities
        if (batchCount > 0) {
            entityManager.flush();
        }
        
        long otherEventsPersistTime = System.currentTimeMillis() - otherEventsPersistStart;
        logger.info("Persisted {} other events (took {}ms)", otherEventCount, otherEventsPersistTime);
        
        if (!entitiesToSave.isEmpty()) {
            logger.debug("Persisted {} game events ({} round starts, {} other events)", 
                    entitiesToSave.size(), roundStartCount, otherEventCount);
            logger.debug("Events with roundStart: {}, without: {}", 
                    eventsWithRoundRef, eventsWithoutRoundRef);
        }
        
        // 3. Persist all accolades using EntityManager (same persistence context)
        long accoladesStart = System.currentTimeMillis();
        // Create defensive copy to prevent ConcurrentModificationException
        List<AccoladeEntity> accoladesToSave = new ArrayList<>(context.getPendingAccolades());
        
        
        for (AccoladeEntity accolade : accoladesToSave) {
            // Check if the game reference is managed; if not, re-attach it
            GameEntity accoladeGame = accolade.getGame();
            if (accoladeGame != null && accoladeGame.getId() != null && !entityManager.contains(accoladeGame)) {
                accolade.setGame(entityManager.merge(accoladeGame));
            }
            
            // Check if entity already has an ID (detached entity) - use merge instead of persist
            if (accolade.getId() == null) {
                entityManager.persist(accolade);
            } else {
                // Entity already has ID, merge it to re-attach to persistence context
                entityManager.merge(accolade);
            }
        }
        long accoladesTime = System.currentTimeMillis() - accoladesStart;
        if (!accoladesToSave.isEmpty()) {
            logger.info("Persisted {} accolades (took {}ms)", accoladesToSave.size(), accoladesTime);
        }
        
        // Flush all pending changes to database
        long finalFlushStart = System.currentTimeMillis();
        entityManager.flush();
        long finalFlushTime = System.currentTimeMillis() - finalFlushStart;
        logger.debug("Final flush complete (took {}ms)", finalFlushTime);
        
        context.clear();
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Total persistGameData() time: {}ms", totalTime);
    }
    
    /**
     * Updates all pending entities and accolades to reference the saved GameEntity.
     * This is necessary because the saved entity has an ID assigned by the database
     * and is now a managed entity in the persistence context.
     */
    private void updateGameReferences(GameEntity savedGame) {
        // Also update the context's current game reference to the managed entity
        context.setCurrentGame(savedGame);
        
        // Create defensive copies to prevent ConcurrentModificationException
        List<GameEventEntity> entities = new ArrayList<>(context.getPendingEntities());
        List<AccoladeEntity> accolades = new ArrayList<>(context.getPendingAccolades());
        
        for (GameEventEntity entity : entities) {
            entity.setGame(savedGame);
        }
        for (AccoladeEntity accolade : accolades) {
            accolade.setGame(savedGame);
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
