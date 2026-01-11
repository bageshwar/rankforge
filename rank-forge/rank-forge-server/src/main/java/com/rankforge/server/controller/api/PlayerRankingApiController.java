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

package com.rankforge.server.controller.api;

import com.rankforge.server.dto.LeaderboardResponseDTO;
import com.rankforge.server.dto.PlayerRankingDTO;
import com.rankforge.server.service.PlayerRankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST API Controller for player rankings
 * Author bageshwar.pn
 * Date 2024
 */
@RestController
@RequestMapping("/api/rankings")
@CrossOrigin(origins = "*") // Allow CORS for frontend development
public class PlayerRankingApiController {

    private final PlayerRankingService playerRankingService;

    @Autowired
    public PlayerRankingApiController(PlayerRankingService playerRankingService) {
        this.playerRankingService = playerRankingService;
    }

    /**
     * Get all player rankings
     * @return List of all player rankings sorted by rank
     */
    @GetMapping
    public ResponseEntity<List<PlayerRankingDTO>> getAllRankings() {
        List<PlayerRankingDTO> rankings = playerRankingService.getAllPlayerRankings();
        return ResponseEntity.ok(rankings);
    }
    
    /**
     * Get all player rankings with summary statistics
     * @return LeaderboardResponseDTO with rankings and summary stats
     */
    @GetMapping("/stats")
    public ResponseEntity<LeaderboardResponseDTO> getAllRankingsWithStats() {
        LeaderboardResponseDTO response = playerRankingService.getAllPlayerRankingsWithStats();
        return ResponseEntity.ok(response);
    }

    /**
     * Get top N player rankings
     * @param limit Number of top players to return (default: 10)
     * @return List of top N player rankings
     */
    @GetMapping("/top")
    public ResponseEntity<List<PlayerRankingDTO>> getTopRankings(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (limit <= 0 || limit > 100) {
            limit = 10; // Default to 10 if invalid
        }
        
        List<PlayerRankingDTO> rankings = playerRankingService.getTopPlayerRankings(limit);
        return ResponseEntity.ok(rankings);
    }
    
    /**
     * Get top N player rankings with summary statistics
     * @param limit Number of top players to return (default: 10)
     * @return LeaderboardResponseDTO with rankings and summary stats
     */
    @GetMapping("/top/stats")
    public ResponseEntity<LeaderboardResponseDTO> getTopRankingsWithStats(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (limit <= 0 || limit > 100) {
            limit = 10; // Default to 10 if invalid
        }
        
        LeaderboardResponseDTO response = playerRankingService.getTopPlayerRankingsWithStats(limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get ranking for a specific player
     * @param playerId The Steam ID or player identifier
     * @return Player ranking details if found
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<PlayerRankingDTO> getPlayerRanking(@PathVariable("playerId") String playerId) {
        Optional<PlayerRankingDTO> ranking = playerRankingService.getPlayerRanking(playerId);
        
        if (ranking.isPresent()) {
            return ResponseEntity.ok(ranking.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get monthly leaderboard for a specific month
     * @param year The year (e.g., 2026). If not provided, defaults to current year
     * @param month The month (1-12). If not provided, defaults to current month
     * @param limit Maximum number of results to return (default: 100, max: 1000)
     * @param offset Number of results to skip for pagination (default: 0)
     * @return LeaderboardResponseDTO with rankings and summary stats for the month
     */
    @GetMapping("/leaderboard/monthly")
    public ResponseEntity<LeaderboardResponseDTO> getMonthlyLeaderboard(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        
        // Default to current month if not provided
        java.time.LocalDate now = java.time.LocalDate.now();
        int queryYear = (year != null) ? year : now.getYear();
        int queryMonth = (month != null) ? month : now.getMonthValue();
        
        // Validate month
        if (queryMonth < 1 || queryMonth > 12) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate year (reasonable range)
        if (queryYear < 2000 || queryYear > 2100) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate limit
        if (limit <= 0 || limit > 1000) {
            limit = 100; // Default to 100 if invalid
        }
        
        // Validate offset
        if (offset < 0) {
            offset = 0;
        }
        
        LeaderboardResponseDTO response = playerRankingService.getMonthlyPlayerRankingsWithStats(
                queryYear, queryMonth, limit, offset);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RankForge API is running with real data!");
    }
}