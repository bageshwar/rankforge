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
import org.springframework.data.jpa.repository.JpaRepository;
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
}
