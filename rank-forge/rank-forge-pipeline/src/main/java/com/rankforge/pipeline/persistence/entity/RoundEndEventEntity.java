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
 * Entity for RoundEnd events
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@DiscriminatorValue("ROUND_END")
public class RoundEndEventEntity extends GameEventEntity {
    
    @jakarta.persistence.Column(name = "players", columnDefinition = "NVARCHAR(MAX)")
    private String playersJson; // Store players list as JSON
    
    public RoundEndEventEntity() {
        super();
    }
    
    public RoundEndEventEntity(Instant timestamp) {
        super(timestamp, GameEventType.ROUND_END);
    }
    
    public String getPlayersJson() {
        return playersJson;
    }
    
    public void setPlayersJson(String playersJson) {
        this.playersJson = playersJson;
    }
}
