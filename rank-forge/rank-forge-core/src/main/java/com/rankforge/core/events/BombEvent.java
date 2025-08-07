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

package com.rankforge.core.events;

import java.time.Instant;
import java.util.Map;

/**
 * This class represents a bomb event in the game
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class BombEvent extends GameEvent {

    private String player;
    private BombEventType eventType;
    private int timeRemaining;

    // Default constructor for Jackson deserialization
    public BombEvent() {}

    public BombEvent(Instant timestamp, Map<String, String> additionalData, String player, BombEventType eventType, int timeRemaining) {
        super(timestamp, GameEventType.BOMB_EVENT, additionalData);
        this.player = player;
        this.eventType = eventType;
        this.timeRemaining = timeRemaining;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public BombEventType getEventType() {
        return eventType;
    }

    public void setEventType(BombEventType eventType) {
        this.eventType = eventType;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public enum BombEventType {
        PLANT, DEFUSE, EXPLODE
    }
}
