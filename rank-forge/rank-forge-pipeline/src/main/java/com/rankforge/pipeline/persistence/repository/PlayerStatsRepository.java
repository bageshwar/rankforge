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

import java.time.Instant;
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
    
    /**
     * Find the latest player stats by nickname (case-insensitive)
     * Used for resolving player names to Steam IDs
     */
    @Query("SELECT p FROM PlayerStatsEntity p WHERE LOWER(p.lastSeenNickname) = LOWER(:nickname) ORDER BY p.gameTimestamp DESC")
    List<PlayerStatsEntity> findByLastSeenNicknameIgnoreCaseOrderByGameTimestampDesc(@Param("nickname") String nickname);
    
    /**
     * Find the latest player stats by nickname (single result)
     */
    default Optional<PlayerStatsEntity> findByLastSeenNickname(String nickname) {
        List<PlayerStatsEntity> results = findByLastSeenNicknameIgnoreCaseOrderByGameTimestampDesc(nickname);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    /**
     * Find all player stats entries for a specific game timestamp.
     * Used for cascade deletion when a game is deleted.
     * 
     * @param gameTimestamp The game timestamp to match
     * @return List of PlayerStatsEntity entries for that game
     * @deprecated Use findByGameId instead for more confident deletion
     */
    @Deprecated
    @Query("SELECT p FROM PlayerStatsEntity p WHERE p.gameTimestamp = :gameTimestamp")
    List<PlayerStatsEntity> findByGameTimestamp(@Param("gameTimestamp") Instant gameTimestamp);
    
    /**
     * Find all player stats entries for a specific game by game ID.
     * Used for cascade deletion when a game is deleted.
     * This is more reliable than timestamp-based lookup.
     * 
     * @param gameId The game ID to match
     * @return List of PlayerStatsEntity entries for that game
     */
    @Query("SELECT p FROM PlayerStatsEntity p WHERE p.game.id = :gameId")
    List<PlayerStatsEntity> findByGameId(@Param("gameId") Long gameId);
    
    /**
     * Find all player stats records within a specific month range (inclusive)
     * @param startOfMonth Start of month (00:00:00 UTC on first day)
     * @param endOfMonth End of month (23:59:59 UTC on last day)
     * @return List of all PlayerStatsEntity records within the month range
     */
    @Query("SELECT p FROM PlayerStatsEntity p WHERE p.gameTimestamp >= :startOfMonth AND p.gameTimestamp <= :endOfMonth ORDER BY p.gameTimestamp ASC, p.lastUpdated ASC")
    List<PlayerStatsEntity> findStatsByMonthRange(@Param("startOfMonth") Instant startOfMonth, @Param("endOfMonth") Instant endOfMonth);
    
    /**
     * Find the latest player stats for a specific player before a given date
     * Used to get baseline stats before a month starts for computing month-only stats
     * @param playerId The player ID
     * @param beforeDate The date before which to find stats
     * @return Optional containing the latest PlayerStatsEntity before the date, or empty if none found
     */
    @Query("SELECT p FROM PlayerStatsEntity p WHERE p.playerId = :playerId AND p.gameTimestamp < :beforeDate ORDER BY p.gameTimestamp DESC, p.lastUpdated DESC")
    List<PlayerStatsEntity> findLatestStatsBeforeDate(@Param("playerId") String playerId, @Param("beforeDate") Instant beforeDate);
    
    /**
     * Get the earliest game timestamp in the database
     * Used to determine the first month available for leaderboard queries
     * @return Optional containing the earliest gameTimestamp, or empty if no records exist
     */
    @Query("SELECT MIN(p.gameTimestamp) FROM PlayerStatsEntity p")
    Optional<Instant> findEarliestGameTimestamp();
    
    /**
     * Count distinct games played by a player (count distinct gameTimestamps)
     * @param playerId The player ID
     * @return Number of distinct games played
     */
    @Query("SELECT COUNT(DISTINCT p.gameTimestamp) FROM PlayerStatsEntity p WHERE p.playerId = :playerId")
    long countDistinctGamesByPlayerId(@Param("playerId") String playerId);
    
    /**
     * Count distinct games played by a player within a month range
     * @param playerId The player ID
     * @param startOfMonth Start of month (00:00:00 UTC on first day)
     * @param endOfMonth End of month (23:59:59 UTC on last day)
     * @return Number of distinct games played in the month
     */
    @Query("SELECT COUNT(DISTINCT p.gameTimestamp) FROM PlayerStatsEntity p WHERE p.playerId = :playerId AND p.gameTimestamp >= :startOfMonth AND p.gameTimestamp <= :endOfMonth")
    long countDistinctGamesByPlayerIdInMonth(@Param("playerId") String playerId, @Param("startOfMonth") Instant startOfMonth, @Param("endOfMonth") Instant endOfMonth);
    
    /**
     * Count total distinct games across all players (all-time)
     * @return Total number of distinct games
     */
    @Query("SELECT COUNT(DISTINCT p.gameTimestamp) FROM PlayerStatsEntity p")
    long countTotalDistinctGames();
    
    /**
     * Count total distinct games within a month range
     * @param startOfMonth Start of month (00:00:00 UTC on first day)
     * @param endOfMonth End of month (23:59:59 UTC on last day)
     * @return Total number of distinct games in the month
     */
    @Query("SELECT COUNT(DISTINCT p.gameTimestamp) FROM PlayerStatsEntity p WHERE p.gameTimestamp >= :startOfMonth AND p.gameTimestamp <= :endOfMonth")
    long countTotalDistinctGamesInMonth(@Param("startOfMonth") Instant startOfMonth, @Param("endOfMonth") Instant endOfMonth);
    
    /**
     * Count distinct games played by multiple players within a month range (batch query)
     * Returns a map of playerId -> game count to avoid N+1 query problem
     * @param playerIds List of player IDs to count games for
     * @param startOfMonth Start of month (00:00:00 UTC on first day)
     * @param endOfMonth End of month (23:59:59 UTC on last day)
     * @return List of Object arrays where [0] = playerId (String), [1] = gameCount (Long)
     */
    @Query("SELECT p.playerId, COUNT(DISTINCT p.gameTimestamp) FROM PlayerStatsEntity p " +
           "WHERE p.playerId IN :playerIds AND p.gameTimestamp >= :startOfMonth AND p.gameTimestamp <= :endOfMonth " +
           "GROUP BY p.playerId")
    List<Object[]> countDistinctGamesByPlayerIdsInMonth(
            @Param("playerIds") List<String> playerIds,
            @Param("startOfMonth") Instant startOfMonth,
            @Param("endOfMonth") Instant endOfMonth);
}
