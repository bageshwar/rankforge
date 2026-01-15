package com.rankforge.server.dto;

/**
 * Data Transfer Object for player statistics in a game
 */
public class PlayerStatsDTO {
    private String playerName;
    private String playerId;
    private int kills;
    private int deaths;
    private int assists;
    private double rating;
    private String team; // "CT" or "T"
    private int damage; // Total damage dealt in the game
    private int headshotKills; // Number of headshot kills in the game
    private double headshotPercentage; // Percentage of kills that were headshots
    
    public PlayerStatsDTO() {}
    
    public PlayerStatsDTO(String playerName, String playerId, int kills, int deaths, int assists, double rating, String team) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.rating = rating;
        this.team = team;
        this.damage = 0;
        this.headshotKills = 0;
        this.headshotPercentage = 0.0;
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
    
    public int getDamage() {
        return damage;
    }
    
    public void setDamage(int damage) {
        this.damage = damage;
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
    
    public double getKdRatio() {
        return deaths > 0 ? (double) kills / deaths : kills;
    }
}