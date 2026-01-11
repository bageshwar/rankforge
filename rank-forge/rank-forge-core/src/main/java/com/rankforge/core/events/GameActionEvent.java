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
    
    // Player coordinates (x, y, z)
    private Integer player1X;
    private Integer player1Y;
    private Integer player1Z;
    private Integer player2X;
    private Integer player2Y;
    private Integer player2Z;

    // Default constructor for Jackson deserialization
    public GameActionEvent() {}

    public GameActionEvent(Instant timestamp, GameEventType type, Map<String, String> additionalData, Player player1, Player player2, String weapon) {
        super(timestamp, type, additionalData);
        this.player1 = player1;
        this.player2 = player2;
        this.weapon = weapon;
    }
    
    public GameActionEvent(Instant timestamp, GameEventType type, Map<String, String> additionalData, Player player1, Player player2, String weapon,
                          Integer player1X, Integer player1Y, Integer player1Z, Integer player2X, Integer player2Y, Integer player2Z) {
        super(timestamp, type, additionalData);
        this.player1 = player1;
        this.player2 = player2;
        this.weapon = weapon;
        this.player1X = player1X;
        this.player1Y = player1Y;
        this.player1Z = player1Z;
        this.player2X = player2X;
        this.player2Y = player2Y;
        this.player2Z = player2Z;
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

    public Integer getPlayer1X() {
        return player1X;
    }

    public void setPlayer1X(Integer player1X) {
        this.player1X = player1X;
    }

    public Integer getPlayer1Y() {
        return player1Y;
    }

    public void setPlayer1Y(Integer player1Y) {
        this.player1Y = player1Y;
    }

    public Integer getPlayer1Z() {
        return player1Z;
    }

    public void setPlayer1Z(Integer player1Z) {
        this.player1Z = player1Z;
    }

    public Integer getPlayer2X() {
        return player2X;
    }

    public void setPlayer2X(Integer player2X) {
        this.player2X = player2X;
    }

    public Integer getPlayer2Y() {
        return player2Y;
    }

    public void setPlayer2Y(Integer player2Y) {
        this.player2Y = player2Y;
    }

    public Integer getPlayer2Z() {
        return player2Z;
    }

    public void setPlayer2Z(Integer player2Z) {
        this.player2Z = player2Z;
    }
}
