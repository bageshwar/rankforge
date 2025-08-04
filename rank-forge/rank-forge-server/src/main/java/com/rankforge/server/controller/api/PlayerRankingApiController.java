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

import com.rankforge.server.dto.PlayerRankingDTO;
import com.rankforge.server.service.PlayerRankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Health check endpoint
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RankForge API is running!");
    }
}