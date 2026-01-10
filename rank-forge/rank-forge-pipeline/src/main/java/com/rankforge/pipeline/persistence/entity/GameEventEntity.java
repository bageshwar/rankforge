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
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Base entity for game events using Single Table Inheritance
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "GameEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "gameEventType", discriminatorType = DiscriminatorType.STRING)
public abstract class GameEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "at", nullable = false)
    private Instant timestamp;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "gameEventType", nullable = false, insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private GameEventType gameEventType;
    
    @Column(name = "player1", length = 255)
    private String player1;
    
    @Column(name = "player2", length = 255)
    private String player2;
    
    // Managed relationship to GameEntity - FK set before batch persist
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameId")
    private GameEntity game;
    
    // Managed relationship to RoundStartEventEntity - FK set before batch persist
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roundStartEventId")
    private RoundStartEventEntity roundStart;
    
    // Default constructor
    public GameEventEntity() {
        this.createdAt = Instant.now();
    }
    
    public GameEventEntity(Instant timestamp, GameEventType gameEventType) {
        this.timestamp = timestamp;
        this.gameEventType = gameEventType;
        this.createdAt = Instant.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public GameEventType getGameEventType() {
        return gameEventType;
    }
    
    public void setGameEventType(GameEventType gameEventType) {
        this.gameEventType = gameEventType;
    }
    
    public String getPlayer1() {
        return player1;
    }
    
    public void setPlayer1(String player1) {
        this.player1 = player1;
    }
    
    public String getPlayer2() {
        return player2;
    }
    
    public void setPlayer2(String player2) {
        this.player2 = player2;
    }
    
    public GameEntity getGame() {
        return game;
    }
    
    public void setGame(GameEntity game) {
        this.game = game;
    }
    
    public RoundStartEventEntity getRoundStart() {
        return roundStart;
    }
    
    public void setRoundStart(RoundStartEventEntity roundStart) {
        this.roundStart = roundStart;
    }
}
