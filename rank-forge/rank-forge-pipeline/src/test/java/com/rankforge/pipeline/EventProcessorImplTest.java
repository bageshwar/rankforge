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

package com.rankforge.pipeline;

import com.rankforge.core.events.*;
import com.rankforge.core.interfaces.RankingService;
import com.rankforge.core.models.Player;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for EventProcessorImpl
 */
@Disabled
@ExtendWith(MockitoExtension.class)
class EventProcessorImplTest {

    @Mock
    private PlayerStatsStore mockStatsStore;

    @Mock
    private RankingService mockRankingService;

    private EventProcessorImpl eventProcessor;

    @BeforeEach
    void setUp() {
        eventProcessor = new EventProcessorImpl(mockStatsStore, mockRankingService);
    }

    @Nested
    @DisplayName("Kill Event Processing Tests")
    class KillEventTests {

        @Test
        @DisplayName("Should process kill event and update player stats")
        void shouldProcessKillEventAndUpdateStats() {
            // Given
            Player killer = new Player("Killer", "[U:1:123456]");
            Player victim = new Player("Victim", "[U:1:789012]");
            KillEvent killEvent = new KillEvent(
                Instant.now(), Map.of(), killer, victim, "ak47", false
            );

            PlayerStats killerStats = createPlayerStats(killer.getSteamId(), 10, 5, 3, 2);
            PlayerStats victimStats = createPlayerStats(victim.getSteamId(), 8, 7, 2, 1);

            when(mockStatsStore.getPlayerStats(killer.getSteamId()))
                .thenReturn(Optional.of(killerStats));
            when(mockStatsStore.getPlayerStats(victim.getSteamId()))
                .thenReturn(Optional.of(victimStats));

            // When
            eventProcessor.processEvent(killEvent);

            // Then
            assertEquals(11, killerStats.getKills()); // Should increment kills
            assertEquals(8, victimStats.getDeaths());  // Should increment deaths

            // Verify stats were stored
            verify(mockStatsStore).store(killerStats, false);
            verify(mockStatsStore).store(victimStats, false);
        }

        @Test
        @DisplayName("Should process headshot kill event")
        void shouldProcessHeadshotKillEvent() {
            // Given
            Player killer = new Player("Sniper", "[U:1:123456]");
            Player victim = new Player("Target", "[U:1:789012]");
            KillEvent headshotKill = new KillEvent(
                Instant.now(), Map.of(), killer, victim, "awp", true
            );

            PlayerStats killerStats = createPlayerStats(killer.getSteamId(), 15, 3, 2, 8);

            when(mockStatsStore.getPlayerStats(killer.getSteamId()))
                .thenReturn(Optional.of(killerStats));
            when(mockStatsStore.getPlayerStats(victim.getSteamId()))
                .thenReturn(Optional.of(createPlayerStats(victim.getSteamId(), 5, 10, 1, 0)));

            // When
            eventProcessor.processEvent(headshotKill);

            // Then
            assertEquals(16, killerStats.getKills());
            assertEquals(9, killerStats.getHeadshotKills()); // Should increment headshot kills
        }

        @Test
        @DisplayName("Should create default stats for new players")
        void shouldCreateDefaultStatsForNewPlayers() {
            // Given
            Player newKiller = new Player("NewPlayer", "[U:1:999888]");
            Player newVictim = new Player("AnotherNew", "[U:1:777666]");
            KillEvent killEvent = new KillEvent(
                Instant.now(), Map.of(), newKiller, newVictim, "glock", false
            );

            when(mockStatsStore.getPlayerStats(newKiller.getSteamId()))
                .thenReturn(Optional.empty());
            when(mockStatsStore.getPlayerStats(newVictim.getSteamId()))
                .thenReturn(Optional.empty());

            // When
            eventProcessor.processEvent(killEvent);

            // Then
            verify(mockStatsStore, times(2)).store(any(PlayerStats.class), eq(false));
        }
    }

    @Nested
    @DisplayName("Assist Event Processing Tests")
    class AssistEventTests {

        @Test
        @DisplayName("Should process assist event and increment assists")
        void shouldProcessAssistEventAndIncrementAssists() {
            // Given
            Player assister = new Player("Helper", "[U:1:123456]");
            Player victim = new Player("Victim", "[U:1:789012]");
            AssistEvent assistEvent = new AssistEvent(
                Instant.now(), Map.of(), assister, victim, "ak47", AssistEvent.AssistType.Regular
            );

            PlayerStats assisterStats = createPlayerStats(assister.getSteamId(), 5, 3, 2, 1);

            when(mockStatsStore.getPlayerStats(assister.getSteamId()))
                .thenReturn(Optional.of(assisterStats));
            when(mockStatsStore.getPlayerStats(victim.getSteamId()))
                .thenReturn(Optional.of(createPlayerStats(victim.getSteamId(), 8, 7, 1, 0)));

            // When
            eventProcessor.processEvent(assistEvent);

            // Then
            assertEquals(3, assisterStats.getAssists()); // Should increment assists
            verify(mockStatsStore).store(assisterStats, false);
        }
    }

