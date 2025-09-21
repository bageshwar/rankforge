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

package com.rankforge.core.stores;

import com.rankforge.core.events.GameEvent;
import com.rankforge.core.events.GameEventType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Interface for storing and retrieving game events
 * Author bageshwar.pn
 * Date 26/10/24
 */
public interface EventStore {
    void store(GameEvent event);

    Optional<GameEvent> getGameEvent(GameEventType eventType, Instant timestamp);
    
    /**
     * Get all GameOver events to display completed games
     */
    List<GameEvent> getGameOverEvents();
    
    /**
     * Get all events of a specific type between two timestamps
     * @param eventType The type of events to retrieve
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @return List of events within the time range
     */
    List<GameEvent> getEventsBetween(GameEventType eventType, Instant startTime, Instant endTime);
    
    /**
     * Get all round end events between game start and end times
     * This is used to determine players who participated in a game
     * @param gameStartTime When the game started
     * @param gameEndTime When the game ended
     * @return List of RoundEndEvent instances for the game
     */
    List<GameEvent> getRoundEndEventsBetween(Instant gameStartTime, Instant gameEndTime);
}
