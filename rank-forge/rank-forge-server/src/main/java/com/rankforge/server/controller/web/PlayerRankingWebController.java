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

import com.rankforge.server.dto.PlayerRankingDTO;
import com.rankforge.server.service.PlayerRankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Web MVC Controller for rendering player rankings frontend
 * Author bageshwar.pn
 * Date 2024
 */
@Controller
public class PlayerRankingWebController {

    private final PlayerRankingService playerRankingService;

    @Autowired
    public PlayerRankingWebController(PlayerRankingService playerRankingService) {
        this.playerRankingService = playerRankingService;
    }

    /**
     * Home page - redirects to rankings
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/rankings";
    }

    /**
     * Display player rankings table
     * @param limit Optional limit for number of players to display
     * @param model Thymeleaf model
     * @return rankings template
     */
    @GetMapping("/rankings")
    public String rankings(@RequestParam(value = "limit", required = false) Integer limit, Model model) {
        List<PlayerRankingDTO> rankings;
        
        if (limit != null && limit > 0 && limit <= 100) {
            rankings = playerRankingService.getTopPlayerRankings(limit);
            model.addAttribute("isLimited", true);
            model.addAttribute("limit", limit);
        } else {
            rankings = playerRankingService.getAllPlayerRankings();
            model.addAttribute("isLimited", false);
        }
        
        model.addAttribute("rankings", rankings);
        model.addAttribute("totalPlayers", rankings.size());
        
        return "rankings";
    }
}