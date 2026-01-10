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
 * Entity for Kill events
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@DiscriminatorValue("KILL")
public class KillEventEntity extends GameEventEntity {
    
    @jakarta.persistence.Column(name = "isHeadshot")
    private Boolean isHeadshot;
    
    @jakarta.persistence.Column(name = "weapon", length = 255)
    private String weapon;
    
    public KillEventEntity() {
        super();
    }
    
    public KillEventEntity(Instant timestamp) {
        super(timestamp, GameEventType.KILL);
    }
    
    public Boolean getIsHeadshot() {
        return isHeadshot;
    }
    
    public void setIsHeadshot(Boolean isHeadshot) {
        this.isHeadshot = isHeadshot;
    }
    
    public String getWeapon() {
        return weapon;
    }
    
    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }
}
