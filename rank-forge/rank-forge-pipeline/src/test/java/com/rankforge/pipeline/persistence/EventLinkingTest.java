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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the simplified event linking flow.
 * 
 * These tests validate the event linking behavior of EventProcessingContext:
 * 
 * Event Processing Order (from Parser):
 * 1. GAME_OVER received (FIRST!) → Create GameEntity, store in context
 * 2. ROUND_START received → Link to game, store as currentRoundStart
 * 3. KILL/ASSIST/ATTACK received → Link to game AND roundStart
 * 4. ROUND_END received → Link to game and roundStart, clear roundStart
 * 5. (More rounds: repeat 2-4)
 * 6. GAME_PROCESSED received → Batch save all pending entities
 * 
 * Note: These are unit tests that test EventProcessingContext in isolation,
 * without database or Spring context dependencies.
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class EventLinkingTest {

    private EventProcessingContext context;

    @BeforeEach
    void setUp() {
        context = new EventProcessingContext();
        // Set appServerId for all tests (required before processing rounds)
        context.setAppServerId(100L);
    }

    @Nested
    @DisplayName("Event Processing Order Validation")
    class EventProcessingOrderTests {

        @Test
        @DisplayName("Should process GAME_OVER first before any rounds (parser rewind behavior)")
        void shouldProcessGameOverFirst() {
            // Given - simulate parser output: GAME_OVER comes first due to rewind
            Instant gameOverTime = Instant.parse("2026-01-01T10:30:00Z");
            Instant roundStartTime = Instant.parse("2026-01-01T10:00:00Z"); // Earlier in real time
            
            // Create game entity (from GAME_OVER event)
            GameEntity game = createGameEntity(gameOverTime, "de_dust2", 16, 14);
            
            // When - process in parser output order (GAME_OVER first)
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(roundStartTime);
            context.onRoundStart(roundStart);
            
            // Then - round should reference the game
            assertNotNull(roundStart.getGame());
            assertSame(game, roundStart.getGame());
        }

        @Test
        @DisplayName("Should have game reference available for all subsequent events")
        void shouldHaveGameReferenceForAllEvents() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // When - process various events
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity kill = new KillEventEntity(Instant.now());
            context.addEvent(kill);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            context.addEvent(assist);
            
            AttackEventEntity attack = new AttackEventEntity(Instant.now());
            context.addEvent(attack);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
            context.onRoundEnd(roundEnd);
            
            // Then - all events should have game reference
            assertAll("All events should reference the game",
                () -> assertSame(game, roundStart.getGame()),
                () -> assertSame(game, kill.getGame()),
                () -> assertSame(game, assist.getGame()),
                () -> assertSame(game, attack.getGame()),
                () -> assertSame(game, roundEnd.getGame())
            );
        }
    }

    @Nested
    @DisplayName("Round Linking Validation")
    class RoundLinkingTests {

        @Test
        @DisplayName("Should link all in-round events to the correct RoundStartEventEntity")
        void shouldLinkInRoundEventsToCorrectRound() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // When - process round with events
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity kill1 = new KillEventEntity(Instant.now());
            kill1.setPlayer1("Player1");
            kill1.setPlayer2("Player2");
            context.addEvent(kill1);
            
            KillEventEntity kill2 = new KillEventEntity(Instant.now().plusMillis(100));
            kill2.setPlayer1("Player3");
            kill2.setPlayer2("Player4");
            context.addEvent(kill2);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now().plusMillis(50));
            assist.setPlayer1("Player5");
            assist.setPlayer2("Player2");
            context.addEvent(assist);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(1));
            context.onRoundEnd(roundEnd);
            
            // Then - all events should reference the same roundStart
            assertAll("All in-round events should reference roundStart",
                () -> assertSame(roundStart, kill1.getRoundStart()),
                () -> assertSame(roundStart, kill2.getRoundStart()),
                () -> assertSame(roundStart, assist.getRoundStart()),
                () -> assertSame(roundStart, roundEnd.getRoundStart())
            );
        }

        @Test
        @DisplayName("Should correctly separate events across multiple rounds")
        void shouldSeparateEventsAcrossMultipleRounds() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // Round 1
            RoundStartEventEntity round1Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:00:00Z"));
            context.onRoundStart(round1Start);
            
            KillEventEntity round1Kill = new KillEventEntity(Instant.parse("2026-01-01T10:00:10Z"));
            round1Kill.setPlayer1("R1Killer");
            round1Kill.setPlayer2("R1Victim");
            context.addEvent(round1Kill);
            
            RoundEndEventEntity round1End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:00:30Z"));
            context.onRoundEnd(round1End);
            
            // Round 2
            RoundStartEventEntity round2Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:01:00Z"));
            context.onRoundStart(round2Start);
            
            KillEventEntity round2Kill = new KillEventEntity(Instant.parse("2026-01-01T10:01:10Z"));
            round2Kill.setPlayer1("R2Killer");
            round2Kill.setPlayer2("R2Victim");
            context.addEvent(round2Kill);
            
            RoundEndEventEntity round2End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:01:30Z"));
            context.onRoundEnd(round2End);
            
            // Then - verify correct round assignments
            assertAll("Events should reference their respective rounds",
                () -> assertSame(round1Start, round1Kill.getRoundStart(), "Round 1 kill should reference round 1"),
                () -> assertSame(round1Start, round1End.getRoundStart(), "Round 1 end should reference round 1 start"),
                () -> assertSame(round2Start, round2Kill.getRoundStart(), "Round 2 kill should reference round 2"),
                () -> assertSame(round2Start, round2End.getRoundStart(), "Round 2 end should reference round 2 start"),
                () -> assertNotSame(round1Kill.getRoundStart(), round2Kill.getRoundStart(), "Kills from different rounds should have different roundStart")
            );
        }

        @Test
        @DisplayName("RoundEndEventEntity should reference its RoundStartEventEntity")
        void roundEndShouldReferenceRoundStart() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(30));
            
            // When
            context.onRoundEnd(roundEnd);
            
            // Then
            assertSame(roundStart, roundEnd.getRoundStart());
        }

        @Test
        @DisplayName("Should clear round context after ROUND_END but keep game context")
        void shouldClearRoundContextAfterRoundEnd() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            assertNotNull(context.getCurrentRoundStart());
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(30));
            
            // When
            context.onRoundEnd(roundEnd);
            
            // Then
            assertNull(context.getCurrentRoundStart(), "Round context should be cleared after ROUND_END");
            assertSame(game, context.getCurrentGame(), "Game context should persist after ROUND_END");
        }
    }

    @Nested
    @DisplayName("Batch Persistence Preparation")
    class BatchPersistenceTests {

        @Test
        @DisplayName("Should collect all entities in pendingEntities for batch save")
        void shouldCollectAllEntitiesForBatchSave() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // Process a complete round
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity kill = new KillEventEntity(Instant.now());
            context.addEvent(kill);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            context.addEvent(assist);
            
            AttackEventEntity attack = new AttackEventEntity(Instant.now());
            context.addEvent(attack);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(30));
            context.onRoundEnd(roundEnd);
            
            // Then - all entities should be in pending list
            List<GameEventEntity> pending = context.getPendingEntities();
            
            assertEquals(5, pending.size());
            assertTrue(pending.contains(roundStart));
            assertTrue(pending.contains(kill));
            assertTrue(pending.contains(assist));
            assertTrue(pending.contains(attack));
            assertTrue(pending.contains(roundEnd));
        }

        @Test
        @DisplayName("Should have correct entity order for batch save (insertion order)")
        void shouldHaveCorrectEntityOrderForBatchSave() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // Process events in specific order
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            AttackEventEntity attack1 = new AttackEventEntity(Instant.now());
            context.addEvent(attack1);
            
            KillEventEntity kill = new KillEventEntity(Instant.now());
            context.addEvent(kill);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            context.addEvent(assist);
            
            AttackEventEntity attack2 = new AttackEventEntity(Instant.now());
            context.addEvent(attack2);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
            context.onRoundEnd(roundEnd);
            
            // Then - order should be preserved
            List<GameEventEntity> pending = context.getPendingEntities();
            
            assertEquals(6, pending.size());
            assertSame(roundStart, pending.get(0), "RoundStart should be first");
            assertSame(attack1, pending.get(1));
            assertSame(kill, pending.get(2));
            assertSame(assist, pending.get(3));
            assertSame(attack2, pending.get(4));
            assertSame(roundEnd, pending.get(5), "RoundEnd should be last");
        }

        @Test
        @DisplayName("Should clear all state after GAME_PROCESSED (simulated by clear())")
        void shouldClearAllStateAfterGameProcessed() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            context.addEvent(new KillEventEntity(Instant.now()));
            context.onRoundEnd(new RoundEndEventEntity(Instant.now()));
            
            assertNotNull(context.getCurrentGame());
            assertFalse(context.getPendingEntities().isEmpty());
            
            // When - simulate GAME_PROCESSED
            context.clear();
            
            // Then
            assertNull(context.getCurrentGame());
            assertNull(context.getCurrentRoundStart());
            assertTrue(context.getPendingEntities().isEmpty());
        }
    }

    @Nested
    @DisplayName("Complete Game Flow Integration")
    class CompleteGameFlowTests {

        @Test
        @DisplayName("Should correctly process a full competitive game simulation")
        void shouldProcessFullCompetitiveGame() {
            // Given - simulate a competitive game with 3 rounds
            Instant gameEndTime = Instant.parse("2026-01-01T11:00:00Z");
            GameEntity game = createGameEntity(gameEndTime, "de_dust2", 2, 1);
            
            // When - process in parser order (GAME_OVER first, then rounds)
            context.setCurrentGame(game);
            
            // Round 1: T wins
            processRound(context, 
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:02:00Z"),
                List.of(
                    createKillEvent("T1", "CT1", Instant.parse("2026-01-01T10:00:30Z")),
                    createKillEvent("T2", "CT2", Instant.parse("2026-01-01T10:00:45Z")),
                    createAssistEvent("T3", "CT2", Instant.parse("2026-01-01T10:00:46Z")),
                    createKillEvent("T1", "CT3", Instant.parse("2026-01-01T10:01:00Z")),
                    createKillEvent("T2", "CT4", Instant.parse("2026-01-01T10:01:15Z")),
                    createKillEvent("T3", "CT5", Instant.parse("2026-01-01T10:01:30Z"))
                )
            );
            
            // Round 2: CT wins
            processRound(context,
                Instant.parse("2026-01-01T10:03:00Z"),
                Instant.parse("2026-01-01T10:05:00Z"),
                List.of(
                    createKillEvent("CT1", "T1", Instant.parse("2026-01-01T10:03:30Z")),
                    createKillEvent("CT2", "T2", Instant.parse("2026-01-01T10:03:45Z")),
                    createKillEvent("CT3", "T3", Instant.parse("2026-01-01T10:04:00Z")),
                    createKillEvent("CT4", "T4", Instant.parse("2026-01-01T10:04:15Z")),
                    createKillEvent("CT5", "T5", Instant.parse("2026-01-01T10:04:30Z"))
                )
            );
            
            // Round 3: T wins (final)
            processRound(context,
                Instant.parse("2026-01-01T10:06:00Z"),
                Instant.parse("2026-01-01T10:08:00Z"),
                List.of(
                    createKillEvent("T1", "CT1", Instant.parse("2026-01-01T10:06:30Z")),
                    createKillEvent("T2", "CT2", Instant.parse("2026-01-01T10:06:45Z")),
                    createAssistEvent("T3", "CT2", Instant.parse("2026-01-01T10:06:46Z")),
                    createKillEvent("T1", "CT3", Instant.parse("2026-01-01T10:07:00Z")),
                    createAttackEvent("T2", "CT4", 45, Instant.parse("2026-01-01T10:07:10Z")),
                    createKillEvent("T3", "CT4", Instant.parse("2026-01-01T10:07:15Z")),
                    createKillEvent("T1", "CT5", Instant.parse("2026-01-01T10:07:30Z"))
                )
            );
            
            // Then - verify all entities are correctly linked
            List<GameEventEntity> allEntities = context.getPendingEntities();
            
            // Should have: 3 rounds × 2 (start+end) + events = 6 + 6 + 5 + 7 = 24 entities
            // Actually: Round1: 1 start + 6 events + 1 end = 8
            //          Round2: 1 start + 5 events + 1 end = 7  
            //          Round3: 1 start + 7 events + 1 end = 9
            //          Total = 24
            assertEquals(24, allEntities.size());
            
            // All entities should reference the same game
            for (GameEventEntity entity : allEntities) {
                assertSame(game, entity.getGame(), 
                    "Entity " + entity.getClass().getSimpleName() + " at " + entity.getTimestamp() 
                    + " should reference the game");
            }
            
            // Verify round groupings by checking roundStart references
            Map<RoundStartEventEntity, List<GameEventEntity>> eventsByRound = new HashMap<>();
            for (GameEventEntity entity : allEntities) {
                if (entity.getRoundStart() != null) {
                    eventsByRound.computeIfAbsent(entity.getRoundStart(), k -> new ArrayList<>()).add(entity);
                }
            }
            
            // Should have 3 distinct rounds
            assertEquals(3, eventsByRound.size(), "Should have 3 distinct rounds");
        }

        @Test
        @DisplayName("Should handle multiple games in sequence")
        void shouldHandleMultipleGamesInSequence() {
            // Game 1
            GameEntity game1 = createGameEntity(Instant.parse("2026-01-01T10:00:00Z"), "de_dust2", 16, 14);
            context.setCurrentGame(game1);
            
            RoundStartEventEntity g1Round = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(g1Round);
            KillEventEntity g1Kill = new KillEventEntity(Instant.now());
            context.addEvent(g1Kill);
            context.onRoundEnd(new RoundEndEventEntity(Instant.now()));
            
            // Verify game 1 links
            assertSame(game1, g1Kill.getGame());
            assertSame(g1Round, g1Kill.getRoundStart());
            
            // Simulate GAME_PROCESSED - batch save and clear
            // In real code: gameEventRepository.saveAll(context.getPendingEntities())
            context.clear();
            
            // Game 2
            GameEntity game2 = createGameEntity(Instant.parse("2026-01-01T11:00:00Z"), "de_mirage", 13, 16);
            context.setCurrentGame(game2);
            
            RoundStartEventEntity g2Round = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(g2Round);
            KillEventEntity g2Kill = new KillEventEntity(Instant.now());
            context.addEvent(g2Kill);
            context.onRoundEnd(new RoundEndEventEntity(Instant.now()));
            
            // Verify game 2 links
            assertSame(game2, g2Kill.getGame());
            assertSame(g2Round, g2Kill.getRoundStart());
            
            // Verify games are independent
            assertNotSame(game1, game2);
            assertNotSame(g1Kill.getGame(), g2Kill.getGame());
            assertNotSame(g1Kill.getRoundStart(), g2Kill.getRoundStart());
            
            // Verify game 1 entities still have correct references (immutable after set)
            assertSame(game1, g1Kill.getGame());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty rounds (no kills)")
        void shouldHandleEmptyRounds() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // When - empty round (timeout/bomb explode with no kills)
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(115));
            context.onRoundEnd(roundEnd);
            
            // Then
            List<GameEventEntity> pending = context.getPendingEntities();
            assertEquals(2, pending.size());
            assertSame(game, roundStart.getGame());
            assertSame(game, roundEnd.getGame());
            assertSame(roundStart, roundEnd.getRoundStart());
        }

        @Test
        @DisplayName("Should handle game with single event")
        void shouldHandleGameWithSingleEvent() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 1, 0);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            // Single kill to win
            KillEventEntity kill = new KillEventEntity(Instant.now());
            kill.setPlayer1("Ace");
            kill.setPlayer2("Victim");
            context.addEvent(kill);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(5));
            context.onRoundEnd(roundEnd);
            
            // Then
            assertEquals(3, context.getPendingEntities().size());
            assertSame(game, kill.getGame());
            assertSame(roundStart, kill.getRoundStart());
        }

        @Test
        @DisplayName("Should handle consecutive rounds without gap")
        void shouldHandleConsecutiveRoundsWithoutGap() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 2, 0);
            context.setCurrentGame(game);
            
            // Round 1 - ends exactly when round 2 starts
            RoundStartEventEntity round1Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:00:00Z"));
            context.onRoundStart(round1Start);
            RoundEndEventEntity round1End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:01:00Z"));
            context.onRoundEnd(round1End);
            
            // Round 2 - starts immediately
            RoundStartEventEntity round2Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:01:00Z"));
            context.onRoundStart(round2Start);
            RoundEndEventEntity round2End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:02:00Z"));
            context.onRoundEnd(round2End);
            
            // Then - rounds should still be distinct
            assertNotSame(round1Start, round2Start);
            assertSame(round1Start, round1End.getRoundStart());
            assertSame(round2Start, round2End.getRoundStart());
        }

        @Test
        @DisplayName("Should handle events with same timestamp but different rounds")
        void shouldHandleEventsWithSameTimestampDifferentRounds() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 2, 0);
            context.setCurrentGame(game);
            Instant sameTime = Instant.parse("2026-01-01T10:00:00Z");
            
            // Round 1
            RoundStartEventEntity round1Start = new RoundStartEventEntity(sameTime);
            context.onRoundStart(round1Start);
            KillEventEntity kill1 = new KillEventEntity(sameTime);
            context.addEvent(kill1);
            context.onRoundEnd(new RoundEndEventEntity(sameTime));
            
            // Round 2 (same timestamp - edge case)
            RoundStartEventEntity round2Start = new RoundStartEventEntity(sameTime);
            context.onRoundStart(round2Start);
            KillEventEntity kill2 = new KillEventEntity(sameTime);
            context.addEvent(kill2);
            context.onRoundEnd(new RoundEndEventEntity(sameTime));
            
            // Then - kills should reference their respective rounds despite same timestamp
            assertSame(round1Start, kill1.getRoundStart());
            assertSame(round2Start, kill2.getRoundStart());
            assertNotSame(kill1.getRoundStart(), kill2.getRoundStart());
        }
    }

    @Nested
    @DisplayName("Bomb Event Linking Tests")
    class BombEventTests {

        @Test
        @DisplayName("Should link bomb plant event to game and round")
        void shouldLinkBombPlantToGameAndRound() {
            // Given
            GameEntity game = createGameEntity(Instant.now(), "de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            BombEventEntity bombPlant = new BombEventEntity(Instant.now());
            bombPlant.setPlayer1("Planter");
            
            // When
            context.addEvent(bombPlant);
            
            // Then
            assertSame(game, bombPlant.getGame());
            assertSame(roundStart, bombPlant.getRoundStart());
        }
    }

    // ============ Helper Methods ============

    private GameEntity createGameEntity(Instant gameOverTimestamp, String map, int team1Score, int team2Score) {
        GameEntity game = new GameEntity();
        game.setAppServerId(100L); // Required field
        game.setGameOverTimestamp(gameOverTimestamp);
        game.setMap(map);
        game.setMode("competitive");
        game.setTeam1Score(team1Score);
        game.setTeam2Score(team2Score);
        game.setEndTime(gameOverTimestamp);
        return game;
    }

    private void processRound(EventProcessingContext ctx, Instant startTime, Instant endTime, List<GameEventEntity> events) {
        RoundStartEventEntity roundStart = new RoundStartEventEntity(startTime);
        ctx.onRoundStart(roundStart);
        
        for (GameEventEntity event : events) {
            ctx.addEvent(event);
        }
        
        RoundEndEventEntity roundEnd = new RoundEndEventEntity(endTime);
        ctx.onRoundEnd(roundEnd);
    }

    private KillEventEntity createKillEvent(String killer, String victim, Instant timestamp) {
        KillEventEntity kill = new KillEventEntity(timestamp);
        kill.setPlayer1(killer);
        kill.setPlayer2(victim);
        kill.setWeapon("ak47");
        kill.setIsHeadshot(false);
        return kill;
    }

    private AssistEventEntity createAssistEvent(String assister, String victim, Instant timestamp) {
        AssistEventEntity assist = new AssistEventEntity(timestamp);
        assist.setPlayer1(assister);
        assist.setPlayer2(victim);
        assist.setAssistType("Regular");
        return assist;
    }

    private AttackEventEntity createAttackEvent(String attacker, String victim, int damage, Instant timestamp) {
        AttackEventEntity attack = new AttackEventEntity(timestamp);
        attack.setPlayer1(attacker);
        attack.setPlayer2(victim);
        attack.setDamage(damage);
        attack.setWeapon("ak47");
        attack.setHitGroup("chest");
        return attack;
    }
}
