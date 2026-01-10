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
 * Entity for Accolades
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "Accolade")
public class AccoladeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "gameTimestamp", nullable = false)
    private Instant gameTimestamp;
    
    @Column(name = "type", nullable = false, length = 255)
    private String type;
    
    @Column(name = "playerName", nullable = false, length = 255)
    private String playerName;
    
    @Column(name = "playerId", length = 255)
    private String playerId;
    
    @Column(name = "value", nullable = false)
    private Double value;
    
    @Column(name = "position", nullable = false)
    private Integer position;
    
    @Column(name = "score", nullable = false)
    private Double score;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "gameId")
    private Long gameId; // Foreign key to GameEntity
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameId", insertable = false, updatable = false)
    private GameEntity game;
    
    // Default constructor
    public AccoladeEntity() {
        this.createdAt = Instant.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Instant getGameTimestamp() {
        return gameTimestamp;
    }
    
    public void setGameTimestamp(Instant gameTimestamp) {
        this.gameTimestamp = gameTimestamp;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getGameId() {
        return gameId;
    }
    
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
    
    public GameEntity getGame() {
        return game;
    }
    
    public void setGame(GameEntity game) {
        this.game = game;
    }
}
