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

package com.rankforge.pipeline.persistence.entity;

import com.rankforge.core.events.GameEventType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Instant;

/**
 * Entity for Bomb events
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@DiscriminatorValue("BOMB_EVENT")
public class BombEventEntity extends GameEventEntity {
    
    @jakarta.persistence.Column(name = "bombPlayer", length = 255)
    private String player;
    
    @jakarta.persistence.Column(name = "bombEventType", length = 50)
    private String eventType;
    
    @jakarta.persistence.Column(name = "timeRemaining")
    private Integer timeRemaining;
    
    public BombEventEntity() {
        super();
    }
    
    public BombEventEntity(Instant timestamp) {
        super(timestamp, GameEventType.BOMB_EVENT);
    }
    
    public String getPlayer() {
        return player;
    }
    
    public void setPlayer(String player) {
        this.player = player;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Integer getTimeRemaining() {
        return timeRemaining;
    }
    
    public void setTimeRemaining(Integer timeRemaining) {
        this.timeRemaining = timeRemaining;
    }
}
