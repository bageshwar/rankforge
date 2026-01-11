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

/**
 * DTO for player ranking display
 * Author bageshwar.pn
 * Date 2024
 */
public class PlayerRankingDTO {
    private int rank;
    private String playerName;
    private String playerId;
    private int kills;
    private int deaths;
    private int assists;
    private double killDeathRatio;
    private int headshotKills;
    private double headshotPercentage;
    private int roundsPlayed;
    private int clutchesWon;
    private double damageDealt;
    private int gamesPlayed;

    public PlayerRankingDTO() {}

    public PlayerRankingDTO(int rank, String playerName, String playerId, int kills, int deaths, 
                           int assists, int headshotKills, int roundsPlayed, int clutchesWon, 
                           double damageDealt, int gamesPlayed) {
        this.rank = rank;
        this.playerName = playerName;
        this.playerId = playerId;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.headshotKills = headshotKills;
        this.roundsPlayed = roundsPlayed;
        this.clutchesWon = clutchesWon;
        this.damageDealt = damageDealt;
        this.gamesPlayed = gamesPlayed;
        
        // Calculate derived stats
        this.killDeathRatio = deaths > 0 ? (double) kills / deaths : kills;
        this.headshotPercentage = kills > 0 ? (double) headshotKills / kills * 100 : 0;
    }

    // Getters and setters
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
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

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public void setRoundsPlayed(int roundsPlayed) {
        this.roundsPlayed = roundsPlayed;
    }

    public int getClutchesWon() {
        return clutchesWon;
    }

    public void setClutchesWon(int clutchesWon) {
        this.clutchesWon = clutchesWon;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(double damageDealt) {
        this.damageDealt = damageDealt;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
}