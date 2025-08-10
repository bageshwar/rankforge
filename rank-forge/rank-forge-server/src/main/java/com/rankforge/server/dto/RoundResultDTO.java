package com.rankforge.server.dto;

import java.time.Instant;

/**
 * Data Transfer Object for round result information
 */
public class RoundResultDTO {
    private int roundNumber;
    private String winnerTeam; // "CT" or "T"
    private String winCondition; // "elimination", "bomb_defused", "bomb_exploded", "time_expired"
    private Instant roundEndTime;
    private int ctScore;
    private int tScore;
    
    public RoundResultDTO() {}
    
    public RoundResultDTO(int roundNumber, String winnerTeam, String winCondition, Instant roundEndTime, int ctScore, int tScore) {
        this.roundNumber = roundNumber;
        this.winnerTeam = winnerTeam;
        this.winCondition = winCondition;
        this.roundEndTime = roundEndTime;
        this.ctScore = ctScore;
        this.tScore = tScore;
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
    
    public String getWinCondition() {
        return winCondition;
    }
    
    public void setWinCondition(String winCondition) {
        this.winCondition = winCondition;
    }
    
    public Instant getRoundEndTime() {
        return roundEndTime;
    }
    
    public void setRoundEndTime(Instant roundEndTime) {
        this.roundEndTime = roundEndTime;
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
}