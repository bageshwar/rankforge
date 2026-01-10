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
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.AccoladeDTO;
import com.rankforge.server.dto.PlayerProfileDTO;
import com.rankforge.server.dto.PlayerProfileDTO.PlayerAccoladeDTO;
import com.rankforge.server.dto.PlayerProfileDTO.RatingHistoryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for player profile operations
 * Provides comprehensive player data including rating history and accolades
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class PlayerProfileService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerProfileService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    
    private final PlayerStatsRepository playerStatsRepository;
    private final AccoladeRepository accoladeRepository;
    
    @Autowired
    public PlayerProfileService(PlayerStatsRepository playerStatsRepository, 
                                  AccoladeRepository accoladeRepository) {
        this.playerStatsRepository = playerStatsRepository;
        this.accoladeRepository = accoladeRepository;
    }
    
    /**
     * Get complete player profile by player ID
     */
    public Optional<PlayerProfileDTO> getPlayerProfile(String playerId) {
        try {
            // Get player stats history
            List<PlayerStatsEntity> statsHistory = playerStatsRepository.findHistoryByPlayerId(playerId);
            
            if (statsHistory.isEmpty()) {
                LOGGER.info("No player stats found for player ID: {}", playerId);
                return Optional.empty();
            }
            
            // Get the latest stats (current state)
            PlayerStatsEntity latestStats = statsHistory.get(statsHistory.size() - 1);
            
            PlayerProfileDTO profile = new PlayerProfileDTO();
            
            // Basic info from latest stats
            profile.setPlayerId(latestStats.getPlayerId());
            profile.setPlayerName(latestStats.getLastSeenNickname() != null ? 
                    latestStats.getLastSeenNickname() : "Unknown Player");
            profile.setCurrentRank(latestStats.getRank());
            
            // Current stats
            profile.setTotalKills(latestStats.getKills());
            profile.setTotalDeaths(latestStats.getDeaths());
            profile.setTotalAssists(latestStats.getAssists());
            profile.setKillDeathRatio(latestStats.getDeaths() > 0 ? 
                    (double) latestStats.getKills() / latestStats.getDeaths() : latestStats.getKills());
            profile.setHeadshotKills(latestStats.getHeadshotKills());
            profile.setHeadshotPercentage(latestStats.getKills() > 0 ? 
                    (double) latestStats.getHeadshotKills() / latestStats.getKills() * 100 : 0);
            profile.setTotalRoundsPlayed(latestStats.getRoundsPlayed());
            profile.setClutchesWon(latestStats.getClutchesWon());
            profile.setTotalDamageDealt(latestStats.getDamageDealt());
            profile.setTotalGamesPlayed(statsHistory.size());
            
            // Build rating history
            List<RatingHistoryPoint> ratingHistory = buildRatingHistory(statsHistory);
            profile.setRatingHistory(ratingHistory);
            
            // Get accolades
            List<AccoladeEntity> accoladeEntities = accoladeRepository.findByPlayerId(playerId);
            List<PlayerAccoladeDTO> accolades = buildAccoladeList(accoladeEntities);
            profile.setAccolades(accolades);
            
            // Accolade analytics
            Map<String, Integer> accoladesByType = new HashMap<>();
            for (AccoladeEntity accolade : accoladeEntities) {
                String type = getAccoladeTypeDescription(accolade.getType());
                accoladesByType.merge(type, 1, Integer::sum);
            }
            profile.setAccoladesByType(accoladesByType);
            profile.setTotalAccolades(accoladeEntities.size());
            
            // Find most frequent accolade
            String mostFrequent = accoladesByType.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");
            profile.setMostFrequentAccolade(mostFrequent);
            
            LOGGER.info("Built profile for player {} with {} games and {} accolades", 
                    profile.getPlayerName(), statsHistory.size(), accoladeEntities.size());
            
            return Optional.of(profile);
            
        } catch (Exception e) {
            LOGGER.error("Failed to get player profile for ID: {}", playerId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Build rating history points for charting
     */
    private List<RatingHistoryPoint> buildRatingHistory(List<PlayerStatsEntity> statsHistory) {
        List<RatingHistoryPoint> history = new ArrayList<>();
        int gameNumber = 1;
        
        for (PlayerStatsEntity stats : statsHistory) {
            String formattedDate = stats.getGameTimestamp() != null ? 
                    DATE_FORMATTER.format(stats.getGameTimestamp()) : "Unknown";
            
            double kd = stats.getDeaths() > 0 ? 
                    (double) stats.getKills() / stats.getDeaths() : stats.getKills();
            
            RatingHistoryPoint point = new RatingHistoryPoint(
                    formattedDate,
                    stats.getRank(),
                    Math.round(kd * 100.0) / 100.0,
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getAssists(),
                    gameNumber++
            );
            history.add(point);
        }
        
        return history;
    }
    
    /**
     * Build accolade list from entities
     */
    private List<PlayerAccoladeDTO> buildAccoladeList(List<AccoladeEntity> accoladeEntities) {
        List<PlayerAccoladeDTO> accolades = new ArrayList<>();
        
        for (AccoladeEntity entity : accoladeEntities) {
            PlayerAccoladeDTO accolade = new PlayerAccoladeDTO();
            accolade.setType(entity.getType());
            accolade.setTypeDescription(getAccoladeTypeDescription(entity.getType()));
            accolade.setValue(entity.getValue());
            accolade.setPosition(entity.getPosition());
            accolade.setScore(entity.getScore());
            
            if (entity.getCreatedAt() != null) {
                accolade.setGameDate(DATE_FORMATTER.format(entity.getCreatedAt()));
            }
            
            if (entity.getGame() != null) {
                accolade.setGameId(entity.getGame().getId());
            }
            
            accolades.add(accolade);
        }
        
        return accolades;
    }
    
    /**
     * Get human-readable description for accolade type
     */
    private String getAccoladeTypeDescription(String type) {
        if (type == null) return "Unknown";
        
        return switch (type.toLowerCase()) {
            case "mvp" -> "MVP";
            case "headshots" -> "Headshot Master";
            case "kills" -> "Top Fragger";
            case "assists" -> "Team Player";
            case "damage" -> "Heavy Hitter";
            case "clutches" -> "Clutch King";
            case "first_blood" -> "First Blood";
            case "ace" -> "Ace";
            case "multikill" -> "Multi-Kill";
            case "survival" -> "Survivor";
            default -> type.replace("_", " ").substring(0, 1).toUpperCase() + 
                    type.replace("_", " ").substring(1).toLowerCase();
        };
    }
    
    /**
     * Get list of all players with basic info
     */
    public List<PlayerProfileDTO> getAllPlayersBasicInfo() {
        try {
            List<PlayerStatsEntity> latestStats = playerStatsRepository.findLatestStatsForAllPlayers();
            
            return latestStats.stream()
                    .map(this::buildBasicProfile)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            LOGGER.error("Failed to get all players basic info", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Build basic profile from entity (without full history)
     */
    private PlayerProfileDTO buildBasicProfile(PlayerStatsEntity stats) {
        PlayerProfileDTO profile = new PlayerProfileDTO();
        profile.setPlayerId(stats.getPlayerId());
        profile.setPlayerName(stats.getLastSeenNickname() != null ? 
                stats.getLastSeenNickname() : "Unknown Player");
        profile.setCurrentRank(stats.getRank());
        profile.setTotalKills(stats.getKills());
        profile.setTotalDeaths(stats.getDeaths());
        profile.setKillDeathRatio(stats.getDeaths() > 0 ? 
                (double) stats.getKills() / stats.getDeaths() : stats.getKills());
        return profile;
    }
}
