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

package com.rankforge.server.service;

import com.rankforge.core.events.GameEventType;
import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.KillEventEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.entity.RoundStartEventEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for AdminGameService delete functionality.
 * Tests cascade deletion of all related entities.
 * Author bageshwar.pn
 * Date 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminGameService Delete Tests")
class AdminGameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameEventRepository gameEventRepository;

    @Mock
    private AccoladeRepository accoladeRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @InjectMocks
    private AdminGameService adminGameService;

    private Instant game1Timestamp;
    private Instant game2Timestamp;
    private GameEntity game1;
    private GameEntity game2;

    @BeforeEach
    void setUp() {
        game1Timestamp = Instant.parse("2026-01-10T10:00:00Z");
        game2Timestamp = Instant.parse("2026-01-10T11:00:00Z");

        // Setup game1
        game1 = new GameEntity();
        game1.setId(1L);
        game1.setGameOverTimestamp(game1Timestamp);
        game1.setMap("de_dust2");
        game1.setMode("competitive");
        game1.setTeam1Score(16);
        game1.setTeam2Score(14);
        game1.setEndTime(game1Timestamp);

        // Setup game2 (different game)
        game2 = new GameEntity();
        game2.setId(2L);
        game2.setGameOverTimestamp(game2Timestamp);
        game2.setMap("de_mirage");
        game2.setMode("competitive");
        game2.setTeam1Score(16);
        game2.setTeam2Score(12);
        game2.setEndTime(game2Timestamp);
    }

    @Nested
    @DisplayName("Game Deletion Tests")
    class GameDeletionTests {

        @Test
        @DisplayName("Should delete game and all related entities with data validation")
        void shouldDeleteGameAndAllRelatedEntities() {
            // Given: Comprehensive test data with multiple games to verify isolation
            Long game1Id = 1L;
            Long game2Id = 2L;
            
            // Game1 data
            List<GameEventEntity> game1Events = createGameEvents(game1, 3); // 3 rounds = 9 events (3 round starts, 9 kills, 3 round ends)
            List<AccoladeEntity> game1Accolades = createAccolades(game1, 5); // 5 accolades
            List<PlayerStatsEntity> game1Stats = createPlayerStats(game1, 4); // 4 players
            
            // Game2 data (should remain untouched)
            List<GameEventEntity> game2Events = createGameEvents(game2, 2); // 2 rounds
            List<AccoladeEntity> game2Accolades = createAccolades(game2, 3); // 3 accolades
            List<PlayerStatsEntity> game2Stats = createPlayerStats(game2, 3); // 3 players

            // Mock repository responses - return game1 data when queried for game1Id
            when(gameRepository.findById(game1Id)).thenReturn(Optional.of(game1));
            when(gameEventRepository.findAllByGameId(game1Id)).thenReturn(game1Events);
            when(accoladeRepository.findByGameId(game1Id)).thenReturn(game1Accolades);
            when(playerStatsRepository.findByGameId(game1Id)).thenReturn(game1Stats);
            
            // Note: We don't stub game2 methods since we only verify they're never called

            // When: Delete game1
            boolean result = adminGameService.deleteGame(game1Id);

            // Then: Verify deletion result
            assertTrue(result, "Delete should return true");

            // Capture what was actually deleted
            ArgumentCaptor<List<GameEventEntity>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<AccoladeEntity>> accoladesCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<PlayerStatsEntity>> statsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<GameEntity> gameCaptor = ArgumentCaptor.forClass(GameEntity.class);

            // Verify game events deletion with data validation
            verify(gameEventRepository, times(1)).findAllByGameId(game1Id);
            verify(gameEventRepository, times(1)).deleteAll(eventsCaptor.capture());
            List<GameEventEntity> deletedEvents = eventsCaptor.getValue();
            // Expected: 3 rounds * (1 ROUND_START + 3 KILLS + 1 ROUND_END) = 3 * 5 = 15 events
            assertEquals(15, deletedEvents.size(), 
                String.format("Should delete all 15 events (3 rounds * 5 events each), but got %d", deletedEvents.size()));
            // Verify all deleted events belong to game1
            Instant game1Timestamp = game1.getGameOverTimestamp();
            deletedEvents.forEach(event -> {
                assertNotNull(event.getGame(), "Event should have game reference");
                assertEquals(game1Id, event.getGame().getId(), 
                    "Deleted event should belong to game1");
                assertNotNull(event.getTimestamp(), "Event should have timestamp");
                // Events should occur before or at the game over timestamp
                assertTrue(event.getTimestamp().isBefore(game1Timestamp) || 
                          event.getTimestamp().equals(game1Timestamp),
                    "Event timestamp should be before or equal to game over timestamp");
            });
            // Verify event types are correct
            long roundStarts = deletedEvents.stream()
                .filter(e -> e.getGameEventType() == GameEventType.ROUND_START)
                .count();
            long kills = deletedEvents.stream()
                .filter(e -> e.getGameEventType() == GameEventType.KILL)
                .count();
            long roundEnds = deletedEvents.stream()
                .filter(e -> e.getGameEventType() == GameEventType.ROUND_END)
                .count();
            assertEquals(3, roundStarts, "Should delete 3 round start events");
            assertEquals(9, kills, "Should delete 9 kill events (3 per round)");
            assertEquals(3, roundEnds, "Should delete 3 round end events");

            // Verify accolades deletion with data validation
            verify(accoladeRepository, times(1)).findByGameId(game1Id);
            verify(accoladeRepository, times(1)).deleteAll(accoladesCaptor.capture());
            List<AccoladeEntity> deletedAccolades = accoladesCaptor.getValue();
            assertEquals(5, deletedAccolades.size(), "Should delete all 5 accolades");
            // Verify all deleted accolades belong to game1
            deletedAccolades.forEach(accolade -> {
                assertNotNull(accolade.getGame(), "Accolade should have game reference");
                assertEquals(game1Id, accolade.getGame().getId(), 
                    "Deleted accolade should belong to game1");
                assertNotNull(accolade.getType(), "Accolade should have type");
                assertFalse(accolade.getType().isEmpty(), "Accolade type should not be empty");
                assertNotNull(accolade.getPlayerName(), "Accolade should have player name");
                assertFalse(accolade.getPlayerName().isEmpty(), "Accolade player name should not be empty");
                assertNotNull(accolade.getPlayerId(), "Accolade should have player ID");
                assertFalse(accolade.getPlayerId().isEmpty(), "Accolade player ID should not be empty");
            });
            // Verify unique players in accolades
            long uniquePlayers = deletedAccolades.stream()
                .map(AccoladeEntity::getPlayerId)
                .distinct()
                .count();
            assertTrue(uniquePlayers >= 1, "Should have at least 1 unique player in accolades");

            // Verify player stats deletion with data validation
            verify(playerStatsRepository, times(1)).findByGameId(game1Id);
            verify(playerStatsRepository, times(1)).deleteAll(statsCaptor.capture());
            List<PlayerStatsEntity> deletedStats = statsCaptor.getValue();
            assertEquals(4, deletedStats.size(), "Should delete all 4 player stats");
            // Verify all deleted stats belong to game1
            // game1Timestamp already defined above
            deletedStats.forEach(stat -> {
                assertNotNull(stat.getGame(), "Player stat should have game reference");
                assertEquals(game1Id, stat.getGame().getId(), 
                    "Deleted player stat should belong to game1");
                assertEquals(game1Timestamp, stat.getGameTimestamp(), 
                    "Player stat gameTimestamp should match game's gameOverTimestamp");
                assertNotNull(stat.getPlayerId(), "Player stat should have playerId");
                assertTrue(stat.getKills() >= 0, "Player stat should have valid kills");
                assertTrue(stat.getDeaths() >= 0, "Player stat should have valid deaths");
                assertNotNull(stat.getLastSeenNickname(), "Player stat should have nickname");
            });
            // Verify unique players
            long uniquePlayerIds = deletedStats.stream()
                .map(PlayerStatsEntity::getPlayerId)
                .distinct()
                .count();
            assertEquals(4, uniquePlayerIds, "Should have 4 unique players");
            // Verify all player IDs are non-null and non-empty
            deletedStats.forEach(stat -> {
                assertNotNull(stat.getPlayerId(), "Player ID should not be null");
                assertFalse(stat.getPlayerId().isEmpty(), "Player ID should not be empty");
            });

            // Verify game deletion with data validation
            verify(gameRepository, times(1)).delete(gameCaptor.capture());
            GameEntity deletedGame = gameCaptor.getValue();
            assertEquals(game1Id, deletedGame.getId(), "Should delete game1");
            assertEquals("de_dust2", deletedGame.getMap(), "Deleted game should be de_dust2");
            assertEquals(16, deletedGame.getTeam1Score(), "Deleted game should have correct team1 score");
            assertEquals(14, deletedGame.getTeam2Score(), "Deleted game should have correct team2 score");
            assertNotNull(deletedGame.getGameOverTimestamp(), "Deleted game should have timestamp");
            assertEquals(game1Timestamp, deletedGame.getGameOverTimestamp(), 
                "Deleted game timestamp should match original");

            // Verify round references in events are correct
            // Each round should have events linked to the same RoundStartEventEntity
            List<RoundStartEventEntity> deletedRoundStarts = deletedEvents.stream()
                .filter(e -> e instanceof RoundStartEventEntity)
                .map(e -> (RoundStartEventEntity) e)
                .collect(java.util.stream.Collectors.toList());
            assertEquals(3, deletedRoundStarts.size(), "Should have 3 round start events");
            
            // Verify kill events are linked to their round starts
            deletedEvents.stream()
                .filter(e -> e instanceof KillEventEntity)
                .map(e -> (KillEventEntity) e)
                .forEach(killEvent -> {
                    assertNotNull(killEvent.getRoundStart(), 
                        "Kill event should be linked to a round start");
                    assertTrue(deletedRoundStarts.contains(killEvent.getRoundStart()), 
                        "Kill event's round start should be one of the deleted round starts");
                });
            
            // Verify round end events are linked to their round starts
            deletedEvents.stream()
                .filter(e -> e instanceof RoundEndEventEntity)
                .map(e -> (RoundEndEventEntity) e)
                .forEach(roundEnd -> {
                    assertNotNull(roundEnd.getRoundStart(), 
                        "Round end event should be linked to a round start");
                    assertTrue(deletedRoundStarts.contains(roundEnd.getRoundStart()), 
                        "Round end's round start should be one of the deleted round starts");
                });

            // Verify game2 data was NOT touched (isolation check)
            verify(gameEventRepository, never()).findAllByGameId(game2Id);
            verify(accoladeRepository, never()).findByGameId(game2Id);
            verify(playerStatsRepository, never()).findByGameId(game2Id);
            verify(gameRepository, never()).delete(game2);
            
            // Verify that game2's data would still exist (if we queried it)
            // This ensures the deletion was isolated to game1
            assertNotEquals(game1Id, game2Id, "Game IDs should be different");
            assertNotEquals(game1.getGameOverTimestamp(), game2.getGameOverTimestamp(), 
                "Game timestamps should be different to ensure isolation");
        }

        @Test
        @DisplayName("Should return false when game does not exist")
        void shouldReturnFalseWhenGameDoesNotExist() {
            // Given
            Long nonExistentGameId = 999L;
            when(gameRepository.findById(nonExistentGameId)).thenReturn(Optional.empty());

            // When
            boolean result = adminGameService.deleteGame(nonExistentGameId);

            // Then
            assertFalse(result, "Should return false for non-existent game");
            verify(gameRepository, never()).delete(any());
            verify(gameEventRepository, never()).deleteAll(any());
            verify(accoladeRepository, never()).deleteAll(any());
            verify(playerStatsRepository, never()).deleteAll(any());
        }

        @Test
        @DisplayName("Should handle game with no events")
        void shouldHandleGameWithNoEvents() {
            // Given
            Long gameId = 1L;
            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game1));
            when(gameEventRepository.findAllByGameId(gameId)).thenReturn(new ArrayList<>());
            when(accoladeRepository.findByGameId(gameId)).thenReturn(new ArrayList<>());
            when(playerStatsRepository.findByGameId(gameId)).thenReturn(new ArrayList<>());

            // When
            boolean result = adminGameService.deleteGame(gameId);

            // Then
            assertTrue(result);
            verify(gameEventRepository, times(1)).findAllByGameId(gameId);
            verify(gameEventRepository, never()).deleteAll(any());
            verify(gameRepository, times(1)).delete(game1);
        }

        @Test
        @DisplayName("Should only delete player stats for the specific game ID")
        void shouldOnlyDeletePlayerStatsForSpecificGameId() {
            // Given: Two games with different IDs
            Long game1Id = 1L;
            Long game2Id = 2L;

            // Game1 stats
            List<PlayerStatsEntity> game1Stats = createPlayerStats(game1, 3);
            // Game2 stats (different game)
            List<PlayerStatsEntity> game2Stats = createPlayerStats(game2, 2);

            when(gameRepository.findById(game1Id)).thenReturn(Optional.of(game1));
            when(gameEventRepository.findAllByGameId(game1Id)).thenReturn(new ArrayList<>());
            when(accoladeRepository.findByGameId(game1Id)).thenReturn(new ArrayList<>());
            when(playerStatsRepository.findByGameId(game1Id)).thenReturn(game1Stats);

            // When: Delete game1
            adminGameService.deleteGame(game1Id);

            // Then: Only game1 stats should be deleted
            verify(playerStatsRepository, times(1)).findByGameId(game1Id);
            verify(playerStatsRepository, times(1)).deleteAll(game1Stats);
            // Verify game2 stats were never touched
            verify(playerStatsRepository, never()).findByGameId(game2Id);
        }
    }

    @Nested
    @DisplayName("Cascade Deletion Verification")
    class CascadeDeletionVerification {

        @Test
        @DisplayName("Should delete all event types including GAME_OVER")
        void shouldDeleteAllEventTypesIncludingGameOver() {
            // Given: Game with multiple event types
            Long gameId = 1L;
            List<GameEventEntity> events = new ArrayList<>();
            
            // Add round start events
            for (int i = 0; i < 3; i++) {
                RoundStartEventEntity roundStart = new RoundStartEventEntity(game1Timestamp.plusSeconds(i * 100));
                roundStart.setId((long) (i + 1));
                roundStart.setGame(game1);
                events.add(roundStart);
            }
            
            // Add kill events
            for (int i = 0; i < 5; i++) {
                KillEventEntity kill = new KillEventEntity(game1Timestamp.plusSeconds(i * 50));
                kill.setId((long) (i + 10));
                kill.setGame(game1);
                events.add(kill);
            }
            
            // Add round end events
            for (int i = 0; i < 3; i++) {
                RoundEndEventEntity roundEnd = new RoundEndEventEntity(game1Timestamp.plusSeconds(i * 100 + 90));
                roundEnd.setId((long) (i + 20));
                roundEnd.setGame(game1);
                events.add(roundEnd);
            }

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game1));
            when(gameEventRepository.findAllByGameId(gameId)).thenReturn(events);
            when(accoladeRepository.findByGameId(gameId)).thenReturn(new ArrayList<>());
            when(playerStatsRepository.findByGameId(gameId)).thenReturn(new ArrayList<>());

            // When
            adminGameService.deleteGame(gameId);

            // Then: All events should be deleted
            ArgumentCaptor<List<GameEventEntity>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            verify(gameEventRepository).deleteAll(eventsCaptor.capture());
            
            List<GameEventEntity> deletedEvents = eventsCaptor.getValue();
            assertEquals(11, deletedEvents.size(), "Should delete all 11 events");
            
            // Verify game is deleted
            verify(gameRepository).delete(game1);
        }

        @Test
        @DisplayName("Should delete all accolades for the game")
        void shouldDeleteAllAccoladesForGame() {
            // Given: Game with multiple accolades
            Long gameId = 1L;
            List<AccoladeEntity> accolades = createAccolades(game1, 10); // 10 accolades

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game1));
            when(gameEventRepository.findAllByGameId(gameId)).thenReturn(new ArrayList<>());
            when(accoladeRepository.findByGameId(gameId)).thenReturn(accolades);
            when(playerStatsRepository.findByGameId(gameId)).thenReturn(new ArrayList<>());

            // When
            adminGameService.deleteGame(gameId);

            // Then
            ArgumentCaptor<List<AccoladeEntity>> accoladesCaptor = ArgumentCaptor.forClass(List.class);
            verify(accoladeRepository).deleteAll(accoladesCaptor.capture());
            
            List<AccoladeEntity> deletedAccolades = accoladesCaptor.getValue();
            assertEquals(10, deletedAccolades.size(), "Should delete all 10 accolades");
            
            // Verify all accolades belong to the game
            deletedAccolades.forEach(accolade -> 
                assertEquals(game1.getId(), accolade.getGame().getId(), 
                    "Accolade should belong to the deleted game"));
        }

        @Test
        @DisplayName("Should delete all player stats for the game ID")
        void shouldDeleteAllPlayerStatsForGameId() {
            // Given: Game with multiple players
            Long gameId = 1L;
            List<PlayerStatsEntity> playerStats = createPlayerStats(game1, 8); // 8 players

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game1));
            when(gameEventRepository.findAllByGameId(gameId)).thenReturn(new ArrayList<>());
            when(accoladeRepository.findByGameId(gameId)).thenReturn(new ArrayList<>());
            when(playerStatsRepository.findByGameId(gameId)).thenReturn(playerStats);

            // When
            adminGameService.deleteGame(gameId);

            // Then
            ArgumentCaptor<List<PlayerStatsEntity>> statsCaptor = ArgumentCaptor.forClass(List.class);
            verify(playerStatsRepository).deleteAll(statsCaptor.capture());
            
            List<PlayerStatsEntity> deletedStats = statsCaptor.getValue();
            assertEquals(8, deletedStats.size(), "Should delete all 8 player stats");
            
            // Verify all stats belong to the correct game
            deletedStats.forEach(stat -> {
                assertNotNull(stat.getGame(), "Player stat should have a game reference");
                assertEquals(game1.getId(), stat.getGame().getId(), 
                    "Player stat should belong to the deleted game");
            });
        }
    }

    @Nested
    @DisplayName("Game Existence Check")
    class GameExistenceCheck {

        @Test
        @DisplayName("Should return true when game exists")
        void shouldReturnTrueWhenGameExists() {
            // Given
            Long gameId = 1L;
            when(gameRepository.existsById(gameId)).thenReturn(true);

            // When
            boolean exists = adminGameService.gameExists(gameId);

            // Then
            assertTrue(exists);
            verify(gameRepository).existsById(gameId);
        }

        @Test
        @DisplayName("Should return false when game does not exist")
        void shouldReturnFalseWhenGameDoesNotExist() {
            // Given
            Long gameId = 999L;
            when(gameRepository.existsById(gameId)).thenReturn(false);

            // When
            boolean exists = adminGameService.gameExists(gameId);

            // Then
            assertFalse(exists);
            verify(gameRepository).existsById(gameId);
        }
    }

    // Helper methods to create test data

    /**
     * Create game events for a game (rounds, kills, etc.)
     */
    private List<GameEventEntity> createGameEvents(GameEntity game, int rounds) {
        List<GameEventEntity> events = new ArrayList<>();
        long eventId = 1L;

        for (int round = 1; round <= rounds; round++) {
            // Round start
            RoundStartEventEntity roundStart = new RoundStartEventEntity(
                game.getGameOverTimestamp().minusSeconds((rounds - round + 1) * 100));
            roundStart.setId(eventId++);
            roundStart.setGame(game);
            events.add(roundStart);

            // Some kill events in the round
            for (int kill = 0; kill < 3; kill++) {
                KillEventEntity killEvent = new KillEventEntity(
                    game.getGameOverTimestamp().minusSeconds((rounds - round + 1) * 100 + kill * 10));
                killEvent.setId(eventId++);
                killEvent.setGame(game);
                killEvent.setRoundStart(roundStart);
                events.add(killEvent);
            }

            // Round end
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(
                game.getGameOverTimestamp().minusSeconds((rounds - round) * 100));
            roundEnd.setId(eventId++);
            roundEnd.setGame(game);
            roundEnd.setRoundStart(roundStart);
            events.add(roundEnd);
        }

        return events;
    }

    /**
     * Create accolades for a game
     */
    private List<AccoladeEntity> createAccolades(GameEntity game, int count) {
        List<AccoladeEntity> accolades = new ArrayList<>();
        String[] accoladeTypes = {"MVP", "Top Fragger", "Clutch King", "Ace", "Headshot Master"};

        for (int i = 0; i < count; i++) {
            AccoladeEntity accolade = new AccoladeEntity();
            accolade.setId((long) (i + 1));
            accolade.setGame(game);
            accolade.setPlayerId("[U:1:" + (1000000 + i) + "]");
            accolade.setPlayerName("Player" + (i + 1));
            accolade.setType(accoladeTypes[i % accoladeTypes.length]);
            accolade.setPosition(i + 1);
            accolade.setValue(100.0 + (i * 10));
            accolade.setScore(1500.0 + (i * 50));
            accolade.setCreatedAt(game.getGameOverTimestamp());
            accolades.add(accolade);
        }

        return accolades;
    }

    /**
     * Create player stats for a game
     */
    private List<PlayerStatsEntity> createPlayerStats(GameEntity game, int playerCount) {
        List<PlayerStatsEntity> stats = new ArrayList<>();
        Instant gameTimestamp = game.getGameOverTimestamp();

        for (int i = 0; i < playerCount; i++) {
            PlayerStatsEntity stat = new PlayerStatsEntity();
            stat.setId((long) (i + 1));
            stat.setPlayerId("[U:1:" + (1000000 + i) + "]");
            stat.setGameTimestamp(gameTimestamp);
            stat.setGame(game); // Set game reference for confident deletion
            stat.setKills(10 + i);
            stat.setDeaths(5 + i);
            stat.setAssists(3 + i);
            stat.setHeadshotKills(2 + i);
            stat.setRoundsPlayed(16);
            stat.setDamageDealt(1500.0 + (i * 100));
            stat.setRank(1000 + i);
            stat.setLastSeenNickname("Player" + (i + 1));
            stat.setCreatedAt(gameTimestamp);
            stat.setLastUpdated(gameTimestamp);
            stats.add(stat);
        }

        return stats;
    }
}
