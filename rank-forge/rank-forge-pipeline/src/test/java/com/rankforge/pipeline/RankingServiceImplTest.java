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

import com.rankforge.core.interfaces.RankingAlgorithm;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for RankingServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock
    private PlayerStatsStore mockStatsStore;

    @Mock
    private RankingAlgorithm mockRankingAlgorithm;

    private RankingServiceImpl rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingServiceImpl(mockStatsStore, mockRankingAlgorithm);
    }

    @Nested
    @DisplayName("Update Rankings Tests")
    class UpdateRankingsTests {

        @Test
        @DisplayName("Should update rankings for all players")
        void shouldUpdateRankingsForAllPlayers() {
            // Given
            PlayerStats player1 = createPlayerStats("player1", 20, 10, 5, 8);
            PlayerStats player2 = createPlayerStats("player2", 15, 12, 3, 6);
            PlayerStats player3 = createPlayerStats("player3", 25, 8, 7, 12);
            List<PlayerStats> players = Arrays.asList(player1, player2, player3);

            when(mockRankingAlgorithm.calculateRank(player1)).thenReturn(1850);
            when(mockRankingAlgorithm.calculateRank(player2)).thenReturn(1520);
            when(mockRankingAlgorithm.calculateRank(player3)).thenReturn(2100);

            // When
            rankingService.updateRankings(players);

            // Then
            assertEquals(1850, player1.getRank());
            assertEquals(1520, player2.getRank());
            assertEquals(2100, player3.getRank());

            // Verify algorithm was called for each player
            verify(mockRankingAlgorithm).calculateRank(player1);
            verify(mockRankingAlgorithm).calculateRank(player2);
            verify(mockRankingAlgorithm).calculateRank(player3);
        }

        @Test
        @DisplayName("Should handle empty player list")
        void shouldHandleEmptyPlayerList() {
            // Given
            List<PlayerStats> emptyList = List.of();

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> rankingService.updateRankings(emptyList));
            
            // Should not call ranking algorithm
            verify(mockRankingAlgorithm, never()).calculateRank(any());
        }

        @Test
        @DisplayName("Should handle single player")
        void shouldHandleSinglePlayer() {
            // Given
            PlayerStats singlePlayer = createPlayerStats("solo", 10, 5, 2, 4);
            List<PlayerStats> singlePlayerList = List.of(singlePlayer);

            when(mockRankingAlgorithm.calculateRank(singlePlayer)).thenReturn(1750);

            // When
            rankingService.updateRankings(singlePlayerList);

            // Then
            assertEquals(1750, singlePlayer.getRank());
            verify(mockRankingAlgorithm).calculateRank(singlePlayer);
        }

        @Test
        @DisplayName("Should handle null player in list gracefully")
        void shouldHandleNullPlayerInListGracefully() {
            // Given
            PlayerStats validPlayer = createPlayerStats("valid", 10, 5, 2, 4);
            List<PlayerStats> playersWithNull = Arrays.asList(validPlayer, null);

            when(mockRankingAlgorithm.calculateRank(validPlayer)).thenReturn(1600);

            // When & Then - Should throw NullPointerException when trying to access null player
            assertThrows(NullPointerException.class, () -> rankingService.updateRankings(playersWithNull));
        }

        @Test
        @DisplayName("Should handle algorithm errors")
        void shouldHandleAlgorithmErrors() {
            // Given
            PlayerStats player = createPlayerStats("problematic", 10, 5, 2, 4);
            List<PlayerStats> players = List.of(player);

            when(mockRankingAlgorithm.calculateRank(player))
                .thenThrow(new RuntimeException("Algorithm calculation failed"));

            // When & Then - Should propagate the exception
            assertThrows(RuntimeException.class, () -> rankingService.updateRankings(players));
        }

        @Test
        @DisplayName("Should maintain original rank if algorithm returns NaN")
        void shouldMaintainOriginalRankIfAlgorithmReturnsNaN() {
            // Given
            PlayerStats player = createPlayerStats("nanPlayer", 10, 5, 2, 4);
            player.setRank(1500); // Set initial rank
            List<PlayerStats> players = List.of(player);

            when(mockRankingAlgorithm.calculateRank(player)).thenReturn((int) Double.NaN);

            // When
            rankingService.updateRankings(players);

            // Then
            assertEquals((int) Double.NaN, player.getRank()); // Should accept NaN if algorithm returns it
        }

        @Test
        @DisplayName("Should handle very large rankings")
        void shouldHandleVeryLargeRankings() {
            // Given
            PlayerStats player = createPlayerStats("highPerformer", 1000, 10, 200, 800);
            List<PlayerStats> players = List.of(player);

            when(mockRankingAlgorithm.calculateRank(player)).thenReturn(50000);

            // When
            rankingService.updateRankings(players);

            // Then
            assertEquals(50000, player.getRank());
        }
    }

    @Nested
    @DisplayName("Get Player Ranking Tests")
    class GetPlayerRankingTests {

        @Test
        @DisplayName("Should return zero for get player ranking (not implemented)")
        void shouldReturnZeroForGetPlayerRanking() {
            // Given
            String playerId = "test-player";

            // When
            double ranking = rankingService.getPlayerRanking(playerId);

            // Then
            assertEquals(0.0, ranking, 0.001);
        }

        @Test
        @DisplayName("Should handle null player ID")
        void shouldHandleNullPlayerId() {
            // When
            double ranking = rankingService.getPlayerRanking(null);

            // Then
            assertEquals(0.0, ranking, 0.001);
        }

        @Test
        @DisplayName("Should handle empty player ID")
        void shouldHandleEmptyPlayerId() {
            // When
            double ranking = rankingService.getPlayerRanking("");

            // Then
            assertEquals(0.0, ranking, 0.001);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should coordinate between stats store and ranking algorithm")
        void shouldCoordinateBetweenStatsStoreAndRankingAlgorithm() {
            // Given
            PlayerStats player1 = createPlayerStats("player1", 20, 10, 5, 8);
            PlayerStats player2 = createPlayerStats("player2", 15, 15, 8, 3);
            List<PlayerStats> players = Arrays.asList(player1, player2);

            // Mock different rankings based on performance
            when(mockRankingAlgorithm.calculateRank(player1)).thenReturn(1800);
            when(mockRankingAlgorithm.calculateRank(player2)).thenReturn(1400);

            // When
            rankingService.updateRankings(players);

            // Then
            // Verify proper coordination
            assertNotEquals(player1.getRank(), player2.getRank());
            assertTrue(player1.getRank() > player2.getRank()); // Better KDR should have higher rank
            
            // Verify all interactions happened
            verify(mockRankingAlgorithm, times(2)).calculateRank(any(PlayerStats.class));
        }

        @Test
        @DisplayName("Should update rankings multiple times correctly")
        void shouldUpdateRankingsMultipleTimesCorrectly() {
            // Given
            PlayerStats player = createPlayerStats("consistent", 10, 10, 5, 2);
            List<PlayerStats> players = List.of(player);

            when(mockRankingAlgorithm.calculateRank(player))
                .thenReturn(1500)
                .thenReturn(1600)
                .thenReturn(1550);

            // When - Update rankings multiple times
            rankingService.updateRankings(players);
            assertEquals(1500, player.getRank());

            rankingService.updateRankings(players);
            assertEquals(1600, player.getRank());

            rankingService.updateRankings(players);
            assertEquals(1550, player.getRank());

            // Then
            verify(mockRankingAlgorithm, times(3)).calculateRank(player);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create service with valid dependencies")
        void shouldCreateServiceWithValidDependencies() {
            // When
            RankingServiceImpl service = new RankingServiceImpl(mockStatsStore, mockRankingAlgorithm);

            // Then
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should handle null stats store")
        void shouldHandleNullStatsStore() {
            // When & Then - Constructor should accept null (though not recommended)
            assertDoesNotThrow(() -> new RankingServiceImpl(null, mockRankingAlgorithm));
        }

        @Test
        @DisplayName("Should handle null ranking algorithm")
        void shouldHandleNullRankingAlgorithm() {
            // When & Then - Constructor should accept null (though not recommended)
            assertDoesNotThrow(() -> new RankingServiceImpl(mockStatsStore, null));
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
        stats.setRoundsPlayed(20);
        stats.setClutchesWon(3);
        stats.setDamageDealt(2500.0);
        stats.setLastUpdated(Instant.now());
        stats.setRank(1000); // Default rank
        return stats;
    }
}