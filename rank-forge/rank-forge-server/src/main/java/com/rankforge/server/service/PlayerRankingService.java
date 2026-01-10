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

import com.rankforge.core.models.PlayerStats;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.PlayerRankingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for managing player rankings
 * Uses Spring Data JPA to provide real player statistics and rankings.
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class PlayerRankingService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRankingService.class);
    
    private final PlayerStatsRepository playerStatsRepository;
    
    @Autowired
    public PlayerRankingService(PlayerStatsRepository playerStatsRepository) {
        this.playerStatsRepository = playerStatsRepository;
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
     * Convert PlayerStatsEntity to PlayerStats domain object
     */
    private PlayerStats convertToDomain(PlayerStatsEntity entity) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(entity.getPlayerId());
        stats.setKills(entity.getKills());
        stats.setDeaths(entity.getDeaths());
        stats.setAssists(entity.getAssists());
        stats.setHeadshotKills(entity.getHeadshotKills());
        stats.setRoundsPlayed(entity.getRoundsPlayed());
        stats.setClutchesWon(entity.getClutchesWon());
        stats.setDamageDealt(entity.getDamageDealt());
        stats.setLastUpdated(entity.getLastUpdated());
        stats.setRank(entity.getRank());
        stats.setLastSeenNickname(entity.getLastSeenNickname());
        return stats;
    }
    
    /**
     * Retrieves all player statistics from the database
     * Gets the latest stats for each player (most recent gameTimestamp)
     */
    private List<PlayerStats> getAllPlayerStatsFromDatabase() {
        List<PlayerStats> playerStatsList = new ArrayList<>();
        
        try {
            // Get latest stats for all players (one record per player)
            List<PlayerStatsEntity> entities = playerStatsRepository.findLatestStatsForAllPlayers();
            
            for (PlayerStatsEntity entity : entities) {
                PlayerStats stats = convertToDomain(entity);
                playerStatsList.add(stats);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve player statistics", e);
        }

        LOGGER.info("Retrieved {} player statistics from database (latest stats per player)", playerStatsList.size());
        return playerStatsList;
    }
    

    /**
     * Gets a specific player's ranking and statistics
     */
    public Optional<PlayerRankingDTO> getPlayerRanking(String playerId) {
        return Optional.empty();
    }
}