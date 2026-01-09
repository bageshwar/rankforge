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
import com.rankforge.pipeline.persistence.PersistenceLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.ResultSet;
import java.sql.SQLException;

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
    private PersistenceLayer persistenceLayer;

    @Mock
    private ResultSet resultSet;

    private GameService gameService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.createObjectMapper();
        gameService = new GameService(objectMapper, persistenceLayer);
    }

    @Test
    void testGetAllGames_WhenGameEventTableDoesNotExist_ReturnsEmptyList() throws SQLException {
        // Mock SQLException with SQL Server error code 208 (Invalid object name)
        SQLException tableNotFoundException = new SQLException("Invalid object name 'GameEvent'", "S0002", 208);
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), anyString(), any()))
                .thenThrow(tableNotFoundException);

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when table doesn't exist");
        });
    }

    @Test
    void testGetAllGames_WhenGameEventTableDoesNotExist_WithErrorMessage_ReturnsEmptyList() throws SQLException {
        // Mock SQLException with error message indicating table doesn't exist
        SQLException tableNotFoundException = new SQLException("Table 'GameEvent' does not exist");
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), anyString(), any()))
                .thenThrow(tableNotFoundException);

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when table doesn't exist");
        });
    }

    @Test
    void testGetRecentGames_WhenDatabaseIsEmpty_ReturnsEmptyList() throws SQLException {
        // Mock SQLException with SQL Server error code 208
        SQLException tableNotFoundException = new SQLException("Invalid object name 'GameEvent'", "S0002", 208);
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), anyString(), any()))
                .thenThrow(tableNotFoundException);

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getRecentGames(10);
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when table doesn't exist");
        });
    }

    @Test
    void testGetGameById_WhenDatabaseIsEmpty_ReturnsNull() throws SQLException {
        // Mock SQLException with SQL Server error code 208
        SQLException tableNotFoundException = new SQLException("Invalid object name 'GameEvent'", "S0002", 208);
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), anyString(), any()))
                .thenThrow(tableNotFoundException);

        // Should return null without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getGameById("some-game-id");
            assertNull(result, "Should return null when table doesn't exist");
        });
    }

    @Test
    void testGetGameDetails_WhenDatabaseIsEmpty_ReturnsNull() throws SQLException {
        // Mock SQLException with SQL Server error code 208
        SQLException tableNotFoundException = new SQLException("Invalid object name 'GameEvent'", "S0002", 208);
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), anyString(), any()))
                .thenThrow(tableNotFoundException);

        // Should return null without throwing exception
        assertDoesNotThrow(() -> {
            var result = gameService.getGameDetails("some-game-id");
            assertNull(result, "Should return null when table doesn't exist");
        });
    }

    @Test
    void testGetAllGames_WhenOtherSQLExceptionOccurs_ReturnsEmptyList() throws SQLException {
        // Mock a different SQLException (not table-not-found)
        SQLException otherException = new SQLException("Connection timeout", "08S01", 0);
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), anyString(), any()))
                .thenThrow(otherException);

        // Should catch exception and return empty list
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list on any exception");
        });
    }

    @Test
    void testGetAllGames_WhenPlayerStatsTableDoesNotExist_StillReturnsGames() throws SQLException {
        // Mock successful GameEvent query but PlayerStats table doesn't exist
        when(resultSet.next()).thenReturn(false); // No GameOver events
        
        when(persistenceLayer.query(eq("GameEvent"), any(String[].class), 
                eq("gameEventType = ?"), any()))
                .thenReturn(resultSet);

        // Should return empty list (no games) but not fail
        assertDoesNotThrow(() -> {
            var result = gameService.getAllGames();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        });
    }
}
