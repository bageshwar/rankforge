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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rankforge.core.models.Player;

import java.time.Instant;
import java.util.Map;

/**
 * This is a base class of game events involving 2 players (kill/assist/attack)
 * Author bageshwar.pn
 * Date 26/10/24
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AssistEvent.class),
        @JsonSubTypes.Type(value = KillEvent.class),
        @JsonSubTypes.Type(value = AttackEvent.class)
})
public abstract class GameActionEvent extends GameEvent {
    private Player player1;
    private Player player2;
    private String weapon;

    // Default constructor for Jackson deserialization
    public GameActionEvent() {}

    public GameActionEvent(Instant timestamp, GameEventType type, Map<String, String> additionalData, Player player1, Player player2, String weapon) {
        super(timestamp, type, additionalData);
        this.player1 = player1;
        this.player2 = player2;
        this.weapon = weapon;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public String getWeapon() {
        return weapon;
    }

    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }
}
