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

import com.rankforge.server.service.AdminGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for admin operations.
 * Requires API key authentication via SecurityConfig.
 * Author bageshwar.pn
 * Date 2026
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "${cors.allowed.origins:http://localhost:5173}")
public class AdminApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminApiController.class);
    
    private final AdminGameService adminGameService;
    
    @Autowired
    public AdminApiController(AdminGameService adminGameService) {
        this.adminGameService = adminGameService;
    }
    
    /**
     * Delete a game and all related entities (cascade deletion).
     * 
     * This endpoint:
     * - Hard deletes the game and all related entities (events, rounds, accolades, stats)
     * - Logs the action for auditing
     * - Returns success or error response
     * 
     * Note: Aggregate stats (leaderboards, user totals, clan totals) should be
     * recomputed in the background after deletion.
     * 
     * @param gameId The ID of the game to delete
     * @return Response indicating success or failure
     */
    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Map<String, Object>> deleteGame(@PathVariable("gameId") Long gameId) {
        logger.info("ADMIN_API: Delete game request received for game ID: {}", gameId);
        
        // Log audit information
        Instant deletionTime = Instant.now();
        logger.info("ADMIN_AUDIT: Game deletion requested - gameId: {}, time: {}, requester: API", 
                gameId, deletionTime);
        
        // Check if game exists
        if (!adminGameService.gameExists(gameId)) {
            logger.warn("ADMIN_API: Game not found for deletion - game ID: {}", gameId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Game not found");
            response.put("gameId", gameId);
            return ResponseEntity.notFound().build();
        }
        
        // Delete the game
        boolean deleted = adminGameService.deleteGame(gameId);
        
        if (deleted) {
            logger.info("ADMIN_AUDIT: Game successfully deleted - gameId: {}, time: {}", 
                    gameId, deletionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Game deleted successfully");
            response.put("gameId", gameId);
            response.put("deletedAt", deletionTime.toString());
            return ResponseEntity.ok(response);
        } else {
            logger.error("ADMIN_API: Failed to delete game - game ID: {}", gameId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to delete game");
            response.put("gameId", gameId);
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Health check endpoint for admin API
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Admin API is healthy");
    }
}
