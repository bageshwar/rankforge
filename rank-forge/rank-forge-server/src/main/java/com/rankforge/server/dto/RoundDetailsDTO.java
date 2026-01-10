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
 * DTO for round details including all events
 * Author bageshwar.pn
 * Date 2026
 */
public class RoundDetailsDTO {
    
    private Long gameId;
    private int roundNumber;
    private String winnerTeam;  // CT or T
    private Instant roundStartTime;
    private Instant roundEndTime;
    private long durationMs;
    
    private List<RoundEventDTO> events;
    
    // Summary stats for this round
    private int totalKills;
    private int totalAssists;
    private int headshotKills;
    private boolean bombPlanted;
    private boolean bombDefused;
    private boolean bombExploded;
    
    public RoundDetailsDTO() {}
    
    public RoundDetailsDTO(Long gameId, int roundNumber, String winnerTeam) {
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.winnerTeam = winnerTeam;
    }
    
    // Getters and Setters
    public Long getGameId() {
        return gameId;
    }
    
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    public String getWinnerTeam() {
        return winnerTeam;
    }
    
    public void setWinnerTeam(String winnerTeam) {
        this.winnerTeam = winnerTeam;
    }
    
    public Instant getRoundStartTime() {
        return roundStartTime;
    }
    
    public void setRoundStartTime(Instant roundStartTime) {
        this.roundStartTime = roundStartTime;
    }
    
    public Instant getRoundEndTime() {
        return roundEndTime;
    }
    
    public void setRoundEndTime(Instant roundEndTime) {
        this.roundEndTime = roundEndTime;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public List<RoundEventDTO> getEvents() {
        return events;
    }
    
    public void setEvents(List<RoundEventDTO> events) {
        this.events = events;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }
    
    public int getTotalAssists() {
        return totalAssists;
    }
    
    public void setTotalAssists(int totalAssists) {
        this.totalAssists = totalAssists;
    }
    
    public int getHeadshotKills() {
        return headshotKills;
    }
    
    public void setHeadshotKills(int headshotKills) {
        this.headshotKills = headshotKills;
    }
    
    public boolean isBombPlanted() {
        return bombPlanted;
    }
    
    public void setBombPlanted(boolean bombPlanted) {
        this.bombPlanted = bombPlanted;
    }
    
    public boolean isBombDefused() {
        return bombDefused;
    }
    
    public void setBombDefused(boolean bombDefused) {
        this.bombDefused = bombDefused;
    }
    
    public boolean isBombExploded() {
        return bombExploded;
    }
    
    public void setBombExploded(boolean bombExploded) {
        this.bombExploded = bombExploded;
    }
}
