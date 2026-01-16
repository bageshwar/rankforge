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

import com.rankforge.server.dto.PlayerProfileDTO;
import com.rankforge.server.dto.UserDTO;
import com.rankforge.server.entity.Clan;
import com.rankforge.server.entity.User;
import com.rankforge.server.repository.UserRepository;
import com.rankforge.server.service.ClanService;
import com.rankforge.server.service.PlayerProfileService;
import com.rankforge.server.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for user management
 * Author bageshwar.pn
 * Date 2026
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PlayerProfileService playerProfileService;
    
    @Autowired
    private ClanService clanService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get current authenticated user profile
     * Requires JWT authentication
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            String steamId64 = (String) request.getAttribute("steamId64");
            
            if (steamId64 == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Unauthorized. Missing authentication."));
            }
            
            Optional<UserDTO> userOpt = userService.getUserDTOBySteamId64(steamId64);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "User not found"));
            }
            
            UserDTO user = userOpt.get();
            
            // Also fetch player stats if available
            Optional<PlayerProfileDTO> playerProfileOpt = playerProfileService.getPlayerProfile(user.getSteamId3());
            if (playerProfileOpt.isPresent()) {
                // Add stats to response
                PlayerProfileDTO stats = playerProfileOpt.get();
                Map<String, Object> response = Map.of(
                    "user", user,
                    "stats", Map.of(
                        "currentRank", stats.getCurrentRank(),
                        "totalKills", stats.getTotalKills(),
                        "totalDeaths", stats.getTotalDeaths(),
                        "totalAssists", stats.getTotalAssists(),
                        "killDeathRatio", stats.getKillDeathRatio(),
                        "totalGamesPlayed", stats.getTotalGamesPlayed(),
                        "totalRoundsPlayed", stats.getTotalRoundsPlayed()
                    )
                );
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.ok(Map.of("user", user));
            
        } catch (Exception e) {
            logger.error("Error fetching current user: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error"));
        }
    }
    
    /**
     * Get user avatar URL by Steam ID
     */
    @GetMapping("/{steamId}/avatar")
    public ResponseEntity<?> getUserAvatar(@PathVariable("steamId") String steamId) {
        try {
            // Try to find by Steam ID64 first
            Optional<UserDTO> userOpt = userService.getUserDTOBySteamId64(steamId);
            
            if (userOpt.isEmpty()) {
                // Try by Steam ID3
                var userBySteamId3 = userService.findBySteamId3(steamId);
                if (userBySteamId3.isPresent()) {
                    userOpt = Optional.of(new UserDTO(userBySteamId3.get()));
                }
            }
            
            if (userOpt.isPresent()) {
                UserDTO user = userOpt.get();
                String avatarUrl = user.getAvatarMediumUrl() != null ? 
                    user.getAvatarMediumUrl() : user.getAvatarUrl();
                
                if (avatarUrl != null) {
                    return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
                }
            }
            
            // Return default avatar if user not found or no avatar
            return ResponseEntity.ok(Map.of("avatarUrl", "/default-avatar.png"));
            
        } catch (Exception e) {
            logger.error("Error fetching user avatar: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("avatarUrl", "/default-avatar.png"));
        }
    }
    
    /**
     * Update default clan for current user
     */
    @PutMapping("/me/default-clan")
    public ResponseEntity<?> updateDefaultClan(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        try {
            String steamId64 = (String) httpRequest.getAttribute("steamId64");
            
            if (steamId64 == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Unauthorized. Missing authentication."));
            }
            
            Long clanId = request.get("clanId");
            
            // Get current user
            Optional<User> userOpt = userRepository.findBySteamId64(steamId64);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // If clanId is provided, verify it exists and user is a member
            if (clanId != null) {
                // Verify clan exists and user is a member
                List<Clan> userClans = clanService.getClansForUser(user.getId());
                boolean isMember = userClans.stream().anyMatch(c -> c.getId().equals(clanId));
                
                if (!isMember) {
                    return ResponseEntity.status(403)
                            .body(Map.of("error", "You are not a member of this clan"));
                }
            }
            
            // Update default clan
            user.setDefaultClanId(clanId);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("success", true, "defaultClanId", clanId != null ? clanId : "null"));
            
        } catch (Exception e) {
            logger.error("Error updating default clan: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update default clan"));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Users API is healthy");
    }
}
