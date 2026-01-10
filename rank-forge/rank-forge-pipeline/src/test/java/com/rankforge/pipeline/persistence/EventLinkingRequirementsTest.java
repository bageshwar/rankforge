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

import com.rankforge.core.events.*;
import com.rankforge.pipeline.persistence.entity.*;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for event linking requirements.
 * These tests verify that:
 * 1. Game table has game entries after processing
 * 2. All GameEvents have gameId set
 * 3. All rounds (ROUND_START events) are linked properly
 * 4. Accolades are persisted and linked to games
 *
 * Author bageshwar.pn
 * Date 2026
 */
class EventLinkingRequirementsTest {

    private EventProcessingContext context;
    private GameEntity testGame;
    private static final int EXPECTED_ROUNDS = 40;

    @BeforeEach
    void setUp() {
        context = new EventProcessingContext();
        
        // Create a test game entity
        testGame = new GameEntity();
        testGame.setGameOverTimestamp(Instant.now());
        testGame.setMap("de_dust2");
        testGame.setMode("competitive");
        testGame.setTeam1Score(13);
        testGame.setTeam2Score(10);
        testGame.setDuration(45);
        testGame.setEndTime(Instant.now());
        testGame.setStartTime(Instant.now().minusSeconds(2700));
    }

    // ========================================================================
    // REQUIREMENT 1: Game table should have game entries after processing
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 1: GameEntity should be available in context after setCurrentGame")
    void gameEntityShouldBeAvailableInContext() {
        // Given: A game entity
        // When: We set it in context
        context.setCurrentGame(testGame);
        
        // Then: It should be retrievable
        assertNotNull(context.getCurrentGame(), "Game should be set in context");
        assertEquals("de_dust2", context.getCurrentGame().getMap());
    }

    @Test
    @DisplayName("REQUIREMENT 1: GameEntity should be in pending entities for persistence")
    void gameEntityShouldBeInPendingEntitiesForPersistence() {
        // Given: A game entity set in context
        context.setCurrentGame(testGame);
        
        // When: We process a GAME_OVER event
        GameOverEventEntity gameOverEvent = new GameOverEventEntity(Instant.now());
        context.addGameOverEvent(gameOverEvent);
        
        // Then: The game entity should be accessible for persistence
        // This tests that we can get the game to save it
        GameEntity game = context.getCurrentGame();
        assertNotNull(game, "Game entity must be available for persistence");
        
        // The GameOverEvent should have the game reference set
        assertEquals(testGame, gameOverEvent.getGame(), 
                "GameOverEvent should have game reference set");
    }

    // ========================================================================
    // REQUIREMENT 2: All GameEvents should have gameId set
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 2: All events added via addEvent should have game reference set")
    void allEventsShouldHaveGameReference() {
        // Given: A context with game set
        context.setCurrentGame(testGame);
        
        // And: A round started
        RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
        context.onRoundStart(roundStart);
        
        // When: We add various events
        KillEventEntity killEvent = new KillEventEntity(Instant.now());
        context.addEvent(killEvent);
        
        AssistEventEntity assistEvent = new AssistEventEntity(Instant.now());
        context.addEvent(assistEvent);
        
        AttackEventEntity attackEvent = new AttackEventEntity(Instant.now());
        context.addEvent(attackEvent);
        
        BombEventEntity bombEvent = new BombEventEntity(Instant.now());
        context.addEvent(bombEvent);
        
        // Then: All events in pending list should have game reference
        List<GameEventEntity> pending = context.getPendingEntities();
        assertFalse(pending.isEmpty(), "Pending entities should not be empty");
        
        for (GameEventEntity entity : pending) {
            assertNotNull(entity.getGame(), 
                    "Event " + entity.getClass().getSimpleName() + " should have game reference");
            assertSame(testGame, entity.getGame(), 
                    "Event should reference the same game entity");
        }
    }

    @Test
    @DisplayName("REQUIREMENT 2: RoundStartEvent should have game reference set")
    void roundStartEventShouldHaveGameReference() {
        // Given: A context with game set
        context.setCurrentGame(testGame);
        
        // When: We start a round
        RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
        context.onRoundStart(roundStart);
        
        // Then: The round start event should have game reference
        assertNotNull(roundStart.getGame(), "RoundStartEvent should have game reference");
        assertSame(testGame, roundStart.getGame());
    }

    @Test
    @DisplayName("REQUIREMENT 2: RoundEndEvent should have game reference set")
    void roundEndEventShouldHaveGameReference() {
        // Given: A context with game and round
        context.setCurrentGame(testGame);
        RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
        context.onRoundStart(roundStart);
        
        // When: We end the round
        RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
        context.onRoundEnd(roundEnd);
        
        // Then: The round end event should have game reference
        assertNotNull(roundEnd.getGame(), "RoundEndEvent should have game reference");
        assertSame(testGame, roundEnd.getGame());
    }

