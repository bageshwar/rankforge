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

package com.rankforge.pipeline.persistence.repository;

import com.rankforge.pipeline.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Game entities.
 * All game fetching is done by ID - timestamp-based scanning has been removed.
 * Author bageshwar.pn
 * Date 2026
 */
@Repository
public interface GameRepository extends JpaRepository<GameEntity, Long> {
    // Uses JpaRepository's built-in findById(Long id), findAll(), etc.
    
    /**
     * Find games by timestamp and map for deduplication.
     * Used to check if a game with the same identity already exists.
     * 
     * @param gameOverTimestamp The game over timestamp
     * @param map The map name
     * @return List of matching games (should be empty or contain one game)
     */
    @Query("SELECT g FROM GameEntity g WHERE g.gameOverTimestamp = :gameOverTimestamp AND g.map = :map")
    List<GameEntity> findByGameOverTimestampAndMap(
            @Param("gameOverTimestamp") Instant gameOverTimestamp,
            @Param("map") String map
    );
    
    /**
     * Find a game by timestamp and map (for deduplication check).
     * Returns Optional.empty() if no duplicate exists.
     * 
     * Uses a tolerance check for timestamps to handle potential precision differences
     * between database storage and Java Instant (e.g., SQL Server datetime2 precision).
     * Checks for timestamps within 1 second of each other.
     */
    default Optional<GameEntity> findDuplicate(Instant gameOverTimestamp, String map) {
        // First try exact match
        List<GameEntity> exactMatches = findByGameOverTimestampAndMap(gameOverTimestamp, map);
        if (!exactMatches.isEmpty()) {
            return Optional.of(exactMatches.get(0));
        }
        
        // If no exact match, check for games with same map and timestamp within 1 second
        // This handles potential precision differences in database storage
        // Use a more efficient approach: query all games with the same map first
        List<GameEntity> gamesWithSameMap = findAll().stream()
                .filter(g -> g.getMap() != null && g.getMap().equals(map))
                .collect(java.util.stream.Collectors.toList());
        
        for (GameEntity game : gamesWithSameMap) {
            if (game.getGameOverTimestamp() != null) {
                long secondsDiff = Math.abs(
                    game.getGameOverTimestamp().getEpochSecond() - 
                    gameOverTimestamp.getEpochSecond()
                );
                if (secondsDiff <= 1) {
                    return Optional.of(game);
                }
            }
        }
        
        return Optional.empty();
    }
}
