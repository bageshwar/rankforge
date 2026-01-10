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

import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private PlayerRankingService playerRankingService;

    @BeforeEach
    void setUp() {
        playerRankingService = new PlayerRankingService(playerStatsRepository);
    }

    @Test
    void testGetAllPlayerRankings_WhenPlayerStatsTableDoesNotExist_ReturnsEmptyList() {
        // Mock empty repository result (simulating empty database)
        when(playerStatsRepository.findAllByOrderByRankAsc()).thenReturn(Collections.emptyList());

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
        when(playerStatsRepository.findAllByOrderByRankAsc())
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
        when(playerStatsRepository.findAllByOrderByRankAsc()).thenReturn(Collections.emptyList());

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
        when(playerStatsRepository.findAllByOrderByRankAsc())
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
}