    // ========================================================================
    // REQUIREMENT 3: All rounds should have distinct roundStartEventId
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 3: All in-round events should have roundStart reference set")
    void allInRoundEventsShouldHaveRoundStartReference() {
        // Given: A context with game set
        context.setCurrentGame(testGame);
        
        // When: We process multiple rounds
        for (int round = 1; round <= 3; round++) {
            RoundStartEventEntity roundStart = new RoundStartEventEntity(
                    Instant.now().plusSeconds(round * 100));
            context.onRoundStart(roundStart);
            
            // Add some events in this round
            KillEventEntity kill = new KillEventEntity(Instant.now().plusSeconds(round * 100 + 10));
            context.addEvent(kill);
            
            // Verify this kill has the correct round reference
            assertSame(roundStart, kill.getRoundStart(), 
                    "Kill in round " + round + " should reference that round's start");
            
            // End round
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(
                    Instant.now().plusSeconds(round * 100 + 50));
            context.onRoundEnd(roundEnd);
            
            // Verify round end has the correct round reference
            assertSame(roundStart, roundEnd.getRoundStart(),
                    "RoundEnd should reference the same round's start");
        }
    }

    @Test
    @DisplayName("REQUIREMENT 3: After round end, currentRoundStart should be null")
    void afterRoundEndCurrentRoundStartShouldBeNull() {
        // Given: A context with game and round
        context.setCurrentGame(testGame);
        RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
        context.onRoundStart(roundStart);
        
        // Verify round is active
        assertNotNull(context.getCurrentRoundStart());
        
        // When: Round ends
        RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
        context.onRoundEnd(roundEnd);
        
        // Then: Current round should be null
        assertNull(context.getCurrentRoundStart(), 
                "Current round should be null after round ends");
    }

    @Test
    @DisplayName("REQUIREMENT 3: Simulating 40 rounds should produce 40 distinct round starts")
    void simulatingFortyRoundsShouldProduceFortyDistinctRoundStarts() {
        // Given: A context with game set
        context.setCurrentGame(testGame);
        
        // When: We process 40 rounds
        for (int round = 1; round <= EXPECTED_ROUNDS; round++) {
            Instant roundTime = Instant.now().plusSeconds(round * 60);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(roundTime);
            context.onRoundStart(roundStart);
            
            // Add some events
            KillEventEntity kill = new KillEventEntity(roundTime.plusSeconds(10));
            context.addEvent(kill);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(roundTime.plusSeconds(55));
            context.onRoundEnd(roundEnd);
        }
        
        // Then: We should have exactly 40 distinct round start entities in pending
        List<GameEventEntity> pending = context.getPendingEntities();
        
        Set<RoundStartEventEntity> uniqueRoundStarts = pending.stream()
                .filter(e -> e instanceof RoundStartEventEntity)
                .map(e -> (RoundStartEventEntity) e)
                .collect(Collectors.toSet());
        
        assertEquals(EXPECTED_ROUNDS, uniqueRoundStarts.size(),
                "Should have " + EXPECTED_ROUNDS + " distinct round starts");
        
        // Also verify that all events (except round starts) have round references
        long eventsWithRoundRef = pending.stream()
                .filter(e -> !(e instanceof RoundStartEventEntity))
                .filter(e -> e.getRoundStart() != null)
                .count();
        
        long eventsNeedingRoundRef = pending.stream()
                .filter(e -> !(e instanceof RoundStartEventEntity))
                .count();
        
        assertEquals(eventsNeedingRoundRef, eventsWithRoundRef,
                "All non-RoundStart events should have round reference");
    }

    // ========================================================================
    // REQUIREMENT 4: Accolades should be persisted and linked to games
    // ========================================================================

    @Test
    @DisplayName("REQUIREMENT 4: Accolades should be added to pending list")
    void accoladesShouldBeAddedToPendingList() {
        // Given: Some accolade entities
        AccoladeEntity accolade1 = new AccoladeEntity();
        accolade1.setType("MVP");
        accolade1.setPlayerName("Player1");
        accolade1.setValue(100.0);
        accolade1.setPosition(1);
        accolade1.setScore(50.0);
        
        AccoladeEntity accolade2 = new AccoladeEntity();
        accolade2.setType("TopKiller");
        accolade2.setPlayerName("Player2");
        accolade2.setValue(25.0);
        accolade2.setPosition(1);
        accolade2.setScore(40.0);
        
        // When: We add them to context (before game exists)
        context.addAccolade(accolade1);
        context.addAccolade(accolade2);
        
        // Then: They should be in pending accolades
        List<AccoladeEntity> pendingAccolades = context.getPendingAccolades();
        assertEquals(2, pendingAccolades.size(), "Should have 2 pending accolades");
    }

    @Test
    @DisplayName("REQUIREMENT 4: Accolades should be linked to game when linkAccoladesToGame is called")
    void accoladesShouldBeLinkedToGameWhenLinkIsCalled() {
        // Given: Accolades added before game exists
        AccoladeEntity accolade1 = new AccoladeEntity();
        accolade1.setType("MVP");
        accolade1.setPlayerName("Player1");
        accolade1.setValue(100.0);
        accolade1.setPosition(1);
        accolade1.setScore(50.0);
        
        context.addAccolade(accolade1);
        
        // When: Game is set and accolades are linked
        context.setCurrentGame(testGame);
        context.linkAccoladesToGame();
        
        // Then: Accolade should have game reference
        assertNotNull(accolade1.getGame(), "Accolade should have game reference after linking");
        assertSame(testGame, accolade1.getGame(), "Accolade should reference the test game");
    }

