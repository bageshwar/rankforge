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

package com.rankforge.server.controller.api;

import com.rankforge.server.dto.PlayerProfileDTO;
import com.rankforge.server.service.PlayerProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST API Controller for player profiles
 * Provides detailed player data including rating history, stats, and accolades
 * Author bageshwar.pn
 * Date 2026
 */
@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "${cors.allowed.origins:http://localhost:5173}")
public class PlayerProfileApiController {
    
    private final PlayerProfileService playerProfileService;
    
    @Autowired
    public PlayerProfileApiController(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }
    
    /**
     * Get complete player profile by player ID
     * @param playerId The numeric Steam ID (e.g., "123456789") or full format "[U:1:123456789]"
     * @return Complete player profile with rating history and accolades
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerProfileDTO> getPlayerProfile(@PathVariable("playerId") String playerId) {
        // URL decode the player ID first
        String decodedPlayerId = java.net.URLDecoder.decode(playerId, java.nio.charset.StandardCharsets.UTF_8);
        
        // If it's just a numeric ID, convert to full Steam ID format [U:1:xxx]
        String fullPlayerId = decodedPlayerId;
        if (decodedPlayerId.matches("\\d+")) {
            fullPlayerId = "[U:1:" + decodedPlayerId + "]";
        }
        
        Optional<PlayerProfileDTO> profile = playerProfileService.getPlayerProfile(fullPlayerId);
        
        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get list of all players with basic info
     * @return List of all players with basic profile data
     */
    @GetMapping
    public ResponseEntity<List<PlayerProfileDTO>> getAllPlayers() {
        List<PlayerProfileDTO> players = playerProfileService.getAllPlayersBasicInfo();
        return ResponseEntity.ok(players);
    }
    
    /**
     * Health check endpoint
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Players API is healthy");
    }
}
