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

import com.rankforge.core.events.GameEventType;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for GameEvent entities.
 * Note: Post-hoc linking methods have been removed - entity references are now set directly
 * via EventProcessingContext before persistence.
 * Author bageshwar.pn
 * Date 2026
 */
@Repository
public interface GameEventRepository extends JpaRepository<GameEventEntity, Long> {
    
    /**
     * Find all events by type
     */
    List<GameEventEntity> findByGameEventType(GameEventType type);
    
    /**
     * Find events by type and exact timestamp
     * Used for deduplication check in CS2LogParser
     */
    List<GameEventEntity> findByGameEventTypeAndTimestamp(GameEventType type, Instant timestamp);
    
    /**
     * Find events by game entity
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.game.id = :gameId AND e.gameEventType != 'GAME_OVER' ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByGameId(@Param("gameId") Long gameId);
    
    /**
     * Find events by round start entity
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.roundStart.id = :roundStartId ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByRoundStartId(@Param("roundStartId") Long roundStartId);
    
    /**
     * Find events by game ID and event type
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.game.id = :gameId AND e.gameEventType = :eventType ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByGameIdAndGameEventType(
            @Param("gameId") Long gameId,
            @Param("eventType") GameEventType eventType
    );
    
    /**
     * Find events by round start ID and event type
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.roundStart.id = :roundStartId AND e.gameEventType = :eventType ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByRoundStartIdAndGameEventType(
            @Param("roundStartId") Long roundStartId,
            @Param("eventType") GameEventType eventType
    );
    
    /**
     * Find round end events by game ID
     */
    @Query("SELECT e FROM RoundEndEventEntity e WHERE e.game.id = :gameId ORDER BY e.timestamp ASC")
    List<RoundEndEventEntity> findRoundEndEventsByGameId(@Param("gameId") Long gameId);
    
    /**
     * Find round end events for multiple games (by game IDs)
     * @param gameIds List of game IDs
     * @return List of RoundEndEventEntity for the specified games
     */
    @Query("SELECT e FROM RoundEndEventEntity e WHERE e.game.id IN :gameIds ORDER BY e.game.id ASC, e.timestamp ASC")
    List<RoundEndEventEntity> findRoundEndEventsByGameIds(@Param("gameIds") List<Long> gameIds);
}
