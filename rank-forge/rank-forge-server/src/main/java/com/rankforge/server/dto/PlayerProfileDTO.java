/*
 *
 *  *Copyright [2026] [Bageshwar Pratap Narain]
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

import java.util.List;
import java.util.Map;

/**
 * DTO for player profile with complete stats, rating history, and accolades
 * Author bageshwar.pn
 * Date 2026
 */
public class PlayerProfileDTO {
    
    // Basic player info
    private String playerId;
    private String playerName;
    private int currentRank;
    
    // Current stats
    private int totalKills;
    private int totalDeaths;
    private int totalAssists;
    private double killDeathRatio;
    private int headshotKills;
    private double headshotPercentage;
    private int totalRoundsPlayed;
    private int clutchesWon;
    private double totalDamageDealt;
    private int totalGamesPlayed;
    
    // Rating history over time (for chart)
    private List<RatingHistoryPoint> ratingHistory;
    
    // Accolades
    private List<PlayerAccoladeDTO> accolades;
    private Map<String, Integer> accoladesByType; // Type -> count
    private String mostFrequentAccolade;
    private int totalAccolades;
    
    public PlayerProfileDTO() {}
    
    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getCurrentRank() {
        return currentRank;
    }
    
    public void setCurrentRank(int currentRank) {
        this.currentRank = currentRank;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }
    
    public int getTotalDeaths() {
        return totalDeaths;
    }
    
    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }
    
    public int getTotalAssists() {
        return totalAssists;
    }
    
    public void setTotalAssists(int totalAssists) {
        this.totalAssists = totalAssists;
    }
    
    public double getKillDeathRatio() {
        return killDeathRatio;
    }
    
    public void setKillDeathRatio(double killDeathRatio) {
        this.killDeathRatio = killDeathRatio;
    }
    
    public int getHeadshotKills() {
        return headshotKills;
    }
    
    public void setHeadshotKills(int headshotKills) {
        this.headshotKills = headshotKills;
    }
    
    public double getHeadshotPercentage() {
        return headshotPercentage;
    }
    
    public void setHeadshotPercentage(double headshotPercentage) {
        this.headshotPercentage = headshotPercentage;
    }
    
    public int getTotalRoundsPlayed() {
        return totalRoundsPlayed;
    }
    
    public void setTotalRoundsPlayed(int totalRoundsPlayed) {
        this.totalRoundsPlayed = totalRoundsPlayed;
    }
    
    public int getClutchesWon() {
        return clutchesWon;
    }
    
    public void setClutchesWon(int clutchesWon) {
        this.clutchesWon = clutchesWon;
    }
    
    public double getTotalDamageDealt() {
        return totalDamageDealt;
    }
    
    public void setTotalDamageDealt(double totalDamageDealt) {
        this.totalDamageDealt = totalDamageDealt;
    }
    
    public int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }
    
    public void setTotalGamesPlayed(int totalGamesPlayed) {
        this.totalGamesPlayed = totalGamesPlayed;
    }
    
    public List<RatingHistoryPoint> getRatingHistory() {
        return ratingHistory;
    }
    
    public void setRatingHistory(List<RatingHistoryPoint> ratingHistory) {
        this.ratingHistory = ratingHistory;
    }
    
    public List<PlayerAccoladeDTO> getAccolades() {
        return accolades;
    }
    
    public void setAccolades(List<PlayerAccoladeDTO> accolades) {
        this.accolades = accolades;
    }
    
    public Map<String, Integer> getAccoladesByType() {
        return accoladesByType;
    }
    
    public void setAccoladesByType(Map<String, Integer> accoladesByType) {
        this.accoladesByType = accoladesByType;
    }
    
    public String getMostFrequentAccolade() {
        return mostFrequentAccolade;
    }
    
    public void setMostFrequentAccolade(String mostFrequentAccolade) {
        this.mostFrequentAccolade = mostFrequentAccolade;
    }
    
    public int getTotalAccolades() {
        return totalAccolades;
    }
    
    public void setTotalAccolades(int totalAccolades) {
        this.totalAccolades = totalAccolades;
    }
    
    /**
     * Represents a point in rating history for charting
     */
    public static class RatingHistoryPoint {
        private String gameDate;
        private int rank;
        private double killDeathRatio;
        private int kills;
        private int deaths;
        private int assists;
        private int gameNumber;
        
        public RatingHistoryPoint() {}
        
        public RatingHistoryPoint(String gameDate, int rank, double killDeathRatio, 
                                   int kills, int deaths, int assists, int gameNumber) {
            this.gameDate = gameDate;
            this.rank = rank;
            this.killDeathRatio = killDeathRatio;
            this.kills = kills;
            this.deaths = deaths;
            this.assists = assists;
            this.gameNumber = gameNumber;
        }
        
        public String getGameDate() {
            return gameDate;
        }
        
        public void setGameDate(String gameDate) {
            this.gameDate = gameDate;
        }
        
        public int getRank() {
            return rank;
        }
        
        public void setRank(int rank) {
            this.rank = rank;
        }
        
        public double getKillDeathRatio() {
            return killDeathRatio;
        }
        
        public void setKillDeathRatio(double killDeathRatio) {
            this.killDeathRatio = killDeathRatio;
        }
        
        public int getKills() {
            return kills;
        }
        
        public void setKills(int kills) {
            this.kills = kills;
        }
        
        public int getDeaths() {
            return deaths;
        }
        
        public void setDeaths(int deaths) {
            this.deaths = deaths;
        }
        
        public int getAssists() {
            return assists;
        }
        
        public void setAssists(int assists) {
            this.assists = assists;
        }
        
        public int getGameNumber() {
            return gameNumber;
        }
        
        public void setGameNumber(int gameNumber) {
            this.gameNumber = gameNumber;
        }
    }
    
    /**
     * DTO for player-specific accolade
     */
    public static class PlayerAccoladeDTO {
        private String type;
        private String typeDescription;
        private double value;
        private int position;
        private double score;
        private String gameDate;
        private Long gameId;
        
        public PlayerAccoladeDTO() {}
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTypeDescription() {
            return typeDescription;
        }
        
        public void setTypeDescription(String typeDescription) {
            this.typeDescription = typeDescription;
        }
        
        public double getValue() {
            return value;
        }
        
        public void setValue(double value) {
            this.value = value;
        }
        
        public int getPosition() {
            return position;
        }
        
        public void setPosition(int position) {
            this.position = position;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
        
        public String getGameDate() {
            return gameDate;
        }
        
        public void setGameDate(String gameDate) {
            this.gameDate = gameDate;
        }
        
        public Long getGameId() {
            return gameId;
        }
        
        public void setGameId(Long gameId) {
            this.gameId = gameId;
        }
    }
}
