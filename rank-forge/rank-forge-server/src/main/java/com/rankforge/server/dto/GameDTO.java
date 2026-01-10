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
import java.util.List;

/**
 * DTO for displaying game information.
 * Uses the Game table's database ID as the unique identifier.
 * Author bageshwar.pn
 * Date 2024
 */
public class GameDTO {
    private Long id;  // Database ID from Game table
    private Instant gameDate;
    private String map;
    private String mode;
    private int team1Score;
    private int team2Score;
    private List<String> players;
    private String duration;

    // Constructors
    public GameDTO() {}

    public GameDTO(Long id, Instant gameDate, String map, String mode, 
                   int team1Score, int team2Score, List<String> players, String duration) {
        this.id = id;
        this.gameDate = gameDate;
        this.map = map;
        this.mode = mode;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        this.players = players;
        this.duration = duration;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getGameDate() {
        return gameDate;
    }

    public void setGameDate(Instant gameDate) {
        this.gameDate = gameDate;
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

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getScore() {
        return team1Score + " - " + team2Score;
    }

    public String getFormattedDuration() {
        if (duration != null && !duration.isEmpty()) {
            return duration + " min";
        }
        return "Unknown";
    }
}
