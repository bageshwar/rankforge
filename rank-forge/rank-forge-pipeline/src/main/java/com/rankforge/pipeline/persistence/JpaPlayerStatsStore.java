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
    
    public JpaPlayerStatsStore(PlayerStatsRepository repository) {
        this.repository = repository;
        this.playerStatsMap = new ConcurrentHashMap<>();
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
     */
    @Transactional
    private void storeBatch(Collection<PlayerStats> stats) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        
        try {
            List<PlayerStatsEntity> entities = new ArrayList<>();
            for (PlayerStats stat : stats) {
                PlayerStatsEntity entity = convertToEntity(stat);
                entity.setLastUpdated(Instant.now());
                entities.add(entity);
            }
            
            // Use saveAll which will handle upsert logic
            repository.saveAll(entities);
            logger.info("Batch stored {} player stats", entities.size());
            
        } catch (Exception e) {
            logger.error("Failed to batch store PlayerStats", e);
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
        storeBatch(playerStatsMap.values());
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
