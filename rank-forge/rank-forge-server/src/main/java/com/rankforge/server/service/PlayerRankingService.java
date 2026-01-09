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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.pipeline.persistence.PersistenceLayer;
import com.rankforge.server.dto.PlayerRankingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for managing player rankings
 * Integrates with the persistence layer to provide real player statistics and rankings.
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class PlayerRankingService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRankingService.class);
    
    private final ObjectMapper objectMapper;
    private final PersistenceLayer persistenceLayer;
    
    @Autowired
    public PlayerRankingService(ObjectMapper objectMapper, PersistenceLayer persistenceLayer) {
        this.objectMapper = objectMapper;
        this.persistenceLayer = persistenceLayer;
    }

    /**
     * Get all player rankings sorted by existing rank field
     * Fetches real data from the persistence layer and sorts by rank
     */
    public List<PlayerRankingDTO> getAllPlayerRankings() {
        try {
            List<PlayerStats> playerStats = getAllPlayerStatsFromDatabase();
            
            // Sort by existing rank field (descending order - rank 1 is best)
            playerStats.sort((p1, p2) -> Integer.compare(p2.getRank(), p1.getRank()));
            
            return playerStats.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve player rankings", e);
            // Return empty list on error instead of crashing
            return new ArrayList<>();
        }
    }

    /**
     * Get top N player rankings
     */
    public List<PlayerRankingDTO> getTopPlayerRankings(int limit) {
        return getAllPlayerRankings().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Convert PlayerStats to PlayerRankingDTO
     */
    private PlayerRankingDTO convertToDTO(PlayerStats stats) {
        return new PlayerRankingDTO(
                stats.getRank(),
                stats.getLastSeenNickname(),
                stats.getPlayerId(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getAssists(),
                stats.getHeadshotKills(),
                stats.getRoundsPlayed(),
                stats.getClutchesWon(),
                stats.getDamageDealt()
        );
    }

    /**
     * Retrieves all player statistics from the database
     */
    private List<PlayerStats> getAllPlayerStatsFromDatabase() throws SQLException {
        List<PlayerStats> playerStatsList = new ArrayList<>();
        
        try (ResultSet resultSet = persistenceLayer.query("PlayerStats", 
                new String[]{"playerId", "playerStats"}, null)) {
            
            while (resultSet.next()) {
                String playerId = resultSet.getString("playerId");
                try {
                    PlayerStats statsOpt = objectMapper.readValue(resultSet.getString("playerStats"), PlayerStats.class);
                    playerStatsList.add(statsOpt);
                } catch (JsonProcessingException e) {
                    LOGGER.warn("Failed to parse playerId {}", playerId, e);
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            if (isTableNotFoundError(e)) {
                LOGGER.info("PlayerStats table does not exist yet. Returning empty list.");
                return playerStatsList;
            }
            throw e;
        }

        LOGGER.info("Retrieved {} player statistics from database", playerStatsList.size());
        return playerStatsList;
    }
    
    /**
     * Checks if an SQLException indicates that a table does not exist.
     * Handles SQL Server (error code 208) and other common database error codes.
     */
    private boolean isTableNotFoundError(SQLException e) {
        if (e == null) {
            return false;
        }
        
        // SQL Server error code for "Invalid object name"
        if (e.getErrorCode() == 208) {
            return true;
        }
        
        // Check error message for common table-not-found patterns
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            String lowerMessage = errorMessage.toLowerCase();
            return lowerMessage.contains("invalid object name") ||
                   lowerMessage.contains("table") && lowerMessage.contains("doesn't exist") ||
                   lowerMessage.contains("table") && lowerMessage.contains("does not exist") ||
                   lowerMessage.contains("no such table") ||
                   lowerMessage.contains("relation") && lowerMessage.contains("does not exist");
        }
        
        return false;
    }
    

    /**
     * Gets a specific player's ranking and statistics
     */
    public Optional<PlayerRankingDTO> getPlayerRanking(String playerId) {
        return Optional.empty();
    }
}