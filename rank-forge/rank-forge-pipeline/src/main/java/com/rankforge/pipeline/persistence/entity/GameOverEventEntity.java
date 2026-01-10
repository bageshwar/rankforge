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
 * Entity for GameOver events
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@DiscriminatorValue("GAME_OVER")
public class GameOverEventEntity extends GameEventEntity {
    
    @jakarta.persistence.Column(name = "map", length = 255)
    private String map;
    
    @jakarta.persistence.Column(name = "mode", length = 255)
    private String mode;
    
    @jakarta.persistence.Column(name = "team1Score")
    private Integer team1Score;
    
    @jakarta.persistence.Column(name = "team2Score")
    private Integer team2Score;
    
    @jakarta.persistence.Column(name = "duration")
    private Integer duration;
    
    public GameOverEventEntity() {
        super();
    }
    
    public GameOverEventEntity(Instant timestamp) {
        super(timestamp, GameEventType.GAME_OVER);
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
}
