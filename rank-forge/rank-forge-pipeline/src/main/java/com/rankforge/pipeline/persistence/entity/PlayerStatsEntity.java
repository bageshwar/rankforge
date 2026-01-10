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

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity for PlayerStats
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "PlayerStats")
public class PlayerStatsEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "playerId", nullable = false, unique = true, length = 255)
    private String playerId;
    
    @Column(name = "kills", nullable = false)
    private Integer kills = 0;
    
    @Column(name = "deaths", nullable = false)
    private Integer deaths = 0;
    
    @Column(name = "assists", nullable = false)
    private Integer assists = 0;
    
    @Column(name = "headshotKills", nullable = false)
    private Integer headshotKills = 0;
    
    @Column(name = "roundsPlayed", nullable = false)
    private Integer roundsPlayed = 0;
    
    @Column(name = "clutchesWon", nullable = false)
    private Integer clutchesWon = 0;
    
    @Column(name = "damageDealt", nullable = false)
    private Double damageDealt = 0.0;
    
    @Column(name = "lastUpdated")
    private Instant lastUpdated;
    
    @Column(name = "rank", nullable = false)
    private Integer rank = 0;
    
    @Column(name = "lastSeenNickname", length = 255)
    private String lastSeenNickname;
    
    @Column(name = "createdAt")
    private Instant createdAt;
    
    // Default constructor
    public PlayerStatsEntity() {
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public Integer getKills() {
        return kills;
    }
    
    public void setKills(Integer kills) {
        this.kills = kills;
    }
    
    public Integer getDeaths() {
        return deaths;
    }
    
    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }
    
    public Integer getAssists() {
        return assists;
    }
    
    public void setAssists(Integer assists) {
        this.assists = assists;
    }
    
    public Integer getHeadshotKills() {
        return headshotKills;
    }
    
    public void setHeadshotKills(Integer headshotKills) {
        this.headshotKills = headshotKills;
    }
    
    public Integer getRoundsPlayed() {
        return roundsPlayed;
    }
    
    public void setRoundsPlayed(Integer roundsPlayed) {
        this.roundsPlayed = roundsPlayed;
    }
    
    public Integer getClutchesWon() {
        return clutchesWon;
    }
    
    public void setClutchesWon(Integer clutchesWon) {
        this.clutchesWon = clutchesWon;
    }
    
    public Double getDamageDealt() {
        return damageDealt;
    }
    
    public void setDamageDealt(Double damageDealt) {
        this.damageDealt = damageDealt;
    }
    
    public Instant getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public String getLastSeenNickname() {
        return lastSeenNickname;
    }
    
    public void setLastSeenNickname(String lastSeenNickname) {
        this.lastSeenNickname = lastSeenNickname;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
