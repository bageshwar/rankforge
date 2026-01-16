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

package com.rankforge.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.server.dto.ClanDTO;
import com.rankforge.server.entity.Clan;
import com.rankforge.server.entity.User;
import com.rankforge.server.repository.ClanMembershipRepository;
import com.rankforge.server.repository.ClanRepository;
import com.rankforge.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing clans
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class ClanService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClanService.class);
    
    private final ClanRepository clanRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GameEventRepository gameEventRepository;
    private final ClanMembershipService clanMembershipService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ClanService(ClanRepository clanRepository,
                       UserRepository userRepository,
                       GameRepository gameRepository,
                       GameEventRepository gameEventRepository,
                       ClanMembershipService clanMembershipService,
                       ObjectMapper objectMapper) {
        this.clanRepository = clanRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.gameEventRepository = gameEventRepository;
        this.clanMembershipService = clanMembershipService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a new clan with appServerId claim validation
     * After creation, retroactively associates existing games and players
     */
    @Transactional
    public Clan createClan(Long appServerId, String name, String telegramChannelId, Long adminUserId) {
        // Validate admin user exists
        Optional<User> adminUserOpt = userRepository.findById(adminUserId);
        if (adminUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Admin user not found: " + adminUserId);
        }
        
        // Check if appServerId is already claimed
        if (clanRepository.existsByAppServerId(appServerId)) {
            throw new IllegalStateException("App server ID already claimed: " + appServerId);
        }
        
        // Create clan
        Clan clan = new Clan(appServerId, name, telegramChannelId, adminUserId);
        Clan savedClan = clanRepository.save(clan);
        logger.info("Created clan {} for appServerId {} by admin {}", savedClan.getId(), appServerId, adminUserId);
        
        // Retroactively associate existing games and players
        try {
            associateExistingGamesAndPlayers(savedClan.getId(), appServerId);
        } catch (Exception e) {
            logger.error("Failed to associate existing games and players for clan {}: {}", savedClan.getId(), e.getMessage(), e);
            // Don't fail clan creation if association fails - it can be retried later
        }
        
        return savedClan;
    }
    
    /**
     * Get clan by app server ID
     */
    public Optional<Clan> getClanByAppServerId(Long appServerId) {
        return clanRepository.findByAppServerId(appServerId);
    }
    
    /**
     * Get clan by ID
     */
    public Optional<Clan> getClanById(Long clanId) {
        return clanRepository.findById(clanId);
    }
    
    /**
     * Get all clans where user is admin
     */
    public List<Clan> getClansByAdmin(Long adminUserId) {
        return clanRepository.findByAdminUserId(adminUserId);
    }
    
    /**
     * Get all clans a user is member of (via ClanMembership)
     */
    public List<Clan> getClansForUser(Long userId) {
        List<Long> clanIds = clanMembershipService.getUserClans(userId);
        return clanRepository.findAllById(clanIds);
    }
    
    /**
     * Transfer admin rights to another user
     */
    @Transactional
    public Clan transferAdmin(Long clanId, Long newAdminUserId, Long currentAdminUserId) {
        Optional<Clan> clanOpt = clanRepository.findById(clanId);
        if (clanOpt.isEmpty()) {
            throw new IllegalArgumentException("Clan not found: " + clanId);
        }
        
        Clan clan = clanOpt.get();
        
        // Verify current user is admin
        if (!clan.getAdminUserId().equals(currentAdminUserId)) {
            throw new IllegalStateException("Only clan admin can transfer admin rights");
        }
        
        // Verify new admin user exists
        Optional<User> newAdminOpt = userRepository.findById(newAdminUserId);
        if (newAdminOpt.isEmpty()) {
            throw new IllegalArgumentException("New admin user not found: " + newAdminUserId);
        }
        
        // Transfer admin
        clan.setAdminUserId(newAdminUserId);
        Clan saved = clanRepository.save(clan);
        logger.info("Transferred admin of clan {} from {} to {}", clanId, currentAdminUserId, newAdminUserId);
        
        return saved;
    }
    
    /**
     * Check if appServerId is already claimed
     */
    public boolean isAppServerClaimed(Long appServerId) {
        return clanRepository.existsByAppServerId(appServerId);
    }
    
    /**
     * Get clan associated with a game (by appServerId)
     */
    public Optional<Clan> getClanForGame(GameEntity game) {
        if (game.getAppServerId() == null) {
            return Optional.empty();
        }
        return clanRepository.findByAppServerId(game.getAppServerId());
    }
    
    /**
     * Retroactively associate existing games and players with a clan
     * Called after clan creation to add players who already played on that appServerId
     */
    @Transactional
    public void associateExistingGamesAndPlayers(Long clanId, Long appServerId) {
        logger.info("Associating existing games and players for clan {} with appServerId {}", clanId, appServerId);
        
        // Find all existing games with this appServerId
        List<GameEntity> games = gameRepository.findByAppServerId(appServerId);
        logger.info("Found {} existing games for appServerId {}", games.size(), appServerId);
        
        if (games.isEmpty()) {
            return;
        }
        
        // Extract all unique player Steam ID3s from all games
        Set<String> uniqueSteamId3s = new HashSet<>();
        Instant earliestGameTime = null;
        
        for (GameEntity game : games) {
            // Track earliest game time for joinedAt timestamp
            if (earliestGameTime == null || game.getGameOverTimestamp().isBefore(earliestGameTime)) {
                earliestGameTime = game.getGameOverTimestamp();
            }
            
            // Extract players from this game
            List<String> gamePlayers = extractPlayersFromGame(game.getId());
            uniqueSteamId3s.addAll(gamePlayers);
        }
        
        logger.info("Found {} unique players across {} games", uniqueSteamId3s.size(), games.size());
        
        // Add all players to clan membership
        Instant joinedAt = earliestGameTime != null ? earliestGameTime : Instant.now();
        int added = clanMembershipService.addMembersBySteamId3(clanId, new ArrayList<>(uniqueSteamId3s), joinedAt);
        logger.info("Added {} players to clan {} from existing games", added, clanId);
    }
    
    /**
     * Extract player Steam ID3s from a game
     * Uses RoundEndEventEntity's playersJson field
     */
    private List<String> extractPlayersFromGame(Long gameId) {
        List<String> steamId3s = new ArrayList<>();
        
        try {
            // Get all rounds for this game
            List<RoundEndEventEntity> rounds = gameEventRepository.findRoundEndEventsByGameId(gameId);
            
            // Extract players from rounds
            for (RoundEndEventEntity roundEntity : rounds) {
                String playersJson = roundEntity.getPlayersJson();
                if (playersJson != null && !playersJson.isEmpty()) {
                    try {
                        JsonNode playersNode = objectMapper.readTree(playersJson);
                        // Players JSON is stored as an array of player IDs (numeric)
                        if (playersNode.isArray()) {
                            for (JsonNode playerIdNode : playersNode) {
                                String playerId = playerIdNode.asText();
                                if (playerId != null && !playerId.isEmpty() && !"0".equals(playerId)) {
                                    // Convert numeric ID to Steam ID3 format [U:1:xxx]
                                    String steamId3 = "[U:1:" + playerId + "]";
                                    if (!steamId3s.contains(steamId3)) {
                                        steamId3s.add(steamId3);
                                    }
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse players JSON for round {} in game {}: {}", 
                            roundEntity.getId(), gameId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract players from game {}: {}", gameId, e.getMessage(), e);
        }
        
        return steamId3s;
    }
    
    /**
     * Get clan DTO by ID
     */
    public Optional<ClanDTO> getClanDTOById(Long clanId) {
        return clanRepository.findById(clanId)
            .map(ClanDTO::new);
    }
    
    /**
     * Get clan DTO by appServerId
     */
    public Optional<ClanDTO> getClanDTOByAppServerId(Long appServerId) {
        return clanRepository.findByAppServerId(appServerId)
            .map(ClanDTO::new);
    }
    
    /**
     * Associate players from a newly processed game with a clan (forward association)
     * Called after a game is persisted to auto-add players to the clan
     * @param game The game entity that was just processed
     */
    @Transactional
    public void associatePlayersFromGame(GameEntity game) {
        if (game.getAppServerId() == null) {
            logger.debug("Game {} has no appServerId, skipping clan association", game.getId());
            return;
        }
        
        // Find clan for this appServerId
        Optional<Clan> clanOpt = clanRepository.findByAppServerId(game.getAppServerId());
        if (clanOpt.isEmpty()) {
            logger.debug("No clan found for appServerId {}, skipping player association", game.getAppServerId());
            return;
        }
        
        Clan clan = clanOpt.get();
        logger.info("Associating players from game {} with clan {} (appServerId: {})", 
            game.getId(), clan.getId(), game.getAppServerId());
        
        // Extract players from this game
        List<String> playerSteamId3s = extractPlayersFromGame(game.getId());
        
        if (playerSteamId3s.isEmpty()) {
            logger.debug("No players found in game {}", game.getId());
            return;
        }
        
        // Add all players to clan membership (idempotent)
        Instant joinedAt = game.getGameOverTimestamp() != null ? 
            game.getGameOverTimestamp() : Instant.now();
        int added = clanMembershipService.addMembersBySteamId3(clan.getId(), playerSteamId3s, joinedAt);
        logger.info("Added {} players to clan {} from game {}", added, clan.getId(), game.getId());
    }
}
