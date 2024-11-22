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

import java.time.Instant;
import java.util.List;

/**
 * Interface for storing and retrieving game events
 * Author bageshwar.pn
 * Date 26/10/24
 */
public interface EventStore {
    void store(GameEvent event);

    List<GameEvent> getEventsBetween(Instant start, Instant end);

    List<GameEvent> getEventsForPlayer(String playerId, Instant start, Instant end);

    void cleanup(Instant before);
}
