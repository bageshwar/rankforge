/*
 *
 *  *Copyright [2026] [Bageshwar Pratap Narain]
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

import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.PlayerProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for accolade timestamp handling in PlayerProfileService.
 * 
 * Tests cover:
 * - Using game end time as the timestamp when available
 * - Fallback to createdAt if game is not available
 * - Timezone consistency
 * - Month/year boundary handling
 * 
 * Author bageshwar.pn
 * Date 2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerProfileService Accolade Timestamp Tests")
class PlayerProfileServiceAccoladeTimestampTest {
    
    @Mock
    private PlayerStatsRepository playerStatsRepository;
    
    @Mock
    private AccoladeRepository accoladeRepository;
    
    @InjectMocks
    private PlayerProfileService playerProfileService;
    
    private PlayerStatsEntity mockPlayerStats;
    private DateTimeFormatter dateFormatter;
    
    @BeforeEach
    void setUp() {
        dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        
        // Setup mock player stats
        mockPlayerStats = new PlayerStatsEntity();
        mockPlayerStats.setPlayerId("123456789");
        mockPlayerStats.setLastSeenNickname("TestPlayer");
        mockPlayerStats.setRank(10);
        mockPlayerStats.setKills(100);
        mockPlayerStats.setDeaths(50);
        mockPlayerStats.setAssists(25);
        mockPlayerStats.setHeadshotKills(30);
        mockPlayerStats.setRoundsPlayed(200);
        mockPlayerStats.setClutchesWon(5);
        mockPlayerStats.setDamageDealt(5000.0);
        mockPlayerStats.setGameTimestamp(Instant.now());
    }
    
    @Test
    @DisplayName("Should use game end time as accolade timestamp")
    void shouldUseGameEndTimeAsAccoladeTimestamp() {
        // Given
        Instant gameEndTime = Instant.parse("2026-01-15T14:30:00Z");
        Instant differentCreatedAt = Instant.parse("2026-01-15T14:35:00Z"); // Different from game end
        
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setEndTime(gameEndTime);
        game.setGameOverTimestamp(gameEndTime);
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setId(1L);
        accolade.setType("MVP");
        accolade.setPlayerName("TestPlayer");
        accolade.setPlayerId("123456789");
        accolade.setValue(1.0);
        accolade.setPosition(1);
        accolade.setScore(100.0);
        accolade.setCreatedAt(differentCreatedAt); // Different timestamp
        accolade.setGame(game);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(1, profile.getAccolades().size());
        
        String formattedDate = profile.getAccolades().get(0).getGameDate();
        String expectedDate = dateFormatter.format(gameEndTime);
        assertEquals(expectedDate, formattedDate, 
                "Accolade timestamp should use game end time, not createdAt");
    }
    
    @Test
    @DisplayName("Should use gameOverTimestamp if endTime is null")
    void shouldUseGameOverTimestampIfEndTimeIsNull() {
        // Given
        Instant gameOverTimestamp = Instant.parse("2026-01-15T14:30:00Z");
        
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setEndTime(null);
        game.setGameOverTimestamp(gameOverTimestamp);
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setId(1L);
        accolade.setType("MVP");
        accolade.setPlayerName("TestPlayer");
        accolade.setPlayerId("123456789");
        accolade.setValue(1.0);
        accolade.setPosition(1);
        accolade.setScore(100.0);
        accolade.setGame(game);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(1, profile.getAccolades().size());
        
        String formattedDate = profile.getAccolades().get(0).getGameDate();
        String expectedDate = dateFormatter.format(gameOverTimestamp);
        assertEquals(expectedDate, formattedDate);
    }
    
    @Test
    @DisplayName("Should fallback to createdAt if game is null")
    void shouldFallbackToCreatedAtIfGameIsNull() {
        // Given
        Instant createdAt = Instant.parse("2026-01-15T14:30:00Z");
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setId(1L);
        accolade.setType("MVP");
        accolade.setPlayerName("TestPlayer");
        accolade.setPlayerId("123456789");
        accolade.setValue(1.0);
        accolade.setPosition(1);
        accolade.setScore(100.0);
        accolade.setCreatedAt(createdAt);
        accolade.setGame(null);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(1, profile.getAccolades().size());
        
        String formattedDate = profile.getAccolades().get(0).getGameDate();
        String expectedDate = dateFormatter.format(createdAt);
        assertEquals(expectedDate, formattedDate);
    }
    
    @Test
    @DisplayName("Should handle timezone conversion correctly")
    void shouldHandleTimezoneConversionCorrectly() {
        // Given - game end time in UTC
        Instant gameEndTimeUTC = Instant.parse("2026-01-15T14:30:00Z");
        
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setEndTime(gameEndTimeUTC);
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setId(1L);
        accolade.setType("MVP");
        accolade.setPlayerName("TestPlayer");
        accolade.setPlayerId("123456789");
        accolade.setValue(1.0);
        accolade.setPosition(1);
        accolade.setScore(100.0);
        accolade.setGame(game);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(1, profile.getAccolades().size());
        
        String formattedDate = profile.getAccolades().get(0).getGameDate();
        // Should be formatted in system default timezone
        String expectedDate = dateFormatter.format(gameEndTimeUTC);
        assertEquals(expectedDate, formattedDate);
        assertNotNull(formattedDate);
    }
    
    @Test
    @DisplayName("Should handle month boundary correctly")
    void shouldHandleMonthBoundaryCorrectly() {
        // Given - game ending at month boundary
        Instant gameEndTime = Instant.parse("2026-01-31T23:59:59Z");
        
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setEndTime(gameEndTime);
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setId(1L);
        accolade.setType("MVP");
        accolade.setPlayerName("TestPlayer");
        accolade.setPlayerId("123456789");
        accolade.setValue(1.0);
        accolade.setPosition(1);
        accolade.setScore(100.0);
        accolade.setGame(game);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(1, profile.getAccolades().size());
        
        String formattedDate = profile.getAccolades().get(0).getGameDate();
        assertNotNull(formattedDate);
        assertTrue(formattedDate.contains("Jan 31") || formattedDate.contains("Feb 01"), 
                "Should handle month boundary correctly");
    }
    
    @Test
    @DisplayName("Should handle year boundary correctly")
    void shouldHandleYearBoundaryCorrectly() {
        // Given - game ending at year boundary
        Instant gameEndTime = Instant.parse("2025-12-31T23:59:59Z");
        
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setEndTime(gameEndTime);
        
        AccoladeEntity accolade = new AccoladeEntity();
        accolade.setId(1L);
        accolade.setType("MVP");
        accolade.setPlayerName("TestPlayer");
        accolade.setPlayerId("123456789");
        accolade.setValue(1.0);
        accolade.setPosition(1);
        accolade.setScore(100.0);
        accolade.setGame(game);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(1, profile.getAccolades().size());
        
        String formattedDate = profile.getAccolades().get(0).getGameDate();
        assertNotNull(formattedDate);
        assertTrue(formattedDate.contains("2025") || formattedDate.contains("2026"), 
                "Should handle year boundary correctly");
    }
    
    @Test
    @DisplayName("Should handle multiple accolades with different game end times")
    void shouldHandleMultipleAccoladesWithDifferentGameEndTimes() {
        // Given
        Instant game1EndTime = Instant.parse("2026-01-15T14:30:00Z");
        Instant game2EndTime = Instant.parse("2026-01-20T16:45:00Z");
        
        GameEntity game1 = new GameEntity();
        game1.setId(1L);
        game1.setEndTime(game1EndTime);
        
        GameEntity game2 = new GameEntity();
        game2.setId(2L);
        game2.setEndTime(game2EndTime);
        
        AccoladeEntity accolade1 = new AccoladeEntity();
        accolade1.setId(1L);
        accolade1.setType("MVP");
        accolade1.setPlayerName("TestPlayer");
        accolade1.setPlayerId("123456789");
        accolade1.setValue(1.0);
        accolade1.setPosition(1);
        accolade1.setScore(100.0);
        accolade1.setGame(game1);
        
        AccoladeEntity accolade2 = new AccoladeEntity();
        accolade2.setId(2L);
        accolade2.setType("TopKills");
        accolade2.setPlayerName("TestPlayer");
        accolade2.setPlayerId("123456789");
        accolade2.setValue(25.0);
        accolade2.setPosition(1);
        accolade2.setScore(95.0);
        accolade2.setGame(game2);
        
        when(playerStatsRepository.findHistoryByPlayerId("123456789"))
                .thenReturn(Arrays.asList(mockPlayerStats));
        when(accoladeRepository.findByPlayerId("123456789"))
                .thenReturn(Arrays.asList(accolade1, accolade2));
        
        // When
        Optional<PlayerProfileDTO> profileOpt = playerProfileService.getPlayerProfile("123456789");
        
        // Then
        assertTrue(profileOpt.isPresent());
        PlayerProfileDTO profile = profileOpt.get();
        assertEquals(2, profile.getAccolades().size());
        
        String date1 = profile.getAccolades().get(0).getGameDate();
        String date2 = profile.getAccolades().get(1).getGameDate();
        
        String expectedDate1 = dateFormatter.format(game1EndTime);
        String expectedDate2 = dateFormatter.format(game2EndTime);
        
        // Both should use their respective game end times
        assertTrue(date1.equals(expectedDate1) || date2.equals(expectedDate1));
        assertTrue(date1.equals(expectedDate2) || date2.equals(expectedDate2));
    }
}
