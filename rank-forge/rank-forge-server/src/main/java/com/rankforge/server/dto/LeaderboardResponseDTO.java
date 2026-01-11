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

import java.util.List;

/**
 * DTO for leaderboard response including rankings and summary statistics
 * Author bageshwar.pn
 * Date 2026
 */
public class LeaderboardResponseDTO {
    private List<PlayerRankingDTO> rankings;
    private long totalGames;
    private long totalRounds;
    private int totalPlayers;

    public LeaderboardResponseDTO() {}

    public LeaderboardResponseDTO(List<PlayerRankingDTO> rankings, long totalGames, long totalRounds, int totalPlayers) {
        this.rankings = rankings;
        this.totalGames = totalGames;
        this.totalRounds = totalRounds;
        this.totalPlayers = totalPlayers;
    }

    public List<PlayerRankingDTO> getRankings() {
        return rankings;
    }

    public void setRankings(List<PlayerRankingDTO> rankings) {
        this.rankings = rankings;
    }

    public long getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(long totalGames) {
        this.totalGames = totalGames;
    }

    public long getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(long totalRounds) {
        this.totalRounds = totalRounds;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }
}
