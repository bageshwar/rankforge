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
 * Entity for Games
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "Game")
public class GameEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
    @SequenceGenerator(name = "game_seq", sequenceName = "game_seq", allocationSize = 50)
    private Long id;
    
    @Column(name = "gameOverTimestamp", nullable = false)
    private Instant gameOverTimestamp;
    
    @Column(name = "map", length = 255)
    private String map;
    
    @Column(name = "mode", length = 255)
    private String mode;
    
    @Column(name = "team1Score", nullable = false)
    private Integer team1Score;
    
    @Column(name = "team2Score", nullable = false)
    private Integer team2Score;
    
    @Column(name = "duration")
    private Integer duration; // Duration in minutes
    
    @Column(name = "startTime")
    private Instant startTime;
    
    @Column(name = "endTime", nullable = false)
    private Instant endTime;
    
    @Column(name = "createdAt")
    private Instant createdAt;
    
    @Column(name = "gameOverEventId")
    private Long gameOverEventId; // Foreign key to GameEventEntity (GAME_OVER event)
    
    // Default constructor
    public GameEntity() {
        this.createdAt = Instant.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Instant getGameOverTimestamp() {
        return gameOverTimestamp;
    }
    
    public void setGameOverTimestamp(Instant gameOverTimestamp) {
        this.gameOverTimestamp = gameOverTimestamp;
    }
    
    public String getMap() {
        return map;
    }
    
    public void setMap(String map) {
        this.map = map;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public Integer getTeam1Score() {
        return team1Score;
    }
    
    public void setTeam1Score(Integer team1Score) {
        this.team1Score = team1Score;
    }
    
    public Integer getTeam2Score() {
        return team2Score;
    }
    
    public void setTeam2Score(Integer team2Score) {
        this.team2Score = team2Score;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getGameOverEventId() {
        return gameOverEventId;
    }
    
    public void setGameOverEventId(Long gameOverEventId) {
        this.gameOverEventId = gameOverEventId;
    }
}
