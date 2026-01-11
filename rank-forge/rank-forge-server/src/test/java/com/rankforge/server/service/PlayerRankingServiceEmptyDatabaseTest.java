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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.LeaderboardResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayerRankingService to ensure it handles empty database scenarios gracefully.
 * Tests cover cases where database tables don't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class PlayerRankingServiceEmptyDatabaseTest {

    @Mock
    private PlayerStatsRepository playerStatsRepository;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private GameEventRepository gameEventRepository;
    
    @Mock
    private ObjectMapper objectMapper;

    private PlayerRankingService playerRankingService;

    @BeforeEach
    void setUp() {
        playerRankingService = new PlayerRankingService(playerStatsRepository, gameRepository, gameEventRepository, objectMapper);
    }

    @Test
    void testGetAllPlayerRankings_WhenPlayerStatsTableDoesNotExist_ReturnsEmptyList() {
        // Mock empty repository result (simulating empty database)
        when(playerStatsRepository.findLatestStatsForAllPlayers()).thenReturn(Collections.emptyList());

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getAllPlayerRankings();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when database is empty");
        });
    }

    @Test
    void testGetAllPlayerRankings_WhenRepositoryThrowsException_ReturnsEmptyList() {
        // Mock repository throwing exception
        when(playerStatsRepository.findLatestStatsForAllPlayers())
                .thenThrow(new RuntimeException("Database connection error"));

        // Should catch exception and return empty list
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getAllPlayerRankings();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list on any exception");
        });
    }

    @Test
    void testGetTopPlayerRankings_WhenDatabaseIsEmpty_ReturnsEmptyList() {
        // Mock empty repository result
        when(playerStatsRepository.findLatestStatsForAllPlayers()).thenReturn(Collections.emptyList());

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getTopPlayerRankings(10);
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when database is empty");
        });
    }

    @Test
    void testGetAllPlayerRankings_WhenOtherExceptionOccurs_ReturnsEmptyList() {
        // Mock a different exception (not empty result)
        when(playerStatsRepository.findLatestStatsForAllPlayers())
                .thenThrow(new RuntimeException("Connection timeout"));

        // Should catch exception and return empty list
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getAllPlayerRankings();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list on any exception");
        });
    }

    @Test
    void testGetPlayerRanking_WhenCalled_ReturnsEmptyOptional() {
        // This method doesn't query database yet, but test it returns empty
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getPlayerRanking("some-player-id");
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty optional");
        });
    }
    
    @Test
    void testGetAllPlayerRankingsWithStats_WhenDatabaseIsEmpty_ReturnsEmptyResponse() {
        // Mock empty repository results
        when(playerStatsRepository.findLatestStatsForAllPlayers()).thenReturn(Collections.emptyList());
        when(playerStatsRepository.countTotalDistinctGames()).thenReturn(0L);
        when(gameRepository.calculateTotalRounds()).thenReturn(0L);
        
        // Should return empty response without throwing exception
        assertDoesNotThrow(() -> {
            LeaderboardResponseDTO result = playerRankingService.getAllPlayerRankingsWithStats();
            assertNotNull(result);
            assertNotNull(result.getRankings());
            assertTrue(result.getRankings().isEmpty());
            assertEquals(0, result.getTotalGames());
            assertEquals(0, result.getTotalRounds());
            assertEquals(0, result.getTotalPlayers());
        });
    }
    
    @Test
    void testGetTopPlayerRankingsWithStats_WhenDatabaseIsEmpty_ReturnsEmptyResponse() {
        // Mock empty repository results
        when(playerStatsRepository.findLatestStatsForAllPlayers()).thenReturn(Collections.emptyList());
        when(playerStatsRepository.countTotalDistinctGames()).thenReturn(0L);
        when(gameRepository.calculateTotalRounds()).thenReturn(0L);
        
        // Should return empty response without throwing exception
        assertDoesNotThrow(() -> {
            LeaderboardResponseDTO result = playerRankingService.getTopPlayerRankingsWithStats(10);
            assertNotNull(result);
            assertNotNull(result.getRankings());
            assertTrue(result.getRankings().isEmpty());
            assertEquals(0, result.getTotalGames());
            assertEquals(0, result.getTotalRounds());
            assertEquals(0, result.getTotalPlayers());
        });
    }
    
    @Test
    void testGetMonthlyPlayerRankings_WhenNoDataInMonth_ReturnsEmptyList() {
        // Mock empty repository results for the month
        when(playerStatsRepository.findStatsByMonthRange(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        
        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getMonthlyPlayerRankings(2026, 1, 100, 0);
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when no data in month");
        });
    }
    
    @Test
    void testGetMonthlyPlayerRankingsWithStats_WhenNoDataInMonth_ReturnsEmptyResponse() {
        // Mock empty repository results for the month
        when(playerStatsRepository.findStatsByMonthRange(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(playerStatsRepository.countTotalDistinctGamesInMonth(any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(gameRepository.calculateTotalRoundsInMonth(any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        
        // Should return empty response without throwing exception
        assertDoesNotThrow(() -> {
            LeaderboardResponseDTO result = playerRankingService.getMonthlyPlayerRankingsWithStats(2026, 1, 100, 0);
            assertNotNull(result);
            assertNotNull(result.getRankings());
            assertTrue(result.getRankings().isEmpty());
            assertEquals(0, result.getTotalGames());
            assertEquals(0, result.getTotalRounds());
            assertEquals(0, result.getTotalPlayers());
        });
    }
    
    @Test
    void testGetAllPlayerRankingsWithStats_UsesGameRepositoryForTotalRounds() {
        // Mock empty player stats but non-zero games/rounds
        when(playerStatsRepository.findLatestStatsForAllPlayers()).thenReturn(Collections.emptyList());
        when(playerStatsRepository.countTotalDistinctGames()).thenReturn(5L);
        when(gameRepository.calculateTotalRounds()).thenReturn(150L);
        
        // Should use GameRepository for total rounds calculation
        LeaderboardResponseDTO result = playerRankingService.getAllPlayerRankingsWithStats();
        
        assertNotNull(result);
        assertEquals(5, result.getTotalGames());
        assertEquals(150, result.getTotalRounds()); // Should come from GameRepository, not player stats
        verify(gameRepository).calculateTotalRounds();
    }
}
