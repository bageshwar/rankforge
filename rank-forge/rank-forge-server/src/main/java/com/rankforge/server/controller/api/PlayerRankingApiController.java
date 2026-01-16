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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;

import java.time.LocalDate;
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
@Validated
public class PlayerRankingApiController {

    private final PlayerRankingService playerRankingService;

    @Autowired
    public PlayerRankingApiController(PlayerRankingService playerRankingService) {
        this.playerRankingService = playerRankingService;
    }

    /**
     * Get all player rankings with summary statistics
     * @param clanId Required clan ID to filter rankings
     * @return LeaderboardResponseDTO with rankings and summary stats
     * @deprecated Use /api/rankings/stats for consistency. This endpoint is kept for backward compatibility.
     */
    @GetMapping
    @Deprecated
    public ResponseEntity<LeaderboardResponseDTO> getAllRankings(@RequestParam(value = "clanId", required = true) Long clanId) {
        if (clanId == null) {
            return ResponseEntity.badRequest().build();
        }
        LeaderboardResponseDTO response = playerRankingService.getAllPlayerRankingsWithStats(clanId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all player rankings with summary statistics
     * @param clanId Required clan ID to filter rankings
     * @return LeaderboardResponseDTO with rankings and summary stats
     */
    @GetMapping("/stats")
    public ResponseEntity<LeaderboardResponseDTO> getAllRankingsWithStats(@RequestParam(value = "clanId", required = true) Long clanId) {
        if (clanId == null) {
            return ResponseEntity.badRequest().build();
        }
        LeaderboardResponseDTO response = playerRankingService.getAllPlayerRankingsWithStats(clanId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get top N player rankings with summary statistics
     * @param limit Number of top players to return (default: 10, max: 100)
     * @param clanId Required clan ID to filter rankings
     * @return LeaderboardResponseDTO with rankings and summary stats
     */
    @GetMapping("/top")
    public ResponseEntity<LeaderboardResponseDTO> getTopRankings(
            @RequestParam(value = "limit", defaultValue = "10") 
            @Min(value = 1, message = "Limit must be >= 1") 
            @Max(value = 100, message = "Limit must be <= 100") 
            int limit,
            @RequestParam(value = "clanId", required = true) Long clanId) {
        
        if (clanId == null) {
            return ResponseEntity.badRequest().build();
        }
        LeaderboardResponseDTO response = playerRankingService.getTopPlayerRankingsWithStats(limit, clanId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get top N player rankings with summary statistics
     * @param limit Number of top players to return (default: 10, max: 100)
     * @param clanId Required clan ID to filter rankings
     * @return LeaderboardResponseDTO with rankings and summary stats
     * @deprecated Use /api/rankings/top for consistency. This endpoint is kept for backward compatibility.
     */
    @GetMapping("/top/stats")
    @Deprecated
    public ResponseEntity<LeaderboardResponseDTO> getTopRankingsWithStats(
            @RequestParam(value = "limit", defaultValue = "10") 
            @Min(value = 1, message = "Limit must be >= 1") 
            @Max(value = 100, message = "Limit must be <= 100") 
            int limit,
            @RequestParam(value = "clanId", required = true) Long clanId) {
        
        if (clanId == null) {
            return ResponseEntity.badRequest().build();
        }
        LeaderboardResponseDTO response = playerRankingService.getTopPlayerRankingsWithStats(limit, clanId);
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
     * @param year The year (e.g., 2026). If not provided, defaults to current year. Must be between 2000-2100.
     * @param month The month (1-12). If not provided, defaults to current month.
     * @param limit Maximum number of results to return (default: 100, max: 1000)
     * @param offset Number of results to skip for pagination (default: 0, max: 10000)
     * @param clanId Required clan ID to filter rankings
     * @return LeaderboardResponseDTO with rankings and summary stats for the month
     */
    @GetMapping("/leaderboard/monthly")
    public ResponseEntity<LeaderboardResponseDTO> getMonthlyLeaderboard(
            @RequestParam(value = "year", required = false) 
            @Min(value = 2000, message = "Year must be >= 2000") 
            @Max(value = 2100, message = "Year must be <= 2100") 
            Integer year,
            @RequestParam(value = "month", required = false) 
            @Min(value = 1, message = "Month must be >= 1") 
            @Max(value = 12, message = "Month must be <= 12") 
            Integer month,
            @RequestParam(value = "limit", defaultValue = "100") 
            @Min(value = 1, message = "Limit must be >= 1") 
            @Max(value = 1000, message = "Limit must be <= 1000") 
            int limit,
            @RequestParam(value = "offset", defaultValue = "0") 
            @Min(value = 0, message = "Offset must be >= 0") 
            @Max(value = 10000, message = "Offset must be <= 10000") 
            int offset,
            @RequestParam(value = "clanId") Long clanId) {
        
        if (clanId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Default to current month if not provided
        LocalDate now = LocalDate.now();
        int queryYear = (year != null) ? year : now.getYear();
        int queryMonth = (month != null) ? month : now.getMonthValue();
        
        // Validate that requested date is not in the future
        LocalDate requestedDate = LocalDate.of(queryYear, queryMonth, 1);
        LocalDate currentDate = LocalDate.now();
        if (requestedDate.isAfter(currentDate)) {
            // Return empty response instead of 400 for future dates
            return ResponseEntity.ok(new LeaderboardResponseDTO(new ArrayList<>(), 0, 0, 0));
        }
        
        LeaderboardResponseDTO response = playerRankingService.getMonthlyPlayerRankingsWithStats(
                queryYear, queryMonth, limit, offset, clanId);
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