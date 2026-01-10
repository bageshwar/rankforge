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
 * Interface for storing and retrieving game events.
 * All event fetching is done by ID or type+timestamp for deduplication - 
 * timestamp range scanning has been removed.
 * Author bageshwar.pn
 * Date 26/10/24
 */
public interface EventStore {
    void store(GameEvent event);

    /**
     * Get a game event by type and exact timestamp.
     * Used for deduplication check (e.g., checking if a game was already processed).
     */
    Optional<GameEvent> getGameEvent(GameEventType eventType, Instant timestamp);
    
    /**
     * Get all GameOver events to display completed games
     */
    List<GameEvent> getGameOverEvents();
}
