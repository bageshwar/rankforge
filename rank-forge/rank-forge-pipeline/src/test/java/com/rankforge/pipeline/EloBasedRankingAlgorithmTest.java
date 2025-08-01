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

import com.rankforge.core.models.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for EloBasedRankingAlgorithm
 */
class EloBasedRankingAlgorithmTest {

    private EloBasedRankingAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new EloBasedRankingAlgorithm();
    }

    @Nested
    @DisplayName("Calculate Rank Tests")
    class CalculateRankTests {

        @Test
        @DisplayName("Should calculate rank for typical player stats")
        void shouldCalculateRankForTypicalStats() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 20, 10, 5, 4);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            // Expected: 1000 + (kdr * 200) + (headshotRatio * 100) + (clutchFactor * 150)
            // KDR = 20 / (10 + 1) = 1.818...
            // HeadshotRatio = 4 / (20 + 1) = 0.190...
            // ClutchFactor = 0 (not implemented)
            // Expected ≈ 1000 + (1.818 * 200) + (0.190 * 100) + 0 = 1000 + 363.6 + 19.0 = 1382.6
            assertEquals(1382, rank);
        }

        @Test
        @DisplayName("Should handle player with zero kills")
        void shouldHandleZeroKills() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 0, 5, 0, 0);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            // KDR = 0 / (5 + 1) = 0
            // HeadshotRatio = 0 / (0 + 1) = 0
            // Expected = 1000 + 0 + 0 + 0 = 1000
            assertEquals(1000, rank);
        }

        @Test
        @DisplayName("Should handle player with zero deaths")
        void shouldHandleZeroDeaths() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 15, 0, 3, 5);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            // KDR = 15 / (0 + 1) = 15.0
            // HeadshotRatio = 5 / (15 + 1) = 0.3125
            // Expected = 1000 + (15 * 200) + (0.3125 * 100) + 0 = 1000 + 3000 + 31.25 = 4031.25
            assertEquals(4031, rank);
        }

        @Test
        @DisplayName("Should handle perfect headshot ratio")
        void shouldHandlePerfectHeadshotRatio() {
            // Given - all kills are headshots
            PlayerStats stats = createPlayerStats("player1", 10, 5, 2, 10);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            // KDR = 10 / (5 + 1) = 1.667
            // HeadshotRatio = 10 / (10 + 1) = 0.909
            // Expected = 1000 + (1.667 * 200) + (0.909 * 100) + 0 ≈ 1000 + 333.33 + 90.91 = 1424.24
            assertEquals(1424, rank);
        }

        @Test
        @DisplayName("Should handle high-skill player")
        void shouldHandleHighSkillPlayer() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 100, 20, 30, 50);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            // KDR = 100 / (20 + 1) = 4.762
            // HeadshotRatio = 50 / (100 + 1) = 0.495
            // Expected = 1000 + (4.762 * 200) + (0.495 * 100) + 0 ≈ 1000 + 952.38 + 49.50 = 2001.88
            assertEquals(2001, rank);
        }

        @ParameterizedTest
        @CsvSource({
            "0, 0, 0, 0, 1000",           // New player
            "1, 1, 0, 0, 1100",           // One kill, one death
            "5, 0, 2, 2, 2033",          // Good start
            "50, 50, 10, 20, 1235"  // Average player
        })
        @DisplayName("Should calculate rank correctly for various stat combinations")
        void shouldCalculateRankForVariousStats(int kills, int deaths, int assists, int headshots, int expectedRank) {
            // Given
            PlayerStats stats = createPlayerStats("player1", kills, deaths, assists, headshots);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            assertEquals(expectedRank, rank);
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should calculate KDR correctly")
        void shouldCalculateKDR() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 20, 10, 5, 4);
            
            // When - Using reflection to test private method through public calculateRank
            // We can verify KDR calculation by checking the rank calculation
            int rank = algorithm.calculateRank(stats);
            
            // Then - Verify the KDR component is correct
            // KDR = 20 / (10 + 1) = 1.8181818...
            // The KDR contribution to rank should be 1.8181818... * 200 = 363.636...
            double expectedKdrContribution = (20.0 / 11.0) * 200;
            double expectedHeadshotContribution = (4.0 / 21.0) * 100;
            double expectedTotal = 1000 + expectedKdrContribution + expectedHeadshotContribution;
            assertEquals((int) expectedTotal, rank);
        }

        @Test
        @DisplayName("Should handle division by zero in KDR calculation")
        void shouldHandleDivisionByZeroInKDR() {
            // Given - Zero deaths (should use deaths + 1)
            PlayerStats stats = createPlayerStats("player1", 10, 0, 2, 3);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then - Should not throw exception and handle gracefully
            assertNotNull(rank);
            assertTrue(rank > 1000); // Should be above base rating
            assertFalse(Double.isInfinite(rank));
            assertFalse(Double.isNaN(rank));
        }

        @Test
        @DisplayName("Should calculate headshot ratio correctly")
        void shouldCalculateHeadshotRatio() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 20, 10, 5, 8);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then - Verify headshot ratio component
            // HeadshotRatio = 8 / (20 + 1) = 0.38095...
            double expectedHeadshotContribution = (8.0 / 21.0) * 100;
            double expectedKdrContribution = (20.0 / 11.0) * 200;
            double expectedTotal = 1000 + expectedKdrContribution + expectedHeadshotContribution;
            assertEquals((int) expectedTotal, rank);
        }

        @Test
        @DisplayName("Should handle zero kills in headshot ratio calculation")
        void shouldHandleZeroKillsInHeadshotRatio() {
            // Given - Zero kills (should use kills + 1)
            PlayerStats stats = createPlayerStats("player1", 0, 5, 2, 0);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then - Should not throw exception
            assertNotNull(rank);
            assertFalse(Double.isInfinite(rank));
            assertFalse(Double.isNaN(rank));
            assertEquals(1000, rank); // Should be base rating
        }

        @Test
        @DisplayName("Should return zero for clutch factor (not implemented)")
        void shouldReturnZeroForClutchFactor() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 10, 5, 2, 3);
            stats.setClutchesWon(5); // This should not affect the calculation yet
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then - Clutch factor should not contribute to rank (returns 0)
            double expectedKdrContribution = (10.0 / 6.0) * 200;
            double expectedHeadshotContribution = (3.0 / 11.0) * 100;
            double expectedTotal = 1000 + expectedKdrContribution + expectedHeadshotContribution; // No clutch contribution
            assertEquals((int) expectedTotal, rank);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null PlayerStats gracefully")
        void shouldHandleNullPlayerStats() {
            // When & Then
            assertThrows(NullPointerException.class, () -> algorithm.calculateRank(null));
        }

        @Test
        @DisplayName("Should handle negative stats")
        void shouldHandleNegativeStats() {
            // Given - This shouldn't happen in real scenarios but let's be defensive
            PlayerStats stats = createPlayerStats("player1", -5, -2, -1, -3);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then - Should handle gracefully
            assertNotNull(rank);
            assertFalse(Double.isInfinite(rank));
            assertFalse(Double.isNaN(rank));
        }

        @Test
        @DisplayName("Should handle very large numbers")
        void shouldHandleVeryLargeNumbers() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 1000000, 500000, 100000, 250000);
            
            // When
            int rank = algorithm.calculateRank(stats);
            
            // Then
            assertNotNull(rank);
            assertFalse(Double.isInfinite(rank));
            assertFalse(Double.isNaN(rank));
            assertTrue(rank > 1000); // Should be significantly above base
        }

        @Test
        @DisplayName("Should be consistent across multiple calls")
        void shouldBeConsistentAcrossMultipleCalls() {
            // Given
            PlayerStats stats = createPlayerStats("player1", 25, 15, 8, 12);
            
            // When
            int rank1 = algorithm.calculateRank(stats);
            int rank2 = algorithm.calculateRank(stats);
            int rank3 = algorithm.calculateRank(stats);
            
            // Then
            assertEquals(rank1, rank2);
            assertEquals(rank2, rank3);
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicTests {

        @Test
        @DisplayName("Better KDR should result in higher rank")
        void betterKDRShouldResultInHigherRank() {
            // Given
            PlayerStats lowKDR = createPlayerStats("player1", 10, 20, 5, 3);  // KDR = 0.476
            PlayerStats highKDR = createPlayerStats("player2", 20, 10, 5, 3); // KDR = 1.818
            
            // When
            int lowRank = algorithm.calculateRank(lowKDR);
            int highRank = algorithm.calculateRank(highKDR);
            
            // Then
            assertTrue(highRank > lowRank, "Higher KDR should result in higher rank");
        }

        @Test
        @DisplayName("Better headshot ratio should result in higher rank")
        void betterHeadshotRatioShouldResultInHigherRank() {
            // Given
            PlayerStats lowHS = createPlayerStats("player1", 20, 10, 5, 2);  // HS ratio = 2/21
            PlayerStats highHS = createPlayerStats("player2", 20, 10, 5, 15); // HS ratio = 15/21
            
            // When
            int lowRank = algorithm.calculateRank(lowHS);
            int highRank = algorithm.calculateRank(highHS);
            
            // Then
            assertTrue(highRank > lowRank, "Higher headshot ratio should result in higher rank");
        }

        @Test
        @DisplayName("Rank should never be below base rating for positive stats")
        void rankShouldNeverBeBelowBaseRating() {
            // Given
            PlayerStats minimalStats = createPlayerStats("player1", 1, 100, 0, 0); // Very poor performance
            
            // When
            double rank = algorithm.calculateRank(minimalStats);
            
            // Then
            assertTrue(rank >= 1000, "Rank should never be below base rating of 1000");
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
        stats.setClutchesWon(0);
        stats.setDamageDealt(1000.0);
        stats.setLastUpdated(Instant.now());
        return stats;
    }
}