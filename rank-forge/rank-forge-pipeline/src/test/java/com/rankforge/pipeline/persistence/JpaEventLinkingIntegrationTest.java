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

import com.rankforge.core.events.GameEventType;
import com.rankforge.pipeline.persistence.entity.*;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style unit tests for event linking validation.
 * 
 * These tests validate:
 * 1. Entity references are correctly set when processed through EventProcessingContext
 * 2. Game and round relationships are properly established in pending entities
 * 3. Accolades are linked to games before persistence
 * 4. Batch persistence preparation contains all required entities with correct references
 * 
 * Note: These are unit tests that verify entity state directly without mocking.
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class JpaEventLinkingIntegrationTest {

    private EventProcessingContext context;

    @BeforeEach
    void setUp() {
        context = new EventProcessingContext();
    }

    @Nested
    @DisplayName("Game Entity Reference Validation")
    class GameEntityReferenceTests {

        @Test
        @DisplayName("Should set game reference on all event entities")
        void shouldSetGameReferenceOnAllEntities() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity kill = new KillEventEntity(Instant.now());
            kill.setPlayer1("Killer");
            kill.setPlayer2("Victim");
            context.addEvent(kill);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now());
            assist.setPlayer1("Assister");
            context.addEvent(assist);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now());
            context.onRoundEnd(roundEnd);
            
            // Then - verify all entities have game reference
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            assertEquals(4, pendingEntities.size());
            
            for (GameEventEntity entity : pendingEntities) {
                assertSame(game, entity.getGame(), 
                    "Entity " + entity.getClass().getSimpleName() + " should have game reference set");
            }
        }

        @Test
        @DisplayName("Should propagate same GameEntity instance to all events in multi-round game")
        void shouldPropagateSameGameInstanceToAllEvents() {
            // Given
            GameEntity game = createGame("de_mirage", 13, 16);
            context.setCurrentGame(game);
            
            // Add multiple rounds with events
            for (int i = 0; i < 5; i++) {
                RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now().plusSeconds(i * 60));
                context.onRoundStart(roundStart);
                
                KillEventEntity kill = new KillEventEntity(Instant.now().plusSeconds(i * 60 + 10));
                context.addEvent(kill);
                
                context.onRoundEnd(new RoundEndEventEntity(Instant.now().plusSeconds(i * 60 + 50)));
            }
            
            // Then
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            assertEquals(15, pendingEntities.size()); // 5 rounds × 3 events each
            
            // All should reference exact same GameEntity instance
            for (GameEventEntity entity : pendingEntities) {
                assertSame(game, entity.getGame(), "All events should reference same game instance");
            }
        }
    }

    @Nested
    @DisplayName("Round Start Reference Validation")
    class RoundStartReferenceTests {

        @Test
        @DisplayName("Should set roundStart reference on in-round events")
        void shouldSetRoundStartReferenceOnInRoundEvents() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            KillEventEntity kill1 = new KillEventEntity(Instant.now().plusMillis(10));
            kill1.setPlayer1("Killer1");
            context.addEvent(kill1);
            
            KillEventEntity kill2 = new KillEventEntity(Instant.now().plusMillis(20));
            kill2.setPlayer1("Killer2");
            context.addEvent(kill2);
            
            AttackEventEntity attack = new AttackEventEntity(Instant.now().plusMillis(15));
            attack.setPlayer1("Attacker");
            attack.setDamage(45);
            context.addEvent(attack);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(1));
            context.onRoundEnd(roundEnd);
            
            // Then - all in-round events should reference the same roundStart
            List<GameEventEntity> inRoundEvents = context.getPendingEntities().stream()
                .filter(e -> e.getGameEventType() != GameEventType.ROUND_START)
                .collect(Collectors.toList());
            
            for (GameEventEntity entity : inRoundEvents) {
                assertSame(roundStart, entity.getRoundStart(),
                    "Entity " + entity.getClass().getSimpleName() + " should reference roundStart");
            }
        }

        @Test
        @DisplayName("Should correctly separate events across multiple rounds")
        void shouldCorrectlySeparateEventsAcrossRounds() {
            // Given
            GameEntity game = createGame("de_dust2", 2, 0);
            context.setCurrentGame(game);
            
            // Round 1
            RoundStartEventEntity round1Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:00:00Z"));
            context.onRoundStart(round1Start);
            
            KillEventEntity round1Kill = new KillEventEntity(Instant.parse("2026-01-01T10:00:15Z"));
            round1Kill.setPlayer1("R1Killer");
            context.addEvent(round1Kill);
            
            RoundEndEventEntity round1End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:00:30Z"));
            context.onRoundEnd(round1End);
            
            // Round 2
            RoundStartEventEntity round2Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:01:00Z"));
            context.onRoundStart(round2Start);
            
            KillEventEntity round2Kill = new KillEventEntity(Instant.parse("2026-01-01T10:01:15Z"));
            round2Kill.setPlayer1("R2Killer");
            context.addEvent(round2Kill);
            
            AssistEventEntity round2Assist = new AssistEventEntity(Instant.parse("2026-01-01T10:01:16Z"));
            round2Assist.setPlayer1("R2Assister");
            context.addEvent(round2Assist);
            
            RoundEndEventEntity round2End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:01:30Z"));
            context.onRoundEnd(round2End);
            
            // Then - Group events by their roundStart
            Map<RoundStartEventEntity, List<GameEventEntity>> eventsByRound = context.getPendingEntities().stream()
                .filter(e -> e.getRoundStart() != null)
                .collect(Collectors.groupingBy(GameEventEntity::getRoundStart));
            
            // Should have 2 distinct rounds
            assertEquals(2, eventsByRound.size(), "Should have 2 distinct rounds");
            
            // Round 1 events (kill + roundEnd = 2)
            List<GameEventEntity> r1Events = eventsByRound.get(round1Start);
            assertNotNull(r1Events);
            assertEquals(2, r1Events.size());
            assertTrue(r1Events.stream().anyMatch(e -> "R1Killer".equals(e.getPlayer1())));
            
            // Round 2 events (kill + assist + roundEnd = 3)
            List<GameEventEntity> r2Events = eventsByRound.get(round2Start);
            assertNotNull(r2Events);
            assertEquals(3, r2Events.size());
            assertTrue(r2Events.stream().anyMatch(e -> "R2Killer".equals(e.getPlayer1())));
            assertTrue(r2Events.stream().anyMatch(e -> "R2Assister".equals(e.getPlayer1())));
        }

        @Test
        @DisplayName("RoundEndEventEntity should reference its RoundStartEventEntity")
        void roundEndShouldReferenceRoundStart() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(30));
            context.onRoundEnd(roundEnd);
            
            // Then - verify reference
            assertSame(roundStart, roundEnd.getRoundStart());
            
            // And verify it's in pending entities
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            RoundEndEventEntity savedRoundEnd = (RoundEndEventEntity) pendingEntities.stream()
                .filter(e -> e instanceof RoundEndEventEntity)
                .findFirst().orElseThrow();
            
            assertSame(roundStart, savedRoundEnd.getRoundStart());
        }
    }

    @Nested
    @DisplayName("Accolade Linking Validation")
    class AccoladeLinkingTests {

        @Test
        @DisplayName("Should link pending accolades to game")
        void shouldLinkAccoladesToGame() {
            // Given - accolades added before game exists (parser order)
            AccoladeEntity accolade1 = createAccolade("MVP", "Player1", 1.0, 1, 100.0);
            AccoladeEntity accolade2 = createAccolade("TopKills", "Player2", 25.0, 2, 85.0);
            AccoladeEntity accolade3 = createAccolade("TopDamage", "Player3", 4500.0, 3, 75.0);
            
            context.addAccolade(accolade1);
            context.addAccolade(accolade2);
            context.addAccolade(accolade3);
            
            // Verify no game reference yet
            assertNull(accolade1.getGame());
            assertNull(accolade2.getGame());
            assertNull(accolade3.getGame());
            
            // When - game is created and linked
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then - accolades should have game reference
            assertSame(game, accolade1.getGame());
            assertSame(game, accolade2.getGame());
            assertSame(game, accolade3.getGame());
            
            // Verify in pending accolades
            List<AccoladeEntity> pendingAccolades = context.getPendingAccolades();
            assertEquals(3, pendingAccolades.size());
            
            for (AccoladeEntity accolade : pendingAccolades) {
                assertSame(game, accolade.getGame(), 
                    "Accolade " + accolade.getType() + " should have game reference");
            }
        }

        @Test
        @DisplayName("Should maintain accolade order as added")
        void shouldMaintainAccoladeOrderAsAdded() {
            // Given - add in specific order
            AccoladeEntity mvp = createAccolade("MVP", "Player1", 1.0, 1, 100.0);
            AccoladeEntity topKills = createAccolade("TopKills", "Player2", 25.0, 2, 85.0);
            AccoladeEntity topDamage = createAccolade("TopDamage", "Player3", 4500.0, 3, 75.0);
            
            // Add in reverse order
            context.addAccolade(topDamage);
            context.addAccolade(topKills);
            context.addAccolade(mvp);
            
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then - verify order is preserved as added
            List<AccoladeEntity> pendingAccolades = context.getPendingAccolades();
            assertEquals(3, pendingAccolades.size());
            assertEquals("TopDamage", pendingAccolades.get(0).getType()); // First added
            assertEquals("TopKills", pendingAccolades.get(1).getType());
            assertEquals("MVP", pendingAccolades.get(2).getType()); // Last added
        }
    }

    @Nested
    @DisplayName("Complete Game Flow Validation")
    class CompleteGameFlowTests {

        @Test
        @DisplayName("Should correctly prepare all entities for batch persistence")
        void shouldCorrectlyPrepareAllEntitiesForBatchPersistence() {
            // Given - simulate complete game processing flow
            // Step 1: Accolades (parsed before game)
            AccoladeEntity mvp = createAccolade("MVP", "StarPlayer", 1.0, 1, 100.0);
            AccoladeEntity topKills = createAccolade("TopKills", "Fragger", 20.0, 2, 88.0);
            context.addAccolade(mvp);
            context.addAccolade(topKills);
            
            // Step 2: Game over processed
            GameEntity game = createGame("de_inferno", 16, 12);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            GameOverEventEntity gameOverEvent = new GameOverEventEntity(Instant.parse("2026-01-01T11:00:00Z"));
            gameOverEvent.setMap("de_inferno");
            gameOverEvent.setTeam1Score(16);
            gameOverEvent.setTeam2Score(12);
            context.addGameOverEvent(gameOverEvent);
            
            // Step 3: Rounds processed
            // Round 1
            RoundStartEventEntity round1Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:00:00Z"));
            context.onRoundStart(round1Start);
            
            KillEventEntity kill1 = new KillEventEntity(Instant.parse("2026-01-01T10:00:15Z"));
            kill1.setPlayer1("StarPlayer");
            kill1.setPlayer2("Enemy1");
            context.addEvent(kill1);
            
            RoundEndEventEntity round1End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:00:30Z"));
            context.onRoundEnd(round1End);
            
            // Round 2
            RoundStartEventEntity round2Start = new RoundStartEventEntity(Instant.parse("2026-01-01T10:01:00Z"));
            context.onRoundStart(round2Start);
            
            KillEventEntity kill2 = new KillEventEntity(Instant.parse("2026-01-01T10:01:15Z"));
            kill2.setPlayer1("Fragger");
            kill2.setPlayer2("Enemy2");
            context.addEvent(kill2);
            
            BombEventEntity bombPlant = new BombEventEntity(Instant.parse("2026-01-01T10:01:20Z"));
            bombPlant.setPlayer1("Planter");
            bombPlant.setEventType("planted");
            context.addEvent(bombPlant);
            
            RoundEndEventEntity round2End = new RoundEndEventEntity(Instant.parse("2026-01-01T10:01:45Z"));
            context.onRoundEnd(round2End);
            
            // Verify state
            List<GameEventEntity> pendingEvents = context.getPendingEntities();
            List<AccoladeEntity> pendingAccolades = context.getPendingAccolades();
            
            // Should have: gameOver, 2×roundStart, 2×kill, 1×bomb, 2×roundEnd = 8 events
            assertEquals(8, pendingEvents.size());
            assertEquals(2, pendingAccolades.size());
            
            // Verify all events reference game
            for (GameEventEntity event : pendingEvents) {
                assertSame(game, event.getGame());
            }
            
            // Verify accolades reference game
            for (AccoladeEntity accolade : pendingAccolades) {
                assertSame(game, accolade.getGame());
            }
            
            // Verify round separation
            assertSame(round1Start, kill1.getRoundStart());
            assertSame(round1Start, round1End.getRoundStart());
            assertSame(round2Start, kill2.getRoundStart());
            assertSame(round2Start, bombPlant.getRoundStart());
            assertSame(round2Start, round2End.getRoundStart());
        }

        @Test
        @DisplayName("Should handle multiple games independently")
        void shouldHandleMultipleGamesIndependently() {
            // Game 1
            GameEntity game1 = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game1);
            
            AccoladeEntity g1Accolade = createAccolade("MVP", "G1Player", 1.0, 1, 100.0);
            context.addAccolade(g1Accolade);
            context.linkAccoladesToGame();
            
            RoundStartEventEntity g1Round = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(g1Round);
            
            KillEventEntity g1Kill = new KillEventEntity(Instant.now());
            g1Kill.setPlayer1("G1Killer");
            context.addEvent(g1Kill);
            
            context.onRoundEnd(new RoundEndEventEntity(Instant.now()));
            
            // Verify game 1 state
            assertSame(game1, g1Kill.getGame());
            assertSame(g1Round, g1Kill.getRoundStart());
            assertSame(game1, g1Accolade.getGame());
            
            // Clear for next game
            context.clear();
            
            // Game 2
            GameEntity game2 = createGame("de_mirage", 13, 16);
            context.setCurrentGame(game2);
            
            AccoladeEntity g2Accolade = createAccolade("MVP", "G2Player", 1.0, 1, 95.0);
            context.addAccolade(g2Accolade);
            context.linkAccoladesToGame();
            
            RoundStartEventEntity g2Round = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(g2Round);
            
            KillEventEntity g2Kill = new KillEventEntity(Instant.now());
            g2Kill.setPlayer1("G2Killer");
            context.addEvent(g2Kill);
            
            context.onRoundEnd(new RoundEndEventEntity(Instant.now()));
            
            // Verify game 2 state is independent
            assertSame(game2, g2Kill.getGame());
            assertSame(g2Round, g2Kill.getRoundStart());
            assertSame(game2, g2Accolade.getGame());
            
            // Verify games are different
            assertNotSame(game1, game2);
            
            // Game 1 entities still have correct references (immutable after set)
            assertSame(game1, g1Kill.getGame());
            assertSame(g1Round, g1Kill.getRoundStart());
            assertSame(game1, g1Accolade.getGame());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty round (no kills)")
        void shouldHandleEmptyRound() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            // No kills - bomb exploded or timeout
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(115));
            context.onRoundEnd(roundEnd);
            
            // Then
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            assertEquals(2, pendingEntities.size()); // Only roundStart and roundEnd
            
            assertSame(game, roundStart.getGame());
            assertSame(game, roundEnd.getGame());
            assertSame(roundStart, roundEnd.getRoundStart());
        }

        @Test
        @DisplayName("Should handle events before round start (outside round context)")
        void shouldHandleEventsBeforeRoundStart() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // Event added before any round
            KillEventEntity kill = new KillEventEntity(Instant.now());
            kill.setPlayer1("EarlyKiller");
            context.addEvent(kill);
            
            // Then
            assertSame(game, kill.getGame());
            assertNull(kill.getRoundStart()); // No round context yet
            
            // Later, normal round
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now().plusSeconds(1));
            context.onRoundStart(roundStart);
            
            KillEventEntity normalKill = new KillEventEntity(Instant.now().plusSeconds(10));
            normalKill.setPlayer1("NormalKiller");
            context.addEvent(normalKill);
            
            context.onRoundEnd(new RoundEndEventEntity(Instant.now().plusSeconds(30)));
            
            // Verify
            assertNull(kill.getRoundStart()); // Still no round context
            assertSame(roundStart, normalKill.getRoundStart()); // Has round context
        }

        @Test
        @DisplayName("Should handle game with all event types")
        void shouldHandleGameWithAllEventTypes() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // Game over
            GameOverEventEntity gameOver = new GameOverEventEntity(Instant.now());
            gameOver.setMap("de_dust2");
            context.addGameOverEvent(gameOver);
            
            // Round with all event types
            RoundStartEventEntity roundStart = new RoundStartEventEntity(Instant.now());
            context.onRoundStart(roundStart);
            
            AttackEventEntity attack = new AttackEventEntity(Instant.now().plusMillis(100));
            attack.setPlayer1("Attacker");
            attack.setDamage(45);
            context.addEvent(attack);
            
            KillEventEntity kill = new KillEventEntity(Instant.now().plusMillis(200));
            kill.setPlayer1("Killer");
            kill.setIsHeadshot(true);
            context.addEvent(kill);
            
            AssistEventEntity assist = new AssistEventEntity(Instant.now().plusMillis(210));
            assist.setPlayer1("Assister");
            context.addEvent(assist);
            
            BombEventEntity bombPlant = new BombEventEntity(Instant.now().plusMillis(300));
            bombPlant.setEventType("planted");
            context.addEvent(bombPlant);
            
            BombEventEntity bombDefuse = new BombEventEntity(Instant.now().plusMillis(400));
            bombDefuse.setEventType("defused");
            context.addEvent(bombDefuse);
            
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(Instant.now().plusSeconds(1));
            context.onRoundEnd(roundEnd);
            
            // Then
            List<GameEventEntity> pendingEntities = context.getPendingEntities();
            
            // Should have: gameOver, roundStart, attack, kill, assist, 2×bomb, roundEnd = 8
            assertEquals(8, pendingEntities.size());
            
            // All should reference game
            for (GameEventEntity entity : pendingEntities) {
                assertSame(game, entity.getGame());
            }
            
            // Verify specific event types are present
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof GameOverEventEntity));
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof RoundStartEventEntity));
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof AttackEventEntity));
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof KillEventEntity));
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof AssistEventEntity));
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof BombEventEntity));
            assertTrue(pendingEntities.stream().anyMatch(e -> e instanceof RoundEndEventEntity));
        }
    }

    // ============ Helper Methods ============

    private GameEntity createGame(String map, int team1Score, int team2Score) {
        GameEntity game = new GameEntity();
        game.setMap(map);
        game.setMode("competitive");
        game.setTeam1Score(team1Score);
        game.setTeam2Score(team2Score);
        game.setGameOverTimestamp(Instant.now());
        game.setEndTime(Instant.now());
        return game;
    }

    private AccoladeEntity createAccolade(String type, String playerName, double value, int position, double score) {
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setType(type);
        accolade.setPlayerName(playerName);
        accolade.setPlayerId("[U:1:" + playerName.hashCode() + "]");
        accolade.setValue(value);
        accolade.setPosition(position);
        accolade.setScore(score);
        return accolade;
    }
}
