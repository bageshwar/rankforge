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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single game event from the CS2 server logs
 * Author bageshwar.pn
 * Date 26/10/24
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GameActionEvent.class),
        @JsonSubTypes.Type(value = RoundStartEvent.class),
        @JsonSubTypes.Type(value = RoundEndEvent.class),
        @JsonSubTypes.Type(value = GameOverEvent.class),
        @JsonSubTypes.Type(value = BombEvent.class)
})
public abstract class GameEvent {

    private Instant timestamp;
    private GameEventType gameEventType;

    private Map<String, String> additionalData;


    // for deserialization
    public GameEvent() {}

    public GameEvent(Instant timestamp, GameEventType gameEventType, Map<String, String> additionalData) {
        this.timestamp = timestamp;
        this.gameEventType = gameEventType;
        this.additionalData = additionalData;
    }

    public GameEventType type() {
        return gameEventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public GameEventType getGameEventType() {
        return gameEventType;
    }

    public void setGameEventType(GameEventType gameEventType) {
        this.gameEventType = gameEventType;
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }
}

