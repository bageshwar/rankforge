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

import com.rankforge.pipeline.persistence.entity.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventProcessingContext.
 * Validates the requirements from the simplified event linking plan:
 * - Game context is set first (when GAME_OVER is processed)
 * - RoundStart events get game reference and become current round
 * - In-round events (KILL, ASSIST, ATTACK) get both game and roundStart references
 * - RoundEnd events get game and roundStart references, then clear currentRound
 * - All pending entities can be retrieved for batch save
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class EventProcessingContextTest {

    private EventProcessingContext context;
    private GameEntity testGame;

    @BeforeEach
    void setUp() {
        context = new EventProcessingContext();
        testGame = createTestGame();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("Should have null currentGame initially")
        void shouldHaveNullCurrentGameInitially() {
            assertNull(context.getCurrentGame());
        }

        @Test
        @DisplayName("Should have null currentRoundStart initially")
        void shouldHaveNullCurrentRoundStartInitially() {
            assertNull(context.getCurrentRoundStart());
        }

        @Test
        @DisplayName("Should have empty pending entities initially")
        void shouldHaveEmptyPendingEntitiesInitially() {
            assertTrue(context.getPendingEntities().isEmpty());
        }
    }

    @Nested
    @DisplayName("Game Context Tests (setCurrentGame)")
    class GameContextTests {

        @Test
        @DisplayName("Should store GameEntity when setCurrentGame is called")
        void shouldStoreGameEntityWhenSetCurrentGameCalled() {
            // When
            context.setCurrentGame(testGame);

            // Then
            assertSame(testGame, context.getCurrentGame());
        }

        @Test
        @DisplayName("Should allow overwriting game context with new game")
        void shouldAllowOverwritingGameContext() {
            // Given
            GameEntity secondGame = createTestGame();
            secondGame.setMap("de_mirage");
            
            // When
            context.setCurrentGame(testGame);
            context.setCurrentGame(secondGame);

            // Then
            assertSame(secondGame, context.getCurrentGame());
            assertEquals("de_mirage", context.getCurrentGame().getMap());
        }
    }

    @Nested
    @DisplayName("Round Start Tests (onRoundStart)")
    class RoundStartTests {

        @Test
        @DisplayName("Should set game reference on RoundStartEventEntity")
        void shouldSetGameReferenceOnRoundStart() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());

            // When
            context.onRoundStart(roundStart);

            // Then
            assertSame(testGame, roundStart.getGame());
        }

        @Test
        @DisplayName("Should store RoundStartEventEntity as currentRoundStart")
        void shouldStoreRoundStartAsCurrentRound() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());

            // When
            context.onRoundStart(roundStart);

            // Then
            assertSame(roundStart, context.getCurrentRoundStart());
        }

        @Test
        @DisplayName("Should add RoundStartEventEntity to pending entities")
        void shouldAddRoundStartToPendingEntities() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());

            // When
            context.onRoundStart(roundStart);

            // Then
            assertTrue(context.getPendingEntities().contains(roundStart));
            assertEquals(1, context.getPendingEntities().size());
        }

        @Test
        @DisplayName("Should handle multiple rounds by updating currentRoundStart")
        void shouldHandleMultipleRounds() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity round1 = new RoundStartEventEntity(Instant.now());
            RoundStartEventEntity round2 = new RoundStartEventEntity(Instant.now().plusSeconds(60));

            // When - simulate processing round 1 then round 2
            context.onRoundStart(round1);
            context.onRoundEnd(new RoundEndEventEntity(Instant.now().plusSeconds(30)));
            context.onRoundStart(round2);

            // Then
            assertSame(round2, context.getCurrentRoundStart());
            assertSame(testGame, round2.getGame());
        }
    }

    @Nested
    @DisplayName("In-Round Event Tests (addEvent)")
    class InRoundEventTests {

        @Test
        @DisplayName("Should set game reference on in-round events (KILL)")
        void shouldSetGameReferenceOnKillEvent() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity killEvent = new KillEventEntity(Instant.now());

            // When
            context.addEvent(killEvent);

            // Then
            assertSame(testGame, killEvent.getGame());
        }

        @Test
        @DisplayName("Should set roundStart reference on in-round events (KILL)")
        void shouldSetRoundStartReferenceOnKillEvent() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity killEvent = new KillEventEntity(Instant.now());

            // When
            context.addEvent(killEvent);

            // Then
            assertSame(roundStart, killEvent.getRoundStart());
        }

        @Test
        @DisplayName("Should add in-round event to pending entities")
        void shouldAddInRoundEventToPendingEntities() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity killEvent = new KillEventEntity(Instant.now());

            // When
            context.addEvent(killEvent);

            // Then
            assertTrue(context.getPendingEntities().contains(killEvent));
        }

        @Test
        @DisplayName("Should handle multiple events in the same round")
        void shouldHandleMultipleEventsInSameRound() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity kill1 = new KillEventEntity(Instant.now());
            KillEventEntity kill2 = new KillEventEntity(Instant.now().plusSeconds(1));
            AssistEventEntity assist = new AssistEventEntity(Instant.now().plusSeconds(2));
            AttackEventEntity attack = new AttackEventEntity(Instant.now().plusSeconds(3));

            // When
            context.addEvent(kill1);
            context.addEvent(kill2);
            context.addEvent(assist);
            context.addEvent(attack);

            // Then - all events should have same game and roundStart
            assertAll("All events should reference same game and round",
                () -> assertSame(testGame, kill1.getGame()),
                () -> assertSame(testGame, kill2.getGame()),
                () -> assertSame(testGame, assist.getGame()),
                () -> assertSame(testGame, attack.getGame()),
                () -> assertSame(roundStart, kill1.getRoundStart()),
                () -> assertSame(roundStart, kill2.getRoundStart()),
                () -> assertSame(roundStart, assist.getRoundStart()),
                () -> assertSame(roundStart, attack.getRoundStart())
            );

            // Pending entities: roundStart + 4 events = 5
            assertEquals(5, context.getPendingEntities().size());
        }

        @Test
        @DisplayName("Should handle assist events correctly")
        void shouldHandleAssistEventsCorrectly() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            assist.setPlayer1("Assister");
            assist.setPlayer2("Victim");
            assist.setAssistType("Regular");

            // When
            context.addEvent(assist);

            // Then
            assertSame(testGame, assist.getGame());
            assertSame(roundStart, assist.getRoundStart());
            assertTrue(context.getPendingEntities().contains(assist));
        }

        @Test
        @DisplayName("Should handle attack events correctly")
        void shouldHandleAttackEventsCorrectly() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            AttackEventEntity attack = new AttackEventEntity(Instant.now());
            attack.setPlayer1("Attacker");
            attack.setPlayer2("Victim");
            attack.setDamage(25);
            attack.setWeapon("ak47");

            // When
            context.addEvent(attack);

            // Then
            assertSame(testGame, attack.getGame());
            assertSame(roundStart, attack.getRoundStart());
            assertTrue(context.getPendingEntities().contains(attack));
        }
    }

    @Nested
    @DisplayName("Round End Tests (onRoundEnd)")
    class RoundEndTests {

        @Test
        @DisplayName("Should set game reference on RoundEndEventEntity")
        void shouldSetGameReferenceOnRoundEnd() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());

            // When
            context.onRoundEnd(roundEnd);

            // Then
            assertSame(testGame, roundEnd.getGame());
        }

        @Test
        @DisplayName("Should set roundStart reference on RoundEndEventEntity")
        void shouldSetRoundStartReferenceOnRoundEnd() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());

            // When
            context.onRoundEnd(roundEnd);

            // Then
            assertSame(roundStart, roundEnd.getRoundStart());
        }

        @Test
        @DisplayName("Should add RoundEndEventEntity to pending entities")
        void shouldAddRoundEndToPendingEntities() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());

            // When
            context.onRoundEnd(roundEnd);

            // Then
            assertTrue(context.getPendingEntities().contains(roundEnd));
        }

        @Test
        @DisplayName("Should clear currentRoundStart after round end")
        void shouldClearCurrentRoundStartAfterRoundEnd() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            assertNotNull(context.getCurrentRoundStart());
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());

            // When
            context.onRoundEnd(roundEnd);

            // Then
            assertNull(context.getCurrentRoundStart());
        }

        @Test
        @DisplayName("Should NOT clear currentGame after round end")
        void shouldNotClearCurrentGameAfterRoundEnd() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());

            // When
            context.onRoundEnd(roundEnd);

            // Then
            assertSame(testGame, context.getCurrentGame()); // Game should persist
        }
    }

    @Nested
    @DisplayName("Clear Context Tests")
    class ClearContextTests {

        @Test
        @DisplayName("Should clear currentGame on clear()")
        void shouldClearCurrentGameOnClear() {
            // Given
            context.setCurrentGame(testGame);

            // When
            context.clear();

            // Then
            assertNull(context.getCurrentGame());
        }

        @Test
        @DisplayName("Should clear currentRoundStart on clear()")
        void shouldClearCurrentRoundStartOnClear() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);

            // When
            context.clear();

            // Then
            assertNull(context.getCurrentRoundStart());
        }

        @Test
        @DisplayName("Should clear pending entities on clear()")
        void shouldClearPendingEntitiesOnClear() {
            // Given
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            context.addEvent(new KillEventEntity(Instant.now()));
            context.addEvent(new AssistEventEntity(Instant.now()));
            assertEquals(3, context.getPendingEntities().size());

            // When
            context.clear();

            // Then
            assertTrue(context.getPendingEntities().isEmpty());
        }
    }

    @Nested
    @DisplayName("Full Game Flow Tests")
    class FullGameFlowTests {

        @Test
        @DisplayName("Should correctly process complete game with multiple rounds")
        void shouldProcessCompleteGameWithMultipleRounds() {
            // Given - simulate parser output order: GAME_OVER first, then rounds
            context.setCurrentGame(testGame);

            // Round 1
            RoundStartEventEntity round1Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:00:00Z"));
            context.onRoundStart(round1Start);
            
            KillEventEntity kill1 = new KillEventEntity(Instant.parse("2026-01-01T10:00:10Z"));
            kill1.setPlayer1("Killer1");
            kill1.setPlayer2("Victim1");
            context.addEvent(kill1);
            
            KillEventEntity kill2 = new KillEventEntity(Instant.parse("2026-01-01T10:00:20Z"));
            kill2.setPlayer1("Killer2");
            kill2.setPlayer2("Victim2");
            context.addEvent(kill2);
            
            RoundEndEventEntity round1End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:00:30Z"));
            context.onRoundEnd(round1End);

            // Round 2
            RoundStartEventEntity round2Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:01:00Z"));
            context.onRoundStart(round2Start);
            
            KillEventEntity kill3 = new KillEventEntity(Instant.parse("2026-01-01T10:01:15Z"));
            kill3.setPlayer1("Killer3");
            kill3.setPlayer2("Victim3");
            context.addEvent(kill3);
            
            AssistEventEntity assist1 = new AssistEventEntity(Instant.parse("2026-01-01T10:01:16Z"));
            assist1.setPlayer1("Assister1");
            assist1.setPlayer2("Victim3");
            context.addEvent(assist1);
            
            RoundEndEventEntity round2End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:01:30Z"));
            context.onRoundEnd(round2End);

            // Then - verify all entities have correct references
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            
            // Should have: 2 round starts, 3 kills, 1 assist, 2 round ends = 8 entities
            assertEquals(8, pendingEntities.size());
            
            // All entities should reference the same game
            pendingEntities.forEach(entity -> 
                assertSame(testGame, entity.getGame(), 
                    "Entity " + entity.getClass().getSimpleName() + " should reference the game"));
            
            // Round 1 events should reference round1Start
            assertSame(round1Start, kill1.getRoundStart());
            assertSame(round1Start, kill2.getRoundStart());
            assertSame(round1Start, round1End.getRoundStart());
            
            // Round 2 events should reference round2Start
            assertSame(round2Start, kill3.getRoundStart());
            assertSame(round2Start, assist1.getRoundStart());
            assertSame(round2Start, round2End.getRoundStart());
        }

        @Test
        @DisplayName("Should handle game with no rounds (edge case)")
        void shouldHandleGameWithNoRounds() {
            // Given - just game over, no rounds
            context.setCurrentGame(testGame);

            // Then
            assertSame(testGame, context.getCurrentGame());
            assertNull(context.getCurrentRoundStart());
            assertTrue(context.getPendingEntities().isEmpty());
        }

        @Test
        @DisplayName("Should reset context for next game after clear")
        void shouldResetContextForNextGameAfterClear() {
            // Given - process first game
            context.setCurrentGame(testGame);
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            context.addEvent(new KillEventEntity(Instant.now()));
            context.onRoundEnd(new RoundEndEventEntity(Instant.now()));

            // When - clear and start new game
            context.clear();
            
            GameEntity newGame = createTestGame();
            newGame.setMap("de_mirage");
            context.setCurrentGame(newGame);
            
            RoundStartEventEntity newRound = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(newRound);
            
            KillEventEntity newKill = new KillEventEntity(Instant.now());
            context.addEvent(newKill);

            // Then
            assertSame(newGame, context.getCurrentGame());
            assertSame(newRound, context.getCurrentRoundStart());
            assertEquals(2, context.getPendingEntities().size()); // roundStart + kill
            
            // New entities should reference new game, not old game
            assertSame(newGame, newKill.getGame());
            assertSame(newRound, newKill.getRoundStart());
            assertNotSame(testGame, newKill.getGame());
        }
    }

    @Nested
    @DisplayName("GameOverEvent Tests (addGameOverEvent)")
    class GameOverEventTests {

        @Test
        @DisplayName("Should set game reference on GameOverEventEntity")
        void shouldSetGameReferenceOnGameOverEvent() {
            // Given
            context.setCurrentGame(testGame);
            GameOverEventEntity gameOverEvent = new GameOverEventEntity(Instant.now());
            gameOverEvent.setMap("de_dust2");
            gameOverEvent.setMode("competitive");
            gameOverEvent.setTeam1Score(16);
            gameOverEvent.setTeam2Score(14);

            // When
            context.addGameOverEvent(gameOverEvent);

            // Then
            assertSame(testGame, gameOverEvent.getGame());
        }

        @Test
        @DisplayName("Should add GameOverEventEntity to pending entities")
        void shouldAddGameOverEventToPendingEntities() {
            // Given
            context.setCurrentGame(testGame);
            GameOverEventEntity gameOverEvent = new GameOverEventEntity(Instant.now());

            // When
            context.addGameOverEvent(gameOverEvent);

            // Then
            assertTrue(context.getPendingEntities().contains(gameOverEvent));
            assertEquals(1, context.getPendingEntities().size());
        }

        @Test
        @DisplayName("Should handle GameOverEvent without game context")
        void shouldHandleGameOverEventWithoutGameContext() {
            // Given - no game set
            GameOverEventEntity gameOverEvent = new GameOverEventEntity(Instant.now());

            // When
            context.addGameOverEvent(gameOverEvent);

            // Then
            assertNull(gameOverEvent.getGame()); // No game context
            assertTrue(context.getPendingEntities().contains(gameOverEvent));
        }

        @Test
        @DisplayName("Should preserve GameOverEventEntity data after adding to context")
        void shouldPreserveGameOverEventDataAfterAdding() {
            // Given
            context.setCurrentGame(testGame);
            GameOverEventEntity gameOverEvent = new GameOverEventEntity(Instant.parse("2026-01-01T10:30:00Z"));
            gameOverEvent.setMap("de_mirage");
            gameOverEvent.setMode("wingman");
            gameOverEvent.setTeam1Score(9);
            gameOverEvent.setTeam2Score(7);
            gameOverEvent.setDuration(25);

            // When
            context.addGameOverEvent(gameOverEvent);

            // Then - all data should be preserved
            assertEquals("de_mirage", gameOverEvent.getMap());
            assertEquals("wingman", gameOverEvent.getMode());
            assertEquals(9, gameOverEvent.getTeam1Score());
            assertEquals(7, gameOverEvent.getTeam2Score());
            assertEquals(25, gameOverEvent.getDuration());
            assertSame(testGame, gameOverEvent.getGame());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle events before round start (outside round)")
        void shouldHandleEventsBeforeRoundStart() {
            // Given
            context.setCurrentGame(testGame);
            // No round start yet
            
            KillEventEntity kill = new KillEventEntity(Instant.now());

            // When
            context.addEvent(kill);

            // Then
            assertSame(testGame, kill.getGame());
            assertNull(kill.getRoundStart()); // No round context yet
            assertTrue(context.getPendingEntities().contains(kill));
        }

        @Test
        @DisplayName("Should handle events without game context")
        void shouldHandleEventsWithoutGameContext() {
            // Given - no game set
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());

            // When
            context.onRoundStart(roundStart);

            // Then
            assertNull(roundStart.getGame()); // No game context
            assertSame(roundStart, context.getCurrentRoundStart());
        }

        @Test
        @DisplayName("Should maintain pending entities order")
        void shouldMaintainPendingEntitiesOrder() {
            // Given
            context.setCurrentGame(testGame);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.parse("2026-01-01T10:00:00Z"));
            KillEventEntity kill = new KillEventEntity(Instant.parse("2026-01-01T10:00:10Z"));
            AssistEventEntity assist = new AssistEventEntity(Instant.parse("2026-01-01T10:00:11Z"));
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.parse("2026-01-01T10:00:30Z"));

            // When
            context.onRoundStart(roundStart);
            context.addEvent(kill);
            context.addEvent(assist);
            context.onRoundEnd(roundEnd);

            // Then - order should be preserved
            List<GameEventEntity> pending = context.getPendingEntities();
            assertEquals(4, pending.size());
            assertEquals(roundStart, pending.get(0));
            assertEquals(kill, pending.get(1));
            assertEquals(assist, pending.get(2));
            assertEquals(roundEnd, pending.get(3));
        }
    }

    /**
     * Helper to create a test GameEntity
     */
    private GameEntity createTestGame() {
        GameEntity game = new GameEntity();
        game.setMap("de_dust2");
        game.setMode("competitive");
        game.setTeam1Score(16);
        game.setTeam2Score(14);
        game.setGameOverTimestamp(Instant.now());
        game.setEndTime(Instant.now());
        return game;
    }
}
