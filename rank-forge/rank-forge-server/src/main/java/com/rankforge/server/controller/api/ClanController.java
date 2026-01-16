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

import com.rankforge.server.dto.ClanDTO;
import com.rankforge.server.dto.ClanMembershipDTO;
import com.rankforge.server.dto.CreateClanRequest;
import com.rankforge.server.dto.TransferAdminRequest;
import com.rankforge.server.entity.Clan;
import com.rankforge.server.entity.ClanMembership;
import com.rankforge.server.entity.User;
import com.rankforge.server.repository.ClanMembershipRepository;
import com.rankforge.server.repository.UserRepository;
import com.rankforge.server.service.ClanMembershipService;
import com.rankforge.server.service.ClanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API Controller for clan management
 * Author bageshwar.pn
 * Date 2026
 */
@RestController
@RequestMapping("/api/clans")
@CrossOrigin(origins = "*")
public class ClanController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClanController.class);
    
    @Autowired
    private ClanService clanService;
    
    @Autowired
    private ClanMembershipService clanMembershipService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ClanMembershipRepository clanMembershipRepository;
    
    /**
     * Create a new clan (requires authentication)
     */
    @PostMapping
    public ResponseEntity<?> createClan(@Valid @RequestBody CreateClanRequest request, 
                                         HttpServletRequest httpRequest) {
        try {
            String steamId64 = (String) httpRequest.getAttribute("steamId64");
            
            if (steamId64 == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized. Missing authentication."));
            }
            
            // Get current user
            Optional<User> userOpt = userRepository.findBySteamId64(steamId64);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Create clan
            Clan clan = clanService.createClan(
                request.getAppServerId(),
                request.getName(),
                request.getTelegramChannelId(),
                user.getId()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ClanDTO(clan));
                    
        } catch (IllegalStateException e) {
            logger.warn("Failed to create clan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request to create clan: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating clan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create clan"));
        }
    }
    
    /**
     * Get current user's clans (requires authentication)
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyClans(HttpServletRequest httpRequest) {
        try {
            String steamId64 = (String) httpRequest.getAttribute("steamId64");
            
            if (steamId64 == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized. Missing authentication."));
            }
            
            // Get current user
            Optional<User> userOpt = userRepository.findBySteamId64(steamId64);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Get clans where user is admin
            List<Clan> adminClans = clanService.getClansByAdmin(user.getId());
            
            // Get clans where user is member
            List<Clan> memberClans = clanService.getClansForUser(user.getId());
            
            // Combine and deduplicate
            List<ClanDTO> allClans = adminClans.stream()
                    .map(ClanDTO::new)
                    .collect(Collectors.toList());
            
            memberClans.stream()
                    .filter(clan -> !adminClans.contains(clan))
                    .map(ClanDTO::new)
                    .forEach(allClans::add);
            
            return ResponseEntity.ok(allClans);
            
        } catch (Exception e) {
            logger.error("Error fetching user clans: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clans"));
        }
    }
    
    /**
     * Get clan details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getClan(@PathVariable("id") Long id) {
        try {
            Optional<ClanDTO> clanOpt = clanService.getClanDTOById(id);
            
            if (clanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Clan not found"));
            }
            
            return ResponseEntity.ok(clanOpt.get());
            
        } catch (Exception e) {
            logger.error("Error fetching clan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clan"));
        }
    }
    
    /**
     * Get clan members
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<?> getClanMembers(@PathVariable("id") Long id) {
        try {
            // Verify clan exists
            Optional<Clan> clanOpt = clanService.getClanById(id);
            if (clanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Clan not found"));
            }
            
            List<ClanMembership> memberships = clanMembershipRepository.findByClanId(id);
            List<ClanMembershipDTO> memberDTOs = memberships.stream()
                    .map(ClanMembershipDTO::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(memberDTOs);
            
        } catch (Exception e) {
            logger.error("Error fetching clan members: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clan members"));
        }
    }
    
    /**
     * Transfer admin rights (admin only, requires authentication)
     */
    @PutMapping("/{id}/admin")
    public ResponseEntity<?> transferAdmin(@PathVariable("id") Long id,
                                            @Valid @RequestBody TransferAdminRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            String steamId64 = (String) httpRequest.getAttribute("steamId64");
            
            if (steamId64 == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized. Missing authentication."));
            }
            
            // Get current user
            Optional<User> userOpt = userRepository.findBySteamId64(steamId64);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Transfer admin
            Clan clan = clanService.transferAdmin(id, request.getNewAdminUserId(), user.getId());
            
            return ResponseEntity.ok(new ClanDTO(clan));
            
        } catch (IllegalStateException e) {
            logger.warn("Failed to transfer admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request to transfer admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error transferring admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to transfer admin"));
        }
    }
    
    /**
     * Check if appServerId is claimed
     */
    @GetMapping("/check-app-server/{appServerId}")
    public ResponseEntity<?> checkAppServer(@PathVariable("appServerId") Long appServerId) {
        try {
            boolean claimed = clanService.isAppServerClaimed(appServerId);
            return ResponseEntity.ok(Map.of("claimed", claimed));
        } catch (Exception e) {
            logger.error("Error checking app server: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check app server"));
        }
    }
    
    /**
     * Get clan by appServerId
     */
    @GetMapping("/by-app-server/{appServerId}")
    public ResponseEntity<?> getClanByAppServerId(@PathVariable("appServerId") Long appServerId) {
        try {
            Optional<ClanDTO> clanOpt = clanService.getClanDTOByAppServerId(appServerId);
            
            if (clanOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Clan not found for appServerId: " + appServerId));
            }
            
            return ResponseEntity.ok(clanOpt.get());
        } catch (Exception e) {
            logger.error("Error fetching clan by appServerId: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch clan"));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Clans API is healthy");
    }
}