    @Nested
    @DisplayName("Attack Event Processing Tests")
    class AttackEventTests {

        @Test
        @DisplayName("Should process attack event and add damage")
        void shouldProcessAttackEventAndAddDamage() {
            // Given
            Player attacker = new Player("Attacker", "[U:1:123456]");
            Player victim = new Player("Victim", "[U:1:789012]");
            AttackEvent attackEvent = new AttackEvent(
                Instant.now(), Map.of(), attacker, victim, "ak47", "25", "5", "chest"
            );

            PlayerStats attackerStats = createPlayerStats(attacker.getSteamId(), 10, 5, 2, 3);
            attackerStats.setDamageDealt(1000.0);

            when(mockStatsStore.getPlayerStats(attacker.getSteamId()))
                .thenReturn(Optional.of(attackerStats));
            when(mockStatsStore.getPlayerStats(victim.getSteamId()))
                .thenReturn(Optional.of(createPlayerStats(victim.getSteamId(), 8, 7, 1, 0)));

            // When
            eventProcessor.processEvent(attackEvent);

            // Then
            assertEquals(1025.0, attackerStats.getDamageDealt(), 0.1); // Should add damage
            verify(mockStatsStore).store(attackerStats, false);
        }
    }

    @Nested
    @DisplayName("Round End Event Processing Tests")
    class RoundEndEventTests {

        @Test
        @DisplayName("Should process round end event and update rankings")
        void shouldProcessRoundEndEventAndUpdateRankings() {
            // Given
            List<String> playerIds = List.of("123456", "789012", "555666");
            RoundEndEvent roundEndEvent = new RoundEndEvent(Instant.now(), Map.of());
            roundEndEvent.setPlayers(playerIds);

            PlayerStats player1 = createPlayerStats("[U:1:123456]", 15, 5, 3, 8);
            PlayerStats player2 = createPlayerStats("[U:1:789012]", 12, 8, 5, 4);
            PlayerStats player3 = createPlayerStats("[U:1:555666]", 8, 10, 2, 2);

            when(mockStatsStore.getPlayerStats("[U:1:123456]"))
                .thenReturn(Optional.of(player1));
            when(mockStatsStore.getPlayerStats("[U:1:789012]"))
                .thenReturn(Optional.of(player2));
            when(mockStatsStore.getPlayerStats("[U:1:555666]"))
                .thenReturn(Optional.of(player3));

            // When
            eventProcessor.processEvent(roundEndEvent);

            // Then
            // Verify rounds played were incremented
            assertEquals(11, player1.getRoundsPlayed());
            assertEquals(11, player2.getRoundsPlayed());
            assertEquals(11, player3.getRoundsPlayed());

            // Verify ranking service was called
            verify(mockRankingService).updateRankings(anyList());

            // Verify all players were stored with updateRank=true
            verify(mockStatsStore).store(player1, true);
            verify(mockStatsStore).store(player2, true);
            verify(mockStatsStore).store(player3, true);
        }

        @Test
        @DisplayName("Should filter out bot players (ID '0')")
        void shouldFilterOutBotPlayers() {
            // Given
            List<String> playerIds = List.of("123456", "0", "789012"); // "0" is bot
            RoundEndEvent roundEndEvent = new RoundEndEvent(Instant.now(), Map.of());
            roundEndEvent.setPlayers(playerIds);

            PlayerStats player1 = createPlayerStats("[U:1:123456]", 15, 5, 3, 8);
            PlayerStats player2 = createPlayerStats("[U:1:789012]", 12, 8, 5, 4);

            when(mockStatsStore.getPlayerStats("[U:1:123456]"))
                .thenReturn(Optional.of(player1));
            when(mockStatsStore.getPlayerStats("[U:1:789012]"))
                .thenReturn(Optional.of(player2));

            // When
            eventProcessor.processEvent(roundEndEvent);

            // Then
            // Should not try to get stats for bot (ID "0")
            verify(mockStatsStore, never()).getPlayerStats("[U:1:0]");
            
            // Should only process non-bot players
            verify(mockStatsStore).store(player1, true);
            verify(mockStatsStore).store(player2, true);
        }

        @Test
        @DisplayName("Should handle missing player stats gracefully")
        void shouldHandleMissingPlayerStatsGracefully() {
            // Given
            List<String> playerIds = List.of("123456", "789012");
            RoundEndEvent roundEndEvent = new RoundEndEvent(Instant.now(), Map.of());
            roundEndEvent.setPlayers(playerIds);

            when(mockStatsStore.getPlayerStats("[U:1:123456]"))
                .thenReturn(Optional.empty()); // Player not found
            when(mockStatsStore.getPlayerStats("[U:1:789012]"))
                .thenReturn(Optional.empty()); // Player not found

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> eventProcessor.processEvent(roundEndEvent));
            
