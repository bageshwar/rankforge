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

import com.rankforge.server.entity.ClanMembership;
import com.rankforge.server.entity.User;
import com.rankforge.server.repository.ClanMembershipRepository;
import com.rankforge.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing clan memberships
 * Handles auto-adding players to clans
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class ClanMembershipService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClanMembershipService.class);
    
    private final ClanMembershipRepository clanMembershipRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public ClanMembershipService(ClanMembershipRepository clanMembershipRepository,
                                 UserRepository userRepository) {
        this.clanMembershipRepository = clanMembershipRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Add a member to a clan if they don't already exist (idempotent)
     * @param clanId The clan ID
     * @param userId The user ID
     * @param joinedAt The timestamp when they joined (or when clan was created for retroactive)
     * @return The membership if created, null if already exists
     */
    @Transactional
    public ClanMembership addMemberIfNotExists(Long clanId, Long userId, Instant joinedAt) {
        if (clanMembershipRepository.existsByClanIdAndUserId(clanId, userId)) {
            logger.debug("User {} already a member of clan {}", userId, clanId);
            return null;
        }
        
        ClanMembership membership = new ClanMembership(clanId, userId, joinedAt);
        ClanMembership saved = clanMembershipRepository.save(membership);
        logger.info("Added user {} to clan {}", userId, clanId);
        return saved;
    }
    
    /**
     * Add a member to a clan if they don't already exist (uses current time)
     */
    @Transactional
    public ClanMembership addMemberIfNotExists(Long clanId, Long userId) {
        return addMemberIfNotExists(clanId, userId, Instant.now());
    }
    
    /**
     * Add multiple users to a clan (idempotent - skips existing members)
     * @param clanId The clan ID
     * @param userIds List of user IDs to add
     * @param joinedAt The timestamp when they joined
     * @return Number of new members added
     */
    @Transactional
    public int addMembersIfNotExists(Long clanId, List<Long> userIds, Instant joinedAt) {
        int added = 0;
        for (Long userId : userIds) {
            if (addMemberIfNotExists(clanId, userId, joinedAt) != null) {
                added++;
            }
        }
        logger.info("Added {} new members to clan {} ({} already existed)", 
            added, clanId, userIds.size() - added);
        return added;
    }
    
    /**
     * Add users to clan by their Steam ID3 (converts to User ID first)
     * Automatically creates User records for Steam ID3s that don't have User records yet
     * @param clanId The clan ID
     * @param steamId3List List of Steam ID3 strings
     * @param joinedAt The timestamp when they joined
     * @return Number of new members added
     */
    @Transactional
    public int addMembersBySteamId3(Long clanId, List<String> steamId3List, Instant joinedAt) {
        List<Long> userIds = new ArrayList<>();
        
        for (String steamId3 : steamId3List) {
            // Try to find existing user
            Optional<User> userOpt = userRepository.findBySteamId3(steamId3);
            
            if (userOpt.isPresent()) {
                // User exists, use their ID
                userIds.add(userOpt.get().getId());
            } else {
                // User doesn't exist, create a minimal User record
                try {
                    User newUser = createUserFromSteamId3(steamId3);
                    User savedUser = userRepository.save(newUser);
                    userIds.add(savedUser.getId());
                    logger.info("Created User record for Steam ID3 {} (ID: {}) during clan association", 
                            steamId3, savedUser.getId());
                } catch (Exception e) {
                    logger.warn("Failed to create User record for Steam ID3 {}: {}", steamId3, e.getMessage());
                    // Continue with other users even if one fails
                }
            }
        }
        
        if (userIds.isEmpty()) {
            logger.debug("No valid users found or created for Steam ID3s: {}", steamId3List);
            return 0;
        }
        
        return addMembersIfNotExists(clanId, userIds, joinedAt);
    }
    
    /**
     * Creates a minimal User record from Steam ID3
     * Converts Steam ID3 to Steam ID64 and creates User with minimal info
     * Full profile data will be fetched when user logs in
     */
    private User createUserFromSteamId3(String steamId3) {
        // Extract numeric account ID from Steam ID3 format [U:1:accountId]
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[U:1:(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(steamId3);
        
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid Steam ID3 format: " + steamId3);
        }
        
        long accountId = Long.parseLong(matcher.group(1));
        // Convert to Steam ID64: steamId64 = accountId * 2 + 76561197960265728
        long steamId64Long = accountId * 2L + 76561197960265728L;
        String steamId64 = String.valueOf(steamId64Long);
        
        // Create minimal User record
        User user = new User();
        user.setSteamId64(steamId64);
        user.setSteamId3(steamId3);
        user.setPersonaName("Player " + accountId); // Placeholder name, will be updated on login
        user.setVacBanned(false);
        user.setCreatedAt(java.time.Instant.now());
        user.setLastLogin(java.time.Instant.now());
        
        return user;
    }
    
    /**
     * Get all clans a user belongs to
     * @param userId The user ID
     * @return List of clan IDs
     */
    public List<Long> getUserClans(Long userId) {
        return clanMembershipRepository.findByUserId(userId).stream()
            .map(ClanMembership::getClanId)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all members of a clan
     * @param clanId The clan ID
     * @return List of user IDs
     */
    public List<Long> getClanMembers(Long clanId) {
        return clanMembershipRepository.findByClanId(clanId).stream()
            .map(ClanMembership::getUserId)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a user is a member of a clan
     */
    public boolean isMember(Long clanId, Long userId) {
        return clanMembershipRepository.existsByClanIdAndUserId(clanId, userId);
    }
}
