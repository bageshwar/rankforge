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

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayerRankingService to ensure it handles empty database scenarios gracefully.
 * Tests cover cases where database tables don't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class PlayerRankingServiceEmptyDatabaseTest {

    @Mock
    private PersistenceLayer persistenceLayer;

    private PlayerRankingService playerRankingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.createObjectMapper();
        playerRankingService = new PlayerRankingService(objectMapper, persistenceLayer);
    }

    @Test
    void testGetAllPlayerRankings_WhenPlayerStatsTableDoesNotExist_ReturnsEmptyList() throws SQLException {
        // Mock SQLException with SQL Server error code 208 (Invalid object name)
        SQLException tableNotFoundException = new SQLException("Invalid object name 'PlayerStats'", "S0002", 208);
        
        when(persistenceLayer.query(eq("PlayerStats"), any(String[].class), isNull()))
                .thenThrow(tableNotFoundException);

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getAllPlayerRankings();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when table doesn't exist");
        });
    }

    @Test
    void testGetAllPlayerRankings_WhenPlayerStatsTableDoesNotExist_WithErrorMessage_ReturnsEmptyList() throws SQLException {
        // Mock SQLException with error message indicating table doesn't exist
        SQLException tableNotFoundException = new SQLException("Table 'PlayerStats' does not exist");
        
        when(persistenceLayer.query(eq("PlayerStats"), any(String[].class), isNull()))
                .thenThrow(tableNotFoundException);

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getAllPlayerRankings();
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when table doesn't exist");
        });
    }

    @Test
    void testGetTopPlayerRankings_WhenDatabaseIsEmpty_ReturnsEmptyList() throws SQLException {
        // Mock SQLException with SQL Server error code 208
        SQLException tableNotFoundException = new SQLException("Invalid object name 'PlayerStats'", "S0002", 208);
        
        when(persistenceLayer.query(eq("PlayerStats"), any(String[].class), isNull()))
                .thenThrow(tableNotFoundException);

        // Should return empty list without throwing exception
        assertDoesNotThrow(() -> {
            var result = playerRankingService.getTopPlayerRankings(10);
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Should return empty list when table doesn't exist");
        });
    }

    @Test
    void testGetAllPlayerRankings_WhenOtherSQLExceptionOccurs_ReturnsEmptyList() throws SQLException {
        // Mock a different SQLException (not table-not-found)
        SQLException otherException = new SQLException("Connection timeout", "08S01", 0);
        
        when(persistenceLayer.query(eq("PlayerStats"), any(String[].class), isNull()))
                .thenThrow(otherException);

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
