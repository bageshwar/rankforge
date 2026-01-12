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
 * Entity for Attack events
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@DiscriminatorValue("ATTACK")
public class AttackEventEntity extends GameEventEntity {
    
    @jakarta.persistence.Column(name = "damage")
    private Integer damage;
    
    @jakarta.persistence.Column(name = "armorDamage")
    private Integer armorDamage;
    
    @jakarta.persistence.Column(name = "hitGroup", length = 50)
    private String hitGroup;
    
    @jakarta.persistence.Column(name = "weapon", length = 255)
    private String weapon;
    
    @jakarta.persistence.Column(name = "healthRemaining")
    private Integer healthRemaining;
    
    public AttackEventEntity() {
        super();
    }
    
    public AttackEventEntity(Instant timestamp) {
        super(timestamp, GameEventType.ATTACK);
    }
    
    public Integer getDamage() {
        return damage;
    }
    
    public void setDamage(Integer damage) {
        this.damage = damage;
    }
    
    public Integer getArmorDamage() {
        return armorDamage;
    }
    
    public void setArmorDamage(Integer armorDamage) {
        this.armorDamage = armorDamage;
    }
    
    public String getHitGroup() {
        return hitGroup;
    }
    
    public void setHitGroup(String hitGroup) {
        this.hitGroup = hitGroup;
    }
    
    public String getWeapon() {
        return weapon;
    }
    
    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }
    
    public Integer getHealthRemaining() {
        return healthRemaining;
    }
    
    public void setHealthRemaining(Integer healthRemaining) {
        this.healthRemaining = healthRemaining;
    }
}