    @Test
    @DisplayName("REQUIREMENT 4: Multiple accolades should all be linked to same game")
    void multipleAccoladesShouldAllBeLinkedToSameGame() {
        // Given: Multiple accolades added before game
        for (int i = 0; i < 10; i++) {
            AccoladeEntity accolade = new AccoladeEntity();
            accolade.setType("Type" + i);
            accolade.setPlayerName("Player" + i);
            accolade.setValue((double) (i * 10));
            accolade.setPosition(i);
            accolade.setScore((double) (i * 5));
            context.addAccolade(accolade);
        }
        
        // When: Game is set and linked
        context.setCurrentGame(testGame);
        context.linkAccoladesToGame();
        
        // Then: All accolades should have the same game reference
        for (AccoladeEntity accolade : context.getPendingAccolades()) {
            assertNotNull(accolade.getGame(), 
                    "Accolade " + accolade.getType() + " should have game reference");
            assertSame(testGame, accolade.getGame(),
                    "All accolades should reference the same game");
        }
    }

    // ========================================================================
    // INTEGRATION: Full game processing flow
    // ========================================================================

    @Test
    @DisplayName("INTEGRATION: Complete game flow - game, rounds, events, and accolades")
    void completeGameFlowShouldLinkAllEntitiesCorrectly() {
        // 1. Accolades are parsed first (before GAME_OVER in parser)
        for (int i = 0; i < 10; i++) {
            AccoladeEntity accolade = new AccoladeEntity();
            accolade.setType("Type" + i);
            accolade.setPlayerName("Player" + i);
            accolade.setValue((double) (i * 10));
            accolade.setPosition(i);
            accolade.setScore((double) (i * 5));
            context.addAccolade(accolade);
        }
        
        // 2. GAME_OVER is processed - creates and sets game
        context.setCurrentGame(testGame);
        context.linkAccoladesToGame();
        
        GameOverEventEntity gameOver = new GameOverEventEntity(Instant.now());
        context.addGameOverEvent(gameOver);
        
        // 3. Rounds are processed after rewind
        for (int round = 1; round <= EXPECTED_ROUNDS; round++) {
            Instant roundTime = Instant.now().plusSeconds(round * 60);
            
            // Round Start
            RoundStartEventEntity roundStart = new RoundStartEventEntity(roundTime);
            context.onRoundStart(roundStart);
            
            // Events within round
            KillEventEntity kill = new KillEventEntity(roundTime.plusSeconds(10));
            context.addEvent(kill);
            
            AttackEventEntity attack = new AttackEventEntity(roundTime.plusSeconds(5));
            context.addEvent(attack);
            
            // Round End
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(roundTime.plusSeconds(55));
            context.onRoundEnd(roundEnd);
        }
        
        // VERIFY: All requirements are met
        List<GameEventEntity> pending = context.getPendingEntities();
        List<AccoladeEntity> accolades = context.getPendingAccolades();
        
        // Requirement 1: Game entity is available
        assertNotNull(context.getCurrentGame());
        
        // Requirement 2: All events have gameId
        for (GameEventEntity event : pending) {
            assertNotNull(event.getGame(), 
                    event.getClass().getSimpleName() + " should have game reference");
        }
        
        // Requirement 3: Count distinct round starts
        Set<RoundStartEventEntity> roundStarts = pending.stream()
                .filter(e -> e instanceof RoundStartEventEntity)
                .map(e -> (RoundStartEventEntity) e)
                .collect(Collectors.toSet());
        assertEquals(EXPECTED_ROUNDS, roundStarts.size(), 
                "Should have " + EXPECTED_ROUNDS + " distinct round starts");
        
        // Requirement 4: All accolades have game reference
        assertEquals(10, accolades.size(), "Should have 10 accolades");
        for (AccoladeEntity accolade : accolades) {
            assertNotNull(accolade.getGame(), "Accolade should have game reference");
        }
    }

    @Test
    @DisplayName("REQUIREMENT: clear() should reset all context state")
    void clearShouldResetAllContextState() {
        // Given: A fully populated context
        context.setCurrentGame(testGame);
        
        RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
        context.onRoundStart(roundStart);
        
        KillEventEntity kill = new KillEventEntity(Instant.now());
        context.addEvent(kill);
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setType("MVP");
        accolade.setPlayerName("Test");
        accolade.setValue(100.0);
        accolade.setPosition(1);
        accolade.setScore(50.0);
        context.addAccolade(accolade);
        
        // When: We clear the context
        context.clear();
        
        // Then: Everything should be reset
        assertNull(context.getCurrentGame(), "Game should be null after clear");
        assertNull(context.getCurrentRoundStart(), "Current round should be null after clear");
        assertTrue(context.getPendingEntities().isEmpty(), "Pending entities should be empty after clear");
        assertTrue(context.getPendingAccolades().isEmpty(), "Pending accolades should be empty after clear");
    }
}
