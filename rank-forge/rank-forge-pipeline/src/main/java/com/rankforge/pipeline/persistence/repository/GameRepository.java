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
     * Calculate total rounds across all games
     * Sum of (team1Score + team2Score) for all games
     * @return Total number of rounds across all games
     */
    @Query("SELECT COALESCE(SUM(g.team1Score + g.team2Score), 0) FROM GameEntity g")
    long calculateTotalRounds();
    
    /**
     * Calculate total rounds for games within a month range
     * @param startOfMonth Start of month (00:00:00 UTC on first day)
     * @param endOfMonth End of month (23:59:59 UTC on last day)
     * @return Total number of rounds in games within the month
     */
    @Query("SELECT COALESCE(SUM(g.team1Score + g.team2Score), 0) FROM GameEntity g WHERE g.gameOverTimestamp >= :startOfMonth AND g.gameOverTimestamp <= :endOfMonth")
    long calculateTotalRoundsInMonth(@Param("startOfMonth") Instant startOfMonth, @Param("endOfMonth") Instant endOfMonth);
    
    /**
     * Find all games within a month range
     * @param startOfMonth Start of month (00:00:00 UTC on first day)
     * @param endOfMonth End of month (23:59:59 UTC on last day)
     * @return List of GameEntity records within the month
     */
    @Query("SELECT g FROM GameEntity g WHERE g.gameOverTimestamp >= :startOfMonth AND g.gameOverTimestamp <= :endOfMonth ORDER BY g.gameOverTimestamp ASC")
    List<GameEntity> findGamesByMonthRange(@Param("startOfMonth") Instant startOfMonth, @Param("endOfMonth") Instant endOfMonth);
}
