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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.*;
import com.rankforge.core.models.Player;
import com.rankforge.pipeline.persistence.entity.*;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Tests for JpaEventStore persistence behavior.
 * These tests verify that entities are correctly prepared and persisted.
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class JpaEventStorePersistenceTest {

    private JpaEventStore eventStore;
    private GameEventRepository gameEventRepository;
    private AccoladeRepository accoladeRepository;
    private GameRepository gameRepository;
    private EventProcessingContext context;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        gameEventRepository = mock(GameEventRepository.class);
        accoladeRepository = mock(AccoladeRepository.class);
        gameRepository = mock(GameRepository.class);
        objectMapper = new ObjectMapper();
        context = new EventProcessingContext();
        
        // Configure gameRepository.save() to return the entity with an ID
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity game = invocation.getArgument(0);
            game.setId(1L); // Simulate database assigning an ID
            return game;
        });
        
        eventStore = new JpaEventStore(gameEventRepository, accoladeRepository, gameRepository, 
                objectMapper, context);
    }

    // ========================================================================
    // REQUIREMENT 1: Game table should have entries after processing
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 1: GameEntity should be saved when onGameEnded is called")
    void gameEntityShouldBeSavedOnGameEnded() {
        // Given: A game has been set in context
        GameEntity game = createTestGame();
        context.setCurrentGame(game);
        
        // And: Some events have been added
        RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
        context.onRoundStart(roundStart);
        
        KillEventEntity kill = new KillEventEntity(Instant.now());
        context.addEvent(kill);
        
        RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
        context.onRoundEnd(roundEnd);
        
        // When: Game ends
        eventStore.onGameEnded(createGameProcessedEvent());
        
        // Then: Verify GameRepository.save() was called to persist the game
        verify(gameRepository, times(1)).save(game);
        
        // And: Verify saveAll was called on game event repository
        verify(gameEventRepository, times(1)).saveAll(anyList());
        
        // Capture what was saved
        ArgumentCaptor<List<GameEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameEventRepository).saveAll(captor.capture());
        
        List<GameEventEntity> savedEntities = captor.getValue();
        assertFalse(savedEntities.isEmpty(), "Should have saved some entities");
        
        // CRITICAL: Every entity should have a game reference with an ID
        for (GameEventEntity entity : savedEntities) {
            assertNotNull(entity.getGame(), 
                    "Entity " + entity.getClass().getSimpleName() + " must have game reference");
            assertNotNull(entity.getGame().getId(),
                    "Game reference must have an ID assigned");
        }
    }

    // ========================================================================
    // REQUIREMENT 2: All GameEvents should have gameId
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 2: All stored events should have game reference before persistence")
    void allStoredEventsShouldHaveGameReference() {
        // Given: A game context is set up
        GameEntity game = createTestGame();
        context.setCurrentGame(game);
        
        // When: We store various events
        eventStore.store(new RoundStartEvent(Instant.now(), null));
        eventStore.store(createKillEvent(Instant.now()));
        eventStore.store(new RoundEndEvent(Instant.now(), null));
        
        // Then: All pending entities should have game reference
        List<GameEventEntity> pending = context.getPendingEntities();
        assertEquals(3, pending.size());
        
        for (GameEventEntity entity : pending) {
            assertNotNull(entity.getGame(),
                    entity.getClass().getSimpleName() + " should have game reference");
            assertSame(game, entity.getGame(),
                    "Should reference the same game instance");
        }
    }

    // ========================================================================
    // REQUIREMENT 3: All rounds should have distinct roundStartEventId
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 3: Events within a round should reference that round's start")
    void eventsWithinRoundShouldReferenceRoundStart() {
        // Given: A game context
        GameEntity game = createTestGame();
        context.setCurrentGame(game);
        
        // When: We process a round with events
        RoundStartEvent roundStart = new RoundStartEvent(Instant.now(), null);
        eventStore.store(roundStart);
        
        // Store events within the round
        eventStore.store(createKillEvent(Instant.now()));
        
        // Then: Kill event should reference the round start
        List<GameEventEntity> pending = context.getPendingEntities();
        
        RoundStartEventEntity roundStartEntity = (RoundStartEventEntity) pending.stream()
                .filter(e -> e instanceof RoundStartEventEntity)
                .findFirst()
                .orElseThrow();
        
        KillEventEntity killEntity = (KillEventEntity) pending.stream()
                .filter(e -> e instanceof KillEventEntity)
                .findFirst()
                .orElseThrow();
        
        assertSame(roundStartEntity, killEntity.getRoundStart(),
                "Kill event should reference the round start entity");
    }

    @Test
    @DisplayName("REQUIREMENT 3: Processing multiple rounds should create distinct round references")
    void processingMultipleRoundsShouldCreateDistinctReferences() {
        // Given: A game context
        GameEntity game = createTestGame();
        context.setCurrentGame(game);
        
        List<RoundStartEventEntity> roundStarts = new ArrayList<>();
        List<KillEventEntity> kills = new ArrayList<>();
        
        // When: We process 5 rounds
        for (int i = 0; i < 5; i++) {
            Instant roundTime = Instant.now().plusSeconds(i * 60);
            
            // Store round start
            eventStore.store(new RoundStartEvent(roundTime, null));
            
            // Get the round start that was just added
            RoundStartEventEntity roundStart = (RoundStartEventEntity) context.getPendingEntities()
                    .stream()
                    .filter(e -> e instanceof RoundStartEventEntity)
                    .reduce((first, second) -> second) // Get last
                    .orElseThrow();
            roundStarts.add(roundStart);
            
            // Store kill within this round
            eventStore.store(createKillEvent(roundTime.plusSeconds(10)));
            
            // Get the kill that was just added
            KillEventEntity kill = (KillEventEntity) context.getPendingEntities()
                    .stream()
                    .filter(e -> e instanceof KillEventEntity)
                    .reduce((first, second) -> second)
                    .orElseThrow();
            kills.add(kill);
            
            // Store round end
            eventStore.store(new RoundEndEvent(roundTime.plusSeconds(55), null));
        }
        
        // Then: Each kill should reference its respective round start
        assertEquals(5, roundStarts.size());
        assertEquals(5, kills.size());
        
        for (int i = 0; i < 5; i++) {
            assertSame(roundStarts.get(i), kills.get(i).getRoundStart(),
                    "Kill " + i + " should reference round start " + i);
        }
    }

    // ========================================================================
    // REQUIREMENT 4: Accolades should be persisted
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 4: Accolades should be saved when onGameEnded is called")
    void accoladesShouldBeSavedOnGameEnded() {
        // Given: A game with accolades
        GameEntity game = createTestGame();
        
        // Add accolades before game (simulating parser behavior)
        AccoladeEntity accolade1 = createAccolade("MVP", "Player1");
        AccoladeEntity accolade2 = createAccolade("TopKiller", "Player2");
        context.addAccolade(accolade1);
        context.addAccolade(accolade2);
        
        // Set game and link
        context.setCurrentGame(game);
        context.linkAccoladesToGame();
        
        // Add a game over event
        GameOverEventEntity gameOver = new GameOverEventEntity(Instant.now());
        context.addGameOverEvent(gameOver);
        
        // When: Game ends
        eventStore.onGameEnded(createGameProcessedEvent());
        
        // Then: Accolades should be saved
        verify(accoladeRepository, times(1)).saveAll(anyList());
        
        ArgumentCaptor<List<AccoladeEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(accoladeRepository).saveAll(captor.capture());
        
        List<AccoladeEntity> savedAccolades = captor.getValue();
        assertEquals(2, savedAccolades.size(), "Should save 2 accolades");
        
        // Verify all accolades have game reference
        for (AccoladeEntity accolade : savedAccolades) {
            assertNotNull(accolade.getGame(), 
                    "Accolade " + accolade.getType() + " must have game reference");
        }
    }

    @Test
    @DisplayName("REQUIREMENT 4: Accolades added before game should still be linked correctly")
    void accoladesAddedBeforeGameShouldBeLinkedCorrectly() {
        // Given: Accolades added to context (simulating parser order)
        AccoladeEntity accolade = createAccolade("MVP", "TestPlayer");
        context.addAccolade(accolade);
        
        // Game doesn't exist yet
        assertNull(accolade.getGame(), "Initially accolade has no game");
        
        // When: Game is created and linked
        GameEntity game = createTestGame();
        context.setCurrentGame(game);
        context.linkAccoladesToGame();
        
        // Then: Accolade should now have game reference
        assertNotNull(accolade.getGame(), "Accolade should have game after linking");
        assertSame(game, accolade.getGame(), "Should reference the correct game");
    }

    // ========================================================================
    // INTEGRATION: Full persistence flow
    // ========================================================================

    @Test
    @DisplayName("INTEGRATION: Complete game persistence should save game, events, and accolades")
    void completeGamePersistenceShouldSaveAllEntities() {
        // Simulate the full flow as it happens in production
        
        // 1. Accolades are parsed first (in CS2LogParser before GAME_OVER is returned)
        AccoladeEntity accolade1 = createAccolade("MVP", "Player1");
        AccoladeEntity accolade2 = createAccolade("TopKiller", "Player2");
        context.addAccolade(accolade1);
        context.addAccolade(accolade2);
        
        // 2. GAME_OVER is detected - EventProcessor creates GameEntity
        GameEntity game = createTestGame();
        context.setCurrentGame(game);
        context.linkAccoladesToGame();
        
        // 3. Parser rewinds and sends events
        // Store GAME_OVER event
        GameOverEvent gameOver = new GameOverEvent(Instant.now(), new HashMap<>(), "de_dust2", "competitive", 13, 10, 45);
        eventStore.store(gameOver);
        
        // Store rounds
        for (int i = 0; i < 3; i++) {
            eventStore.store(new RoundStartEvent(Instant.now().plusSeconds(i * 60), null));
            eventStore.store(createKillEvent(Instant.now().plusSeconds(i * 60 + 10)));
            eventStore.store(new RoundEndEvent(Instant.now().plusSeconds(i * 60 + 55), null));
        }
        
        // 4. GAME_PROCESSED triggers persistence
        eventStore.onGameEnded(createGameProcessedEvent());
        
        // Verify both repositories were called
        verify(gameEventRepository).saveAll(anyList());
        verify(accoladeRepository).saveAll(anyList());
        
        // Verify counts
        ArgumentCaptor<List<GameEventEntity>> eventCaptor = ArgumentCaptor.forClass(List.class);
        verify(gameEventRepository).saveAll(eventCaptor.capture());
        
        List<GameEventEntity> savedEvents = eventCaptor.getValue();
        // Expected: 1 GAME_OVER + 3 rounds * (1 ROUND_START + 1 KILL + 1 ROUND_END) = 1 + 9 = 10
        assertEquals(10, savedEvents.size(), "Should save 10 events");
        
        // Verify all events have game reference
        for (GameEventEntity event : savedEvents) {
            assertNotNull(event.getGame(), 
                    event.getClass().getSimpleName() + " should have game reference");
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private GameEntity createTestGame() {
        GameEntity game = new GameEntity();
        game.setGameOverTimestamp(Instant.now());
        game.setMap("de_dust2");
        game.setMode("competitive");
        game.setTeam1Score(13);
        game.setTeam2Score(10);
        game.setDuration(45);
        game.setEndTime(Instant.now());
        game.setStartTime(Instant.now().minusSeconds(2700));
        return game;
    }

    private AccoladeEntity createAccolade(String type, String playerName) {
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setType(type);
        accolade.setPlayerName(playerName);
        accolade.setValue(100.0);
        accolade.setPosition(1);
        accolade.setScore(50.0);
        return accolade;
    }

    private KillEvent createKillEvent(Instant timestamp) {
        Player killer = new Player("Killer", "[U:1:123]");
        Player victim = new Player("Victim", "[U:1:456]");
        return new KillEvent(timestamp, new HashMap<>(), killer, victim, "AK-47", true);
    }

    private GameProcessedEvent createGameProcessedEvent() {
        return new GameProcessedEvent(Instant.now(), new HashMap<>());
    }
}
