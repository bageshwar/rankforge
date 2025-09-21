package com.rankforge.server.dto;

/**
 * Data Transfer Object for player statistics in a game
 */
public class PlayerStatsDTO {
    private String playerName;
    private int kills;
    private int deaths;
    private int assists;
    private double rating;
    private String team; // "CT" or "T"
    
    public PlayerStatsDTO() {}
    
    public PlayerStatsDTO(String playerName, int kills, int deaths, int assists, double rating, String team) {
        this.playerName = playerName;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.rating = rating;
        this.team = team;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
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
    
    public double getRating() {
        return rating;
    }
    
    public void setRating(double rating) {
        this.rating = rating;
    }
    
    public String getTeam() {
        return team;
    }
    
    public void setTeam(String team) {
        this.team = team;
    }
    
    public double getKdRatio() {
        return deaths > 0 ? (double) kills / deaths : kills;
    }
}