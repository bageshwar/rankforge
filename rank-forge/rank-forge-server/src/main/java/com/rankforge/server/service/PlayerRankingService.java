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

package com.rankforge.server.service;

import com.rankforge.core.models.PlayerStats;
import com.rankforge.server.dto.PlayerRankingDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing player rankings
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class PlayerRankingService {

    /**
     * Get all player rankings sorted by rank
     * For now, this returns mock data. In a real implementation, 
     * this would fetch from a database or ranking service.
     */
    public List<PlayerRankingDTO> getAllPlayerRankings() {
        List<PlayerStats> playerStats = generateMockPlayerStats();
        
        return playerStats.stream()
                .sorted(Comparator.comparingInt(PlayerStats::getRank))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get top N player rankings
     */
    public List<PlayerRankingDTO> getTopPlayerRankings(int limit) {
        return getAllPlayerRankings().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Convert PlayerStats to PlayerRankingDTO
     */
    private PlayerRankingDTO convertToDTO(PlayerStats stats) {
        return new PlayerRankingDTO(
                stats.getRank(),
                stats.getLastSeenNickname(),
                stats.getPlayerId(),
                stats.getKills(),
                stats.getDeaths(),
                stats.getAssists(),
                stats.getHeadshotKills(),
                stats.getRoundsPlayed(),
                stats.getClutchesWon(),
                stats.getDamageDealt()
        );
    }

    /**
     * Generate mock player data for demonstration
     * In a real implementation, this would be replaced with actual data access
     */
    private List<PlayerStats> generateMockPlayerStats() {
        List<PlayerStats> playerStatsList = new ArrayList<>();

        String[] playerNames = {
                "ProGamer_2024", "ShadowSniper", "FragMaster", "HeadshotKing", "ClutchLord",
                "AWPer_Elite", "SprayControl", "TacticalNinja", "BombDefuser", "TeamLeader",
                "QuickScope", "FlashMaster", "SmokeGuru", "EntryFragger", "SupportPlayer"
        };

        for (int i = 0; i < playerNames.length; i++) {
            PlayerStats stats = new PlayerStats();
            stats.setPlayerId("STEAM_" + (1000000 + i));
            stats.setLastSeenNickname(playerNames[i]);
            stats.setRank(i + 1);
            
            // Generate realistic but varied stats
            int baseKills = 150 - (i * 8) + (int)(Math.random() * 20);
            int baseDeaths = 80 - (i * 3) + (int)(Math.random() * 15);
            int baseAssists = 45 - (i * 2) + (int)(Math.random() * 10);
            
            stats.setKills(Math.max(10, baseKills));
            stats.setDeaths(Math.max(5, baseDeaths));
            stats.setAssists(Math.max(2, baseAssists));
            stats.setHeadshotKills((int)(stats.getKills() * (0.25 + Math.random() * 0.15)));
            stats.setRoundsPlayed(120 - (i * 3) + (int)(Math.random() * 20));
            stats.setClutchesWon((int)(Math.random() * 8) + 1);
            stats.setDamageDealt(12000 - (i * 400) + (Math.random() * 2000));
            stats.setLastUpdated(Instant.now());
            
            playerStatsList.add(stats);
        }

        return playerStatsList;
    }
}