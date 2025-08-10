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

package com.rankforge.server.controller.web;

import com.rankforge.server.dto.GameDTO;
import com.rankforge.server.dto.GameDetailsDTO;
import com.rankforge.server.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Web MVC Controller for rendering processed games frontend
 * Author bageshwar.pn
 * Date 2024
 */
@Controller
public class GameWebController {

    private final GameService gameService;

    @Autowired
    public GameWebController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Display processed games table
     * @param limit Optional limit for number of games to display
     * @param model Thymeleaf model
     * @return games template
     */
    @GetMapping("/games")
    public String games(@RequestParam(value = "limit", required = false) Integer limit, Model model) {
        List<GameDTO> games;
        
        if (limit != null && limit > 0 && limit <= 100) {
            games = gameService.getRecentGames(limit);
            model.addAttribute("isLimited", true);
            model.addAttribute("limit", limit);
        } else {
            games = gameService.getAllGames();
            model.addAttribute("isLimited", false);
        }
        
        model.addAttribute("games", games);
        model.addAttribute("totalGames", games.size());
        
        return "games";
    }
    
    /**
     * Display detailed game information
     * @param gameId Unique game identifier
     * @param model Thymeleaf model
     * @return game-details template or redirect to games if not found
     */
    @GetMapping("/games/details/{gameId}")
    public String gameDetails(@PathVariable("gameId") String gameId, Model model) {
        GameDTO game = gameService.getGameById(gameId);
        if (game == null) {
            // Game not found, redirect to games list
            return "redirect:/games";
        }
        
        GameDetailsDTO gameDetails = gameService.getGameDetails(gameId);
        
        model.addAttribute("game", game);
        model.addAttribute("gameDetails", gameDetails);
        
        if (gameDetails != null) {
            model.addAttribute("rounds", gameDetails.getRounds());
            model.addAttribute("playerStats", gameDetails.getPlayerStats());
        }
        
        return "game-details";
    }
}