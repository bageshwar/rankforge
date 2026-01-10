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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for GameEvent entities
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
     * Find events by type and timestamp range
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.gameEventType = :type AND e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByGameEventTypeAndTimestampBetween(
            @Param("type") GameEventType type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
    
    /**
     * Find events by timestamp range
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByTimestampBetween(
            @Param("start") Instant start,
            @Param("end") Instant end
    );
    
    /**
     * Find events by type and exact timestamp
     */
    List<GameEventEntity> findByGameEventTypeAndTimestamp(GameEventType type, Instant timestamp);
    
    /**
     * Find events by game ID
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.gameId = :gameId AND e.gameEventType != 'GAME_OVER' ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByGameId(@Param("gameId") Long gameId);
    
    /**
     * Find events by round ID
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.roundId = :roundId ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByRoundId(@Param("roundId") Long roundId);
    
    /**
     * Find events by game ID and event type
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.gameId = :gameId AND e.gameEventType = :eventType ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByGameIdAndGameEventType(
            @Param("gameId") Long gameId,
            @Param("eventType") GameEventType eventType
    );
    
    /**
     * Find events by round ID and event type
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.roundId = :roundId AND e.gameEventType = :eventType ORDER BY e.timestamp ASC")
    List<GameEventEntity> findByRoundIdAndGameEventType(
            @Param("roundId") Long roundId,
            @Param("eventType") GameEventType eventType
    );
    
    /**
     * Find round end events by game ID
     */
    @Query("SELECT e FROM RoundEndEventEntity e WHERE e.gameId = :gameId ORDER BY e.timestamp ASC")
    List<RoundEndEventEntity> findRoundEndEventsByGameId(@Param("gameId") Long gameId);
    
    /**
     * Find events between timestamps
     */
    @Query("SELECT e FROM GameEventEntity e WHERE e.timestamp BETWEEN :startTime AND :endTime ORDER BY e.timestamp ASC")
    List<GameEventEntity> findEventsBetweenTimestamps(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    
    /**
     * Bulk update: Set gameId for multiple events (excluding GAME_OVER)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GameEventEntity e SET e.gameId = :gameId WHERE e.id IN :eventIds AND e.gameEventType != 'GAME_OVER'")
    int updateEventsGameId(@Param("gameId") Long gameId, @Param("eventIds") List<Long> eventIds);
    
    /**
     * Bulk update: Set roundId for multiple events
     * Only updates events that don't already have a roundId (to prevent overwriting)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GameEventEntity e SET e.roundId = :roundId WHERE e.id IN :eventIds AND e.roundId IS NULL")
    int updateEventsRoundId(@Param("roundId") Long roundId, @Param("eventIds") List<Long> eventIds);
    
    /**
     * Bulk update: Set gameId for multiple round end events
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RoundEndEventEntity r SET r.gameId = :gameId WHERE r.id IN :roundIds")
    int updateRoundsGameId(@Param("gameId") Long gameId, @Param("roundIds") List<Long> roundIds);
}
