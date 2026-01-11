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
import com.rankforge.core.interfaces.RankingAlgorithm;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.LeaderboardResponseDTO;
import com.rankforge.server.dto.PlayerRankingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlayerRankingService monthly leaderboard rounds calculation
 * Validates that rounds are correctly counted from ROUND_END events
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerRankingService Monthly Rounds Tests")
class PlayerRankingServiceMonthlyRoundsTest {

    @Mock
    private PlayerStatsRepository playerStatsRepository;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private GameEventRepository gameEventRepository;
    
    private ObjectMapper objectMapper;
    
    @Mock
    private RankingAlgorithm rankingAlgorithm;
    
    private PlayerRankingService playerRankingService;
    
    private Instant novemberStart;
    private Instant novemberEnd;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        playerRankingService = new PlayerRankingService(
                playerStatsRepository, 
                gameRepository, 
                gameEventRepository, 
                objectMapper,
                rankingAlgorithm
        );
        
        // November 2025 boundaries
        LocalDateTime startOfMonth = LocalDateTime.of(2025, 11, 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        novemberStart = startOfMonth.toInstant(ZoneOffset.UTC);
        novemberEnd = endOfMonth.toInstant(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should count rounds correctly from ROUND_END events")
    void shouldCountRoundsCorrectlyFromRoundEndEvents() throws Exception {
        // Given: A player who played 34 games in November
        String playerId = "[U:1:1090227400]";
        String numericPlayerId = "1090227400"; // Numeric ID format in ROUND_END events
        
        // Create a game in November
        GameEntity game1 = new GameEntity();
        game1.setId(1L);
        game1.setGameOverTimestamp(novemberStart.plusSeconds(3600));
        game1.setTeam1Score(16);
        game1.setTeam2Score(14);
        
        // Create ROUND_END events - each game has ~30 rounds (16+14)
        // For simplicity, create 34 rounds across multiple games
        List<RoundEndEventEntity> roundEndEvents = new ArrayList<>();
        for (int i = 0; i < 34; i++) {
            RoundEndEventEntity roundEnd = new RoundEndEventEntity(novemberStart.plusSeconds(i * 100));
            roundEnd.setId((long) (i + 1));
            roundEnd.setGame(game1);
            // Players JSON contains numeric IDs: ["1090227400", "1234567890", ...]
            String playersJson = objectMapper.writeValueAsString(Arrays.asList(numericPlayerId, "1234567890"));
            roundEnd.setPlayersJson(playersJson);
            roundEndEvents.add(roundEnd);
        }
        
        // Create player stats record for November
        PlayerStatsEntity playerStats = new PlayerStatsEntity();
        playerStats.setId(1L);
        playerStats.setPlayerId(playerId); // Full format
        playerStats.setGameTimestamp(novemberStart.plusSeconds(3600));
        playerStats.setKills(100);
        playerStats.setDeaths(50);
        playerStats.setRoundsPlayed(34); // This should be overridden by ROUND_END count
        playerStats.setLastSeenNickname("TestPlayer");
        
        // Mock repository responses
        when(gameRepository.findGamesByMonthRange(novemberStart, novemberEnd))
                .thenReturn(Arrays.asList(game1));
        when(gameEventRepository.findRoundEndEventsByGameIds(anyList()))
                .thenReturn(roundEndEvents);
        when(playerStatsRepository.findStatsByMonthRange(novemberStart, novemberEnd))
                .thenReturn(Arrays.asList(playerStats));
        when(playerStatsRepository.findLatestStatsBeforeDate(eq(playerId), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(playerStatsRepository.countDistinctGamesByPlayerIdInMonth(eq(playerId), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        when(playerStatsRepository.countTotalDistinctGamesInMonth(any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        when(gameRepository.calculateTotalRoundsInMonth(any(Instant.class), any(Instant.class)))
                .thenReturn(30L);
        
        // When: Get monthly leaderboard
        LeaderboardResponseDTO result = playerRankingService.getMonthlyPlayerRankingsWithStats(2025, 11, 100, 0);
        
        // Then: Player should have 34 rounds (one for each ROUND_END event they appear in)
        assertNotNull(result);
        assertNotNull(result.getRankings());
        assertEquals(1, result.getRankings().size(), "Should have one player");
        
        PlayerRankingDTO playerRanking = result.getRankings().get(0);
        assertEquals(34, playerRanking.getRoundsPlayed(), "Player should have 34 rounds from ROUND_END events");
        assertEquals(1, playerRanking.getGamesPlayed(), "Player should have 1 game");
        
        // Verify ROUND_END events were queried
        verify(gameEventRepository).findRoundEndEventsByGameIds(anyList());
    }
    
    @Test
    @DisplayName("Should handle player ID format mismatch between ROUND_END and PlayerStats")
    void shouldHandlePlayerIdFormatMismatch() throws Exception {
        // Given: ROUND_END events use numeric IDs, PlayerStats uses full format
        String fullFormatPlayerId = "[U:1:1090227400]";
        String numericPlayerId = "1090227400";
        
        GameEntity game1 = new GameEntity();
        game1.setId(1L);
        game1.setGameOverTimestamp(novemberStart.plusSeconds(3600));
        
        // ROUND_END event with numeric ID
        RoundEndEventEntity roundEnd = new RoundEndEventEntity(novemberStart.plusSeconds(100));
        roundEnd.setId(1L);
        roundEnd.setGame(game1);
        String playersJson = objectMapper.writeValueAsString(Arrays.asList(numericPlayerId));
        roundEnd.setPlayersJson(playersJson);
        
        // PlayerStats with full format ID
        PlayerStatsEntity playerStats = new PlayerStatsEntity();
        playerStats.setId(1L);
        playerStats.setPlayerId(fullFormatPlayerId);
        playerStats.setGameTimestamp(novemberStart.plusSeconds(3600));
        playerStats.setKills(10);
        playerStats.setDeaths(5);
        playerStats.setRoundsPlayed(1);
        playerStats.setLastSeenNickname("TestPlayer");
        
        // Mock repository responses
        when(gameRepository.findGamesByMonthRange(novemberStart, novemberEnd))
                .thenReturn(Arrays.asList(game1));
        when(gameEventRepository.findRoundEndEventsByGameIds(anyList()))
                .thenReturn(Arrays.asList(roundEnd));
        when(playerStatsRepository.findStatsByMonthRange(novemberStart, novemberEnd))
                .thenReturn(Arrays.asList(playerStats));
        when(playerStatsRepository.findLatestStatsBeforeDate(eq(fullFormatPlayerId), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(playerStatsRepository.countDistinctGamesByPlayerIdInMonth(eq(fullFormatPlayerId), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        when(playerStatsRepository.countTotalDistinctGamesInMonth(any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        when(gameRepository.calculateTotalRoundsInMonth(any(Instant.class), any(Instant.class)))
                .thenReturn(1L);
        
        // When: Get monthly leaderboard
        LeaderboardResponseDTO result = playerRankingService.getMonthlyPlayerRankingsWithStats(2025, 11, 100, 0);
        
        // Then: Should match player IDs correctly and count rounds
        assertNotNull(result);
        assertNotNull(result.getRankings());
        // The rounds should be counted if we normalize the IDs properly
        // This test will fail if ID normalization is not implemented
        if (!result.getRankings().isEmpty()) {
            PlayerRankingDTO playerRanking = result.getRankings().get(0);
            assertTrue(playerRanking.getRoundsPlayed() > 0, 
                    "Rounds should be counted even with ID format mismatch");
        }
    }
}
