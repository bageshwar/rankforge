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

package com.rankforge.core.models;

/**
 * This class represents a player
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class Player {
    private String name;
    private String steamId;
    private boolean isBot;
    private String team; // "CT" or "T" (normalized from "TERRORIST")

    // Default constructor for Jackson deserialization
    public Player() {
    }

    public Player(String name, String steamId) {
        this.name = name;
        this.steamId = steamId;
        // TODO Handle "STEAM_ID_PENDING" cases
        this.isBot = (steamId == null) || "BOT".equals(steamId);
    }

    public Player(String name, String steamId, String team) {
        this.name = name;
        this.steamId = steamId;
        // TODO Handle "STEAM_ID_PENDING" cases
        this.isBot = (steamId == null) || "BOT".equals(steamId);
        this.team = normalizeTeam(team);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSteamId() {
        return steamId;
    }

    public void setSteamId(String steamId) {
        this.steamId = steamId;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = normalizeTeam(team);
    }

    /**
     * Normalize team value from log format to standard format.
     * "TERRORIST" -> "T", "CT" -> "CT"
     */
    private static String normalizeTeam(String team) {
        if (team == null) {
            return null;
        }
        if ("TERRORIST".equalsIgnoreCase(team)) {
            return "T";
        }
        return team;
    }
}
