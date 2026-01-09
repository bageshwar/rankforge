package com.rankforge.server.dto;

import java.util.List;

/**
 * Data Transfer Object for detailed game information
 */
public class GameDetailsDTO {
    private int ctScore;
    private int tScore;
    private int totalRounds;
    private List<PlayerStatsDTO> playerStats;
    private List<RoundResultDTO> rounds;
    private List<AccoladeDTO> accolades;
    
    public GameDetailsDTO() {}
    
    public GameDetailsDTO(int ctScore, int tScore, int totalRounds) {
        this.ctScore = ctScore;
        this.tScore = tScore;
        this.totalRounds = totalRounds;
    }
    
    public int getCtScore() {
        return ctScore;
    }
    
    public void setCtScore(int ctScore) {
        this.ctScore = ctScore;
    }
    
    public int getTScore() {
        return tScore;
    }
    
    public void setTScore(int tScore) {
        this.tScore = tScore;
    }
    
    public int getTotalRounds() {
        return totalRounds;
    }
    
    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }
    
    public List<PlayerStatsDTO> getPlayerStats() {
        return playerStats;
    }
    
    public void setPlayerStats(List<PlayerStatsDTO> playerStats) {
        this.playerStats = playerStats;
    }
    
    public List<RoundResultDTO> getRounds() {
        return rounds;
    }
    
    public void setRounds(List<RoundResultDTO> rounds) {
        this.rounds = rounds;
    }
    
    public List<AccoladeDTO> getAccolades() {
        return accolades;
    }
    
    public void setAccolades(List<AccoladeDTO> accolades) {
        this.accolades = accolades;
    }
}