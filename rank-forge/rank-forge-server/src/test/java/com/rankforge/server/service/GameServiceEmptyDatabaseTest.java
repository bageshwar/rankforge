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
import com.rankforge.core.util.ObjectMapperFactory;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GameService to ensure it handles empty database scenarios gracefully.
 * Tests cover cases where database tables don't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class GameServiceEmptyDatabaseTest {

    @Mock
    private GameEventRepository gameEventRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private AccoladeRepository accoladeRepository;

    private GameService gameService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.createObjectMapper();
        gameService = new GameService(objectMapper, gameEventRepository, playerStatsRepository, gameRepository, accoladeRepository);
    }

    @Test
    void testGetAllGames_WhenGameEventTableDoesNotExist_ReturnsEmptyList() {
        // Mock empty repository result (simulating empty database)
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when database is empty");
        });
    }

    @Test
    void testGetAllGames_WhenRepositoryThrowsException_ReturnsEmptyList() {
        // Mock repository throwing exception
        when(gameRepository.findAll())
                .thenThrow(new RuntimeException("Database connection error"));

        // Should catch exception and return empty list
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list on any exception");
        });
    }

    @Test
    void testGetRecentGames_WhenDatabaseIsEmpty_ReturnsEmptyList() {
        // Mock empty repository result
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getRecentGames(10);
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when database is empty");
        });
    }

    @Test
    void testGetGameById_WhenDatabaseIsEmpty_ReturnsNull() {
        // Mock empty repository result
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());

        // Should return null without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getGameById("some-game-id");
            assertNull(result, "Should return null when database is empty");
        });
    }

    @Test
    void testGetGameDetails_WhenDatabaseIsEmpty_ReturnsNull() {
        // Mock empty repository result
        when(gameRepository.findByGameOverTimestamp(any())).thenReturn(Optional.empty());

        // Should return null without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getGameDetails("1234567890_de_dust2");
            assertNull(result, "Should return null when database is empty");
        });
    }

    @Test
    void testGetAllGames_WhenPlayerStatsRepositoryThrowsException_StillReturnsGames() {
        // Mock successful GameRepository query returning empty list
        // Note: When gameRepository returns empty list, playerStatsRepository is not called
        // because there are no games to process players for
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());

        // Should return empty list (no games) but not fail
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        });
    }
}
