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

import com.rankforge.server.dto.GameDTO;
import com.rankforge.server.dto.GameDetailsDTO;
import com.rankforge.server.dto.RoundDetailsDTO;
import com.rankforge.server.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for processed games
 * Author bageshwar.pn
 * Date 2024
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*") // Allow CORS for frontend development
public class GameApiController {

    private final GameService gameService;

    @Autowired
    public GameApiController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Get all processed games
     * @return List of all processed games sorted by date
     */
    @GetMapping
    public ResponseEntity<List<GameDTO>> getAllGames() {
        List<GameDTO> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }

    /**
     * Get recent N processed games
     * @param limit Number of recent games to return (default: 10)
     * @return List of recent N games
     */
    @GetMapping("/recent")
    public ResponseEntity<List<GameDTO>> getRecentGames(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        if (limit <= 0 || limit > 100) {
            limit = 10; // Default to 10 if invalid
        }
        
        List<GameDTO> games = gameService.getRecentGames(limit);
        return ResponseEntity.ok(games);
    }

    /**
     * Health check endpoint
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Games API is healthy");
    }
    
    /**
     * Get detailed information for a specific game
     * @param gameId Unique game identifier
     * @return Detailed game information including rounds and player stats
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameDTO> getGame(@PathVariable("gameId") String gameId) {
        GameDTO game = gameService.getGameById(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
    }
    
    /**
     * Get detailed game statistics including rounds and player performance
     * @param gameId Unique game identifier
     * @return Detailed game statistics
     */
    @GetMapping("/{gameId}/details")
    public ResponseEntity<GameDetailsDTO> getGameDetails(@PathVariable("gameId") String gameId) {
        GameDetailsDTO gameDetails = gameService.getGameDetails(gameId);
        if (gameDetails == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(gameDetails);
    }
    
    /**
     * Get detailed round information including all events
     * @param gameId Unique game identifier
     * @param roundNumber Round number (1-indexed)
     * @return Round details with all events sorted by timeline
     */
    @GetMapping("/{gameId}/rounds/{roundNumber}")
    public ResponseEntity<RoundDetailsDTO> getRoundDetails(
            @PathVariable("gameId") String gameId,
            @PathVariable("roundNumber") int roundNumber) {
        RoundDetailsDTO roundDetails = gameService.getRoundDetails(gameId, roundNumber);
        if (roundDetails == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(roundDetails);
    }
}