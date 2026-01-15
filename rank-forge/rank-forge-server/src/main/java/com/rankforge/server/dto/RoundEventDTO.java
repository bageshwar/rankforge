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

package com.rankforge.server.dto;

import java.time.Instant;

/**
 * DTO for individual round events
 * Author bageshwar.pn
 * Date 2026
 */
public class RoundEventDTO {
    
    private Long id;
    private String eventType;  // KILL, ASSIST, BOMB_EVENT, etc.
    private Instant timestamp;
    private long timeOffsetMs;  // Milliseconds since round start
    
    // Players involved
    private String player1Id;
    private String player1Name;
    private String player1Team;  // "CT" or "T"
    private String player2Id;
    private String player2Name;
    private String player2Team;  // "CT" or "T"
    
    // Event-specific details
    private String weapon;
    private Boolean isHeadshot;
    private Integer damage;
    private Integer armorDamage;
    private String hitGroup;
    private String bombEventType;  // planted, defused, exploded, etc.
    private String assistType;     // flash_assist, etc.
    
    public RoundEventDTO() {}
    
    public RoundEventDTO(Long id, String eventType, Instant timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getTimeOffsetMs() {
        return timeOffsetMs;
    }
    
    public void setTimeOffsetMs(long timeOffsetMs) {
        this.timeOffsetMs = timeOffsetMs;
    }
    
    public String getPlayer1Id() {
        return player1Id;
    }
    
    public void setPlayer1Id(String player1Id) {
        this.player1Id = player1Id;
    }
    
    public String getPlayer1Name() {
        return player1Name;
    }
    
    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }
    
    public String getPlayer1Team() {
        return player1Team;
    }
    
    public void setPlayer1Team(String player1Team) {
        this.player1Team = player1Team;
    }
    
    public String getPlayer2Id() {
        return player2Id;
    }
    
    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
    }
    
    public String getPlayer2Name() {
        return player2Name;
    }
    
    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }
    
    public String getPlayer2Team() {
        return player2Team;
    }
    
    public void setPlayer2Team(String player2Team) {
        this.player2Team = player2Team;
    }
    
    public String getWeapon() {
        return weapon;
    }
    
    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }
    
    public Boolean getIsHeadshot() {
        return isHeadshot;
    }
    
    public void setIsHeadshot(Boolean isHeadshot) {
        this.isHeadshot = isHeadshot;
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
    
    public String getBombEventType() {
        return bombEventType;
    }
    
    public void setBombEventType(String bombEventType) {
        this.bombEventType = bombEventType;
    }
    
    public String getAssistType() {
        return assistType;
    }
    
    public void setAssistType(String assistType) {
        this.assistType = assistType;
    }
}
