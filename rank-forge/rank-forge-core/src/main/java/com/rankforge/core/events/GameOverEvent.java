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

import java.time.Instant;
import java.util.Map;

/**
 * This class represents the Game Over Event (competitive)
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class GameOverEvent extends GameEvent {
    String map;
    String mode;
    int team1Score;
    int team2Score;
    Integer duration; // Duration in minutes

    // Default constructor for Jackson deserialization
    public GameOverEvent() {}

    public GameOverEvent(Instant timestamp, Map<String, String> additionalData,
                         String map, String mode, int team1Score, int team2Score) {
        super(timestamp, GameEventType.GAME_OVER, additionalData);
        this.map = map;
        this.mode = mode;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
    }

    public GameOverEvent(Instant timestamp, Map<String, String> additionalData,
                         String map, String mode, int team1Score, int team2Score, Integer duration) {
        super(timestamp, GameEventType.GAME_OVER, additionalData);
        this.map = map;
        this.mode = mode;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.duration = duration;
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

    public int getTeam1Score() {
        return team1Score;
    }

    public void setTeam1Score(int team1Score) {
        this.team1Score = team1Score;
    }

    public int getTeam2Score() {
        return team2Score;
    }

    public void setTeam2Score(int team2Score) {
        this.team2Score = team2Score;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
