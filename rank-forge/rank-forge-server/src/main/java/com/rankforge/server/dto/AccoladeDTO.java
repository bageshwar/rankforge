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

/**
 * Data Transfer Object for game accolades
 * Represents player achievements/awards at the end of a match
 * Author bageshwar.pn
 * Date 2026
 */
public class AccoladeDTO {
    private String type;
    private String playerName;
    private String playerId;
    private double value;
    private int position;
    private double score;
    
    public AccoladeDTO() {}
    
    public AccoladeDTO(String type, String playerName, String playerId, double value, int position, double score) {
        this.type = type;
        this.playerName = playerName;
        this.playerId = playerId;
        this.value = value;
        this.position = position;
        this.score = score;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
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
    
    /**
     * Get a human-readable description of the accolade type
     */
    public String getTypeDescription() {
        if (type == null) return "Unknown";
        
        // Map common accolade types to readable descriptions
        return switch (type.toLowerCase()) {
            case "5k" -> "5 Kills";
            case "4k" -> "4 Kills";
            case "3k" -> "3 Kills";
            case "firstkills" -> "First Kills";
            case "deaths" -> "Most Deaths";
            case "assists" -> "Most Assists";
            case "hsp" -> "Headshot Percentage";
            case "enemiesflashed" -> "Enemies Flashed";
            case "burndamage" -> "Burn Damage";
            case "uniqueweaponkills" -> "Unique Weapon Kills";
            case "cashspent" -> "Cash Spent";
            default -> type;
        };
    }
}
