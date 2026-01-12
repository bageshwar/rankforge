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

import com.rankforge.core.models.Player;

import java.time.Instant;
import java.util.Map;

/**
 * This class represents an assist event in the game.
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class AttackEvent extends GameActionEvent {

    private int damage;
    private int armorDamage;
    private String hitGroup;
    private int healthRemaining;

    // Default constructor for Jackson deserialization
    public AttackEvent() {}

    public AttackEvent(Instant timestamp, Map<String, String> additionalData, Player player1, Player player2, String weapon, String damage, String armorDamage, String hitGroup, String healthRemaining) {
        super(timestamp, GameEventType.ATTACK, additionalData, player1, player2, weapon);
        this.damage = Integer.parseInt(damage);
        this.armorDamage = Integer.parseInt(armorDamage);
        this.hitGroup = hitGroup;
        this.healthRemaining = Integer.parseInt(healthRemaining);
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getArmorDamage() {
        return armorDamage;
    }

    public void setArmorDamage(int armorDamage) {
        this.armorDamage = armorDamage;
    }

    public String getHitGroup() {
        return hitGroup;
    }

    public void setHitGroup(String hitGroup) {
        this.hitGroup = hitGroup;
    }

    public int getHealthRemaining() {
        return healthRemaining;
    }

    public void setHealthRemaining(int healthRemaining) {
        this.healthRemaining = healthRemaining;
    }
}
