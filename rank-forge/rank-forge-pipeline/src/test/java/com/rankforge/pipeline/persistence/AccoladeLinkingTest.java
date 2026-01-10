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

import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Accolade linking to GameEntity.
 * 
 * According to the plan:
 * - Accolades are parsed in CS2LogParser.parseGameOverEvent() BEFORE GameOverEvent is returned
 * - At this point, GameEntity doesn't exist yet
 * - Accolades need deferred storage or linking after GameEntity is created
 * 
 * Solution implemented:
 * - Accolades are queued in context via addAccolade()
 * - When GameEntity is created (setCurrentGame), linkAccoladesToGame() is called
 * - All pending accolades get their game reference set
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class AccoladeLinkingTest {

    private EventProcessingContext context;

    @BeforeEach
    void setUp() {
        context = new EventProcessingContext();
    }

    @Nested
    @DisplayName("Accolade Queue Tests")
    class AccoladeQueueTests {

        @Test
        @DisplayName("Should queue accolades before game exists")
        void shouldQueueAccoladesBeforeGameExists() {
            // Given - accolades parsed before GameEntity exists
            AccoladeEntity accolade1 = createAccolade("MVP", "Player1", "[U:1:123]", 1.0, 1, 100.0);
            AccoladeEntity accolade2 = createAccolade("TopKills", "Player2", "[U:1:456]", 15.0, 2, 85.0);
            
            // When
            context.addAccolade(accolade1);
            context.addAccolade(accolade2);
            
            // Then
            List<AccoladeEntity> pending = context.getPendingAccolades();
            assertEquals(2, pending.size());
            assertTrue(pending.contains(accolade1));
            assertTrue(pending.contains(accolade2));
        }

        @Test
        @DisplayName("Should not have game reference when queued")
        void shouldNotHaveGameReferenceWhenQueued() {
            // Given
            AccoladeEntity accolade = createAccolade("MVP", "Player1", "[U:1:123]", 1.0, 1, 100.0);
            
            // When
            context.addAccolade(accolade);
            
            // Then
            assertNull(accolade.getGame());
        }

        @Test
        @DisplayName("Should handle empty accolades list")
        void shouldHandleEmptyAccoladesList() {
            // Then
            assertTrue(context.getPendingAccolades().isEmpty());
        }
    }

    @Nested
    @DisplayName("Accolade Linking Tests")
    class AccoladeLinkingTests {

        @Test
        @DisplayName("Should link pending accolades to game when linkAccoladesToGame is called")
        void shouldLinkAccoladesToGame() {
            // Given - accolades queued before game exists
            AccoladeEntity accolade1 = createAccolade("MVP", "Player1", "[U:1:123]", 1.0, 1, 100.0);
            AccoladeEntity accolade2 = createAccolade("TopKills", "Player2", "[U:1:456]", 15.0, 2, 85.0);
            AccoladeEntity accolade3 = createAccolade("TopAssists", "Player3", "[U:1:789]", 8.0, 3, 70.0);
            
            context.addAccolade(accolade1);
            context.addAccolade(accolade2);
            context.addAccolade(accolade3);
            
            // When - game is created and linked
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then - all accolades should reference the game
            assertAll("All accolades should reference the game",
                () -> assertSame(game, accolade1.getGame()),
                () -> assertSame(game, accolade2.getGame()),
                () -> assertSame(game, accolade3.getGame())
            );
        }

        @Test
        @DisplayName("Should not fail if linkAccoladesToGame called before setCurrentGame")
        void shouldNotFailIfLinkCalledBeforeSetGame() {
            // Given
            AccoladeEntity accolade = createAccolade("MVP", "Player1", "[U:1:123]", 1.0, 1, 100.0);
            context.addAccolade(accolade);
            
            // When - link called with no game set
            context.linkAccoladesToGame();
            
            // Then - should not throw, accolade remains without game
            assertNull(accolade.getGame());
        }

        @Test
        @DisplayName("Should handle no pending accolades when linkAccoladesToGame is called")
        void shouldHandleNoPendingAccolades() {
            // Given
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // When - no accolades queued
            assertDoesNotThrow(() -> context.linkAccoladesToGame());
            
            // Then
            assertTrue(context.getPendingAccolades().isEmpty());
        }
    }

    @Nested
    @DisplayName("Accolade and Game Flow Integration Tests")
    class AccoladeGameFlowTests {

        @Test
        @DisplayName("Should correctly process accolades in parser order: accolades first, then game")
        void shouldProcessAccoladesInParserOrder() {
            // Parser order: Accolades parsed → stored → GAME_OVER returned → GameEntity created
            
            // Step 1: Accolades parsed and queued (before GameEntity exists)
            List<AccoladeEntity> accolades = createTypicalGameAccolades();
            for (AccoladeEntity accolade : accolades) {
                context.addAccolade(accolade);
                assertNull(accolade.getGame()); // No game reference yet
            }
            
            // Step 2: GAME_OVER processed, GameEntity created
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // Step 3: Link accolades to game
            context.linkAccoladesToGame();
            
            // Verify all accolades now have game reference
            for (AccoladeEntity accolade : accolades) {
                assertSame(game, accolade.getGame());
            }
        }

        @Test
        @DisplayName("Should maintain accolade data integrity after linking")
        void shouldMaintainAccoladeDataIntegrity() {
            // Given
            AccoladeEntity accolade = createAccolade("3K", "AcePlayer", "[U:1:999]", 3.0, 1, 50.0);
            context.addAccolade(accolade);
            
            // When
            GameEntity game = createGame("de_mirage", 13, 16);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then - original data should be intact
            assertEquals("3K", accolade.getType());
            assertEquals("AcePlayer", accolade.getPlayerName());
            assertEquals("[U:1:999]", accolade.getPlayerId());
            assertEquals(3.0, accolade.getValue());
            assertEquals(1, accolade.getPosition());
            assertEquals(50.0, accolade.getScore());
            assertSame(game, accolade.getGame());
        }

        @Test
        @DisplayName("Should handle multiple games with separate accolades")
        void shouldHandleMultipleGamesWithSeparateAccolades() {
            // Game 1 accolades
            AccoladeEntity g1Accolade1 = createAccolade("MVP", "G1Player1", "[U:1:111]", 1.0, 1, 100.0);
            AccoladeEntity g1Accolade2 = createAccolade("TopKills", "G1Player2", "[U:1:222]", 20.0, 2, 90.0);
            
            context.addAccolade(g1Accolade1);
            context.addAccolade(g1Accolade2);
            
            GameEntity game1 = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game1);
            context.linkAccoladesToGame();
            
            // Simulate GAME_PROCESSED - batch save and clear
            // In real code: accoladeRepository.saveAll(context.getPendingAccolades())
            context.clear();
            
            // Game 2 accolades
            AccoladeEntity g2Accolade1 = createAccolade("MVP", "G2Player1", "[U:1:333]", 1.0, 1, 95.0);
            AccoladeEntity g2Accolade2 = createAccolade("TopDamage", "G2Player2", "[U:1:444]", 3500.0, 2, 88.0);
            
            context.addAccolade(g2Accolade1);
            context.addAccolade(g2Accolade2);
            
            GameEntity game2 = createGame("de_mirage", 13, 16);
            context.setCurrentGame(game2);
            context.linkAccoladesToGame();
            
            // Verify game 1 accolades still reference game 1
            assertSame(game1, g1Accolade1.getGame());
            assertSame(game1, g1Accolade2.getGame());
            
            // Verify game 2 accolades reference game 2
            assertSame(game2, g2Accolade1.getGame());
            assertSame(game2, g2Accolade2.getGame());
            
            // Verify they reference different games
            assertNotSame(g1Accolade1.getGame(), g2Accolade1.getGame());
        }

        @Test
        @DisplayName("Should clear pending accolades after context.clear()")
        void shouldClearPendingAccoladesAfterClear() {
            // Given
            context.addAccolade(createAccolade("MVP", "Player1", "[U:1:123]", 1.0, 1, 100.0));
            context.addAccolade(createAccolade("TopKills", "Player2", "[U:1:456]", 15.0, 2, 85.0));
            
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            assertEquals(2, context.getPendingAccolades().size());
            
            // When
            context.clear();
            
            // Then
            assertTrue(context.getPendingAccolades().isEmpty());
            assertNull(context.getCurrentGame());
        }
    }

    @Nested
    @DisplayName("Accolade Types Coverage Tests")
    class AccoladeTypesCoverageTests {

        @Test
        @DisplayName("Should handle all common accolade types")
        void shouldHandleAllCommonAccoladeTypes() {
            // Given - various accolade types from CS2
            List<AccoladeEntity> accolades = List.of(
                createAccolade("MVP", "Player1", "[U:1:1]", 1.0, 1, 100.0),
                createAccolade("TopKills", "Player2", "[U:1:2]", 25.0, 2, 95.0),
                createAccolade("TopAssists", "Player3", "[U:1:3]", 12.0, 3, 80.0),
                createAccolade("TopDamage", "Player4", "[U:1:4]", 4500.0, 4, 85.0),
                createAccolade("3K", "Player5", "[U:1:5]", 3.0, 5, 60.0),
                createAccolade("4K", "Player6", "[U:1:6]", 4.0, 6, 75.0),
                createAccolade("5K", "Player7", "[U:1:7]", 5.0, 7, 100.0),
                createAccolade("Headshots", "Player8", "[U:1:8]", 15.0, 8, 70.0),
                createAccolade("Clutches", "Player9", "[U:1:9]", 3.0, 9, 65.0),
                createAccolade("FirstKills", "Player10", "[U:1:10]", 8.0, 10, 55.0)
            );
            
            // When
            for (AccoladeEntity accolade : accolades) {
                context.addAccolade(accolade);
            }
            
            GameEntity game = createGame("de_inferno", 16, 10);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then - all should be linked
            assertEquals(10, context.getPendingAccolades().size());
            for (AccoladeEntity accolade : context.getPendingAccolades()) {
                assertSame(game, accolade.getGame(), 
                    "Accolade type " + accolade.getType() + " should reference the game");
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle accolade with null playerId (bot or unknown)")
        void shouldHandleAccoladeWithNullPlayerId() {
            // Given
            AccoladeEntity accolade = createAccolade("TopKills", "BOT Expert", null, 10.0, 1, 50.0);
            
            // When
            context.addAccolade(accolade);
            GameEntity game = createGame("de_dust2", 16, 0);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then
            assertNull(accolade.getPlayerId());
            assertSame(game, accolade.getGame());
        }

        @Test
        @DisplayName("Should handle accolade with zero values")
        void shouldHandleAccoladeWithZeroValues() {
            // Given
            AccoladeEntity accolade = createAccolade("Participation", "Player1", "[U:1:123]", 0.0, 10, 0.0);
            
            // When
            context.addAccolade(accolade);
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then
            assertEquals(0.0, accolade.getValue());
            assertEquals(0.0, accolade.getScore());
            assertSame(game, accolade.getGame());
        }

        @Test
        @DisplayName("Should handle game with no accolades")
        void shouldHandleGameWithNoAccolades() {
            // Given - no accolades added
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            
            // When
            context.linkAccoladesToGame();
            
            // Then
            assertTrue(context.getPendingAccolades().isEmpty());
            assertSame(game, context.getCurrentGame());
        }

        @Test
        @DisplayName("Should handle duplicate accolade entries")
        void shouldHandleDuplicateAccoladeEntries() {
            // Given - same player gets multiple accolades
            AccoladeEntity accolade1 = createAccolade("TopKills", "Player1", "[U:1:123]", 25.0, 1, 100.0);
            AccoladeEntity accolade2 = createAccolade("TopDamage", "Player1", "[U:1:123]", 4500.0, 2, 95.0);
            AccoladeEntity accolade3 = createAccolade("MVP", "Player1", "[U:1:123]", 1.0, 3, 90.0);
            
            // When
            context.addAccolade(accolade1);
            context.addAccolade(accolade2);
            context.addAccolade(accolade3);
            
            GameEntity game = createGame("de_dust2", 16, 14);
            context.setCurrentGame(game);
            context.linkAccoladesToGame();
            
            // Then - all should be linked
            assertEquals(3, context.getPendingAccolades().size());
            assertAll("All accolades for same player should reference game",
                () -> assertSame(game, accolade1.getGame()),
                () -> assertSame(game, accolade2.getGame()),
                () -> assertSame(game, accolade3.getGame())
            );
        }
    }

    // ============ Helper Methods ============

    private AccoladeEntity createAccolade(String type, String playerName, String playerId, 
                                           double value, int position, double score) {
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setType(type);
        accolade.setPlayerName(playerName);
        accolade.setPlayerId(playerId);
        accolade.setValue(value);
        accolade.setPosition(position);
        accolade.setScore(score);
        return accolade;
    }

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

    private List<AccoladeEntity> createTypicalGameAccolades() {
        List<AccoladeEntity> accolades = new ArrayList<>();
        accolades.add(createAccolade("MVP", "Player1", "[U:1:111]", 1.0, 1, 100.0));
        accolades.add(createAccolade("TopKills", "Player2", "[U:1:222]", 22.0, 2, 92.0));
        accolades.add(createAccolade("TopAssists", "Player3", "[U:1:333]", 10.0, 3, 78.0));
        accolades.add(createAccolade("TopDamage", "Player4", "[U:1:444]", 3800.0, 4, 85.0));
        accolades.add(createAccolade("3K", "Player5", "[U:1:555]", 2.0, 5, 55.0));
        return accolades;
    }
}