            // Should still call ranking service with empty list
            verify(mockRankingService).updateRankings(anyList());
        }
    }

    @Nested
    @DisplayName("Game Event Type Handling Tests")
    class GameEventTypeTests {

        @Test
        @DisplayName("Should handle BombEvent")
        void shouldHandleBombEvent() {
            // Given
            BombEvent bombEvent = new BombEvent(Instant.now(), Map.of(), 
                "Planter", BombEvent.BombEventType.PLANT, 0);

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> eventProcessor.processEvent(bombEvent));
        }

        @Test
        @DisplayName("Should handle RoundStartEvent")
        void shouldHandleRoundStartEvent() {
            // Given
            RoundStartEvent roundStartEvent = new RoundStartEvent(Instant.now(), Map.of());

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> eventProcessor.processEvent(roundStartEvent));
        }

        @Test
        @DisplayName("Should handle GameOverEvent")
        void shouldHandleGameOverEvent() {
            // Given
            GameOverEvent gameOverEvent = new GameOverEvent(Instant.now(), Map.of(), 
                "de_dust2", "competitive", 16, 14);

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> eventProcessor.processEvent(gameOverEvent));
        }

        @Test
        @DisplayName("Should handle GameProcessedEvent")
        void shouldHandleGameProcessedEvent() {
            // Given
            GameProcessedEvent gameProcessedEvent = new GameProcessedEvent(Instant.now(), Map.of());

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> eventProcessor.processEvent(gameProcessedEvent));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null game event gracefully")
        void shouldHandleNullGameEventGracefully() {
            // When & Then
            assertThrows(NullPointerException.class, () -> eventProcessor.processEvent(null));
        }

        @Test
        @DisplayName("Should handle unknown event type")
        void shouldHandleUnknownEventType() {
            // Given - Create a mock event with unknown type
            GameEvent unknownEvent = mock(GameEvent.class);
            when(unknownEvent.getGameEventType()).thenReturn(null);

            // When & Then
            assertThrows(IllegalStateException.class, () -> eventProcessor.processEvent(unknownEvent));
        }

        @Test
        @DisplayName("Should handle stats store errors gracefully")
        void shouldHandleStatsStoreErrorsGracefully() {
            // Given
            Player killer = new Player("Killer", "[U:1:123456]");
            Player victim = new Player("Victim", "[U:1:789012]");
            KillEvent killEvent = new KillEvent(
                Instant.now(), Map.of(), killer, victim, "ak47", false
            );

            when(mockStatsStore.getPlayerStats(anyString()))
                .thenThrow(new RuntimeException("Database error"));

            // When & Then - Should propagate the exception
            assertThrows(RuntimeException.class, () -> eventProcessor.processEvent(killEvent));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should process multiple events in sequence")
        void shouldProcessMultipleEventsInSequence() {
            // Given
            Player player1 = new Player("Player1", "[U:1:123456]");
            Player player2 = new Player("Player2", "[U:1:789012]");
            
            KillEvent kill = new KillEvent(Instant.now(), Map.of(), player1, player2, "ak47", true);
            AssistEvent assist = new AssistEvent(Instant.now(), Map.of(), player2, player1, "m4a1", AssistEvent.AssistType.Regular);
            AttackEvent attack = new AttackEvent(Instant.now(), Map.of(), player1, player2, "m4a1", "30", "0", "chest");

            PlayerStats stats1 = createPlayerStats(player1.getSteamId(), 10, 5, 2, 3);
            PlayerStats stats2 = createPlayerStats(player2.getSteamId(), 8, 7, 3, 1);

            when(mockStatsStore.getPlayerStats(player1.getSteamId()))
                .thenReturn(Optional.of(stats1));
            when(mockStatsStore.getPlayerStats(player2.getSteamId()))
                .thenReturn(Optional.of(stats2));

            // When
            eventProcessor.processEvent(kill);
            eventProcessor.processEvent(assist);
            eventProcessor.processEvent(attack);

            // Then
            assertEquals(11, stats1.getKills());      // From kill event
            assertEquals(4, stats1.getHeadshotKills()); // From headshot kill
            assertEquals(1030.0, stats1.getDamageDealt(), 0.1); // From attack event
            assertEquals(8, stats2.getDeaths());      // From kill event
            assertEquals(4, stats2.getAssists());     // From assist event

            // Verify all store calls
            verify(mockStatsStore, times(5)).store(any(PlayerStats.class), eq(false));
        }
    }

    /**
     * Helper method to create PlayerStats for testing
     */
    private PlayerStats createPlayerStats(String playerId, int kills, int deaths, int assists, int headshotKills) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(playerId);
        stats.setKills(kills);
        stats.setDeaths(deaths);
        stats.setAssists(assists);
        stats.setHeadshotKills(headshotKills);
        stats.setRoundsPlayed(10);
        stats.setClutchesWon(2);
        stats.setDamageDealt(1000.0);
        stats.setLastUpdated(Instant.now());
        return stats;
    }
}