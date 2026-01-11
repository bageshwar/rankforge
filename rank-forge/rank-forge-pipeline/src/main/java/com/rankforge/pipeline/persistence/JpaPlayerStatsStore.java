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

package com.rankforge.pipeline.persistence;

import com.rankforge.core.events.GameOverEvent;
import com.rankforge.core.events.GameProcessedEvent;
import com.rankforge.core.events.RoundEndEvent;
import com.rankforge.core.events.RoundStartEvent;
import com.rankforge.core.interfaces.GameEventListener;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JPA-based implementation of PlayerStatsStore
 * Author bageshwar.pn
 * Date 2026
 */
public class JpaPlayerStatsStore implements PlayerStatsStore, GameEventListener {
    private static final Logger logger = LoggerFactory.getLogger(JpaPlayerStatsStore.class);
    
    private final PlayerStatsRepository repository;
    private final Map<String, PlayerStats> playerStatsMap;
    private final EventProcessingContext context;
    
    public JpaPlayerStatsStore(PlayerStatsRepository repository) {
        this.repository = repository;
        this.playerStatsMap = new ConcurrentHashMap<>();
        this.context = null; // For backward compatibility
    }
    
    public JpaPlayerStatsStore(PlayerStatsRepository repository, EventProcessingContext context) {
        this.repository = repository;
        this.playerStatsMap = new ConcurrentHashMap<>();
        this.context = context;
    }
    
    /**
     * Convert PlayerStats domain object to PlayerStatsEntity
     */
    private PlayerStatsEntity convertToEntity(PlayerStats stats) {
        PlayerStatsEntity entity = new PlayerStatsEntity();
        entity.setPlayerId(stats.getPlayerId());
        entity.setKills(stats.getKills());
        entity.setDeaths(stats.getDeaths());
        entity.setAssists(stats.getAssists());
        entity.setHeadshotKills(stats.getHeadshotKills());
        entity.setRoundsPlayed(stats.getRoundsPlayed());
        entity.setClutchesWon(stats.getClutchesWon());
        entity.setDamageDealt(stats.getDamageDealt());
        entity.setLastUpdated(stats.getLastUpdated() != null ? stats.getLastUpdated() : Instant.now());
        entity.setRank(stats.getRank());
        entity.setLastSeenNickname(stats.getLastSeenNickname());
        return entity;
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
     * Stores multiple player stats in batches
     * Always inserts new records to track progression over time
     * Each record is associated with a game entity for confident deletion
     */
    @Transactional
    private void storeBatch(Collection<PlayerStats> stats, Instant gameTimestamp, GameEntity game) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        
        try {
            // Deduplicate by playerId within the batch - keep the latest stats for each player in this game
            Map<String, PlayerStats> uniqueStats = new HashMap<>();
            for (PlayerStats stat : stats) {
                if (stat.getPlayerId() != null) {
                    // If player already in map, keep the one with later lastUpdated
                    PlayerStats existing = uniqueStats.get(stat.getPlayerId());
                    if (existing == null || 
                        (stat.getLastUpdated() != null && 
                         (existing.getLastUpdated() == null || stat.getLastUpdated().isAfter(existing.getLastUpdated())))) {
                        uniqueStats.put(stat.getPlayerId(), stat);
                    }
                }
            }
            
            if (uniqueStats.isEmpty()) {
                return;
            }
            
            List<PlayerStatsEntity> entitiesToSave = new ArrayList<>();
            
            // Always create new entities for historical tracking
            for (PlayerStats stat : uniqueStats.values()) {
                PlayerStatsEntity entity = convertToEntity(stat);
                entity.setGameTimestamp(gameTimestamp);
                entity.setGame(game); // Set game reference for confident deletion
                entity.setLastUpdated(stat.getLastUpdated() != null ? stat.getLastUpdated() : Instant.now());
                entitiesToSave.add(entity);
            }
            
            // Always insert new records (never update) to track progression
            repository.saveAll(entitiesToSave);
            logger.info("Batch stored {} player stats for game ID {} at {} (historical records)", 
                    entitiesToSave.size(), 
                    game != null ? game.getId() : "unknown",
                    gameTimestamp);
            
        } catch (Exception e) {
            logger.error("Failed to batch store PlayerStats", e);
            throw e; // Re-throw to let caller handle
        }
    }

    @Override
    public void store(PlayerStats stat, boolean archive) {
        playerStatsMap.put(stat.getPlayerId(), stat);
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(String playerSteamId) {
        if (playerSteamId == null) {
            return Optional.empty();
        }
        
        // First check in-memory cache
        if (playerStatsMap.containsKey(playerSteamId)) {
            return Optional.of(playerStatsMap.get(playerSteamId));
        }
        
        // Then check database
        try {
            Optional<PlayerStatsEntity> entityOpt = repository.findByPlayerId(playerSteamId);
            if (entityOpt.isPresent()) {
                PlayerStats stats = convertToDomain(entityOpt.get());
                // Cache it
                playerStatsMap.put(playerSteamId, stats);
                return Optional.of(stats);
            }
        } catch (org.springframework.beans.factory.BeanCreationNotAllowedException e) {
            // Spring context is shutting down - this is expected during test cleanup
            // Don't log as error, just return empty (processing will stop naturally)
            logger.debug("Spring context shutting down, cannot retrieve PlayerStats for {}", playerSteamId);
        } catch (Exception e) {
            logger.error("Failed to get PlayerStats for {}", playerSteamId, e);
        }
        
        return Optional.empty();
    }

    @Override
    public void onGameStarted(GameOverEvent event) {
        // no-op
    }

    @Override
    @Transactional
    public void onGameEnded(GameProcessedEvent event) {
        Instant gameTimestamp = event.getTimestamp();
        // Get the game entity from context if available (game is already persisted at this point)
        GameEntity game = null;
        if (context != null) {
            game = context.getCurrentGame();
        }
        storeBatch(playerStatsMap.values(), gameTimestamp, game);
        playerStatsMap.clear();
    }

    @Override
    public void onRoundStarted(RoundStartEvent event) {
        // no-op
    }

    @Override
    public void onRoundEnded(RoundEndEvent event) {
        // no-op
    }
}
