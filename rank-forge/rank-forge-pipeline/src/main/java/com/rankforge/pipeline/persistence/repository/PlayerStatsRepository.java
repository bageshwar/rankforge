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

import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PlayerStats entities
 * Supports historical progression tracking - stores multiple records per player over time
 * Author bageshwar.pn
 * Date 2026
 */
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStatsEntity, Long> {
    
    /**
     * Find the latest player stats by player ID (most recent gameTimestamp)
     * Used for getting current player stats
     */
    @Query("SELECT p FROM PlayerStatsEntity p WHERE p.playerId = :playerId ORDER BY p.gameTimestamp DESC, p.lastUpdated DESC")
    List<PlayerStatsEntity> findByPlayerIdOrderByGameTimestampDesc(@Param("playerId") String playerId);
    
    /**
     * Find the latest player stats by player ID (single result)
     */
    default Optional<PlayerStatsEntity> findByPlayerId(String playerId) {
        List<PlayerStatsEntity> results = findByPlayerIdOrderByGameTimestampDesc(playerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Find all historical records for a player, ordered by game timestamp (for progression tracking)
     */
    @Query("SELECT p FROM PlayerStatsEntity p WHERE p.playerId = :playerId ORDER BY p.gameTimestamp ASC, p.lastUpdated ASC")
    List<PlayerStatsEntity> findHistoryByPlayerId(@Param("playerId") String playerId);
    
    /**
     * Find the latest stats for all players (one record per player - most recent gameTimestamp)
     * Gets all records, then filters to latest per player using a subquery
     */
    @Query("SELECT p FROM PlayerStatsEntity p " +
           "WHERE p.gameTimestamp = (" +
           "  SELECT MAX(p2.gameTimestamp) FROM PlayerStatsEntity p2 WHERE p2.playerId = p.playerId" +
           ") " +
           "AND p.id = (" +
           "  SELECT MAX(p3.id) FROM PlayerStatsEntity p3 " +
           "  WHERE p3.playerId = p.playerId AND p3.gameTimestamp = p.gameTimestamp" +
           ") " +
           "ORDER BY p.rank ASC")
    List<PlayerStatsEntity> findLatestStatsForAllPlayers();
    
    /**
     * Find all players ordered by rank ascending (uses latest stats)
     * @deprecated Use findLatestStatsForAllPlayers() instead for historical data
     */
    @Deprecated
    default List<PlayerStatsEntity> findAllByOrderByRankAsc() {
        return findLatestStatsForAllPlayers();
    }
}
