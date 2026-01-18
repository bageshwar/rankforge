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

import com.rankforge.server.auth.SteamAuthService;
import com.rankforge.server.dto.SteamUserProfileDTO;
import com.rankforge.server.dto.UserDTO;
import com.rankforge.server.entity.User;
import com.rankforge.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for user management
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final SteamAuthService steamAuthService;
    
    @Autowired
    public UserService(UserRepository userRepository, SteamAuthService steamAuthService) {
        this.userRepository = userRepository;
        this.steamAuthService = steamAuthService;
    }
    
    /**
     * Create or update user from Steam profile
     */
    @Transactional
    public User createOrUpdateUser(SteamUserProfileDTO steamProfile) {
        String steamId64 = steamProfile.getSteamId64();
        
        Optional<User> existingUser = userRepository.findBySteamId64(steamId64);
        User user;
        
        if (existingUser.isPresent()) {
            user = existingUser.get();
            logger.debug("Updating existing user: {}", steamId64);
        } else {
            user = new User();
            user.setSteamId64(steamId64);
            user.setSteamId3(steamAuthService.convertToSteamId3(steamId64));
            logger.debug("Creating new user: {}", steamId64);
        }
        
        // Update user data from Steam profile
        user.setPersonaName(steamProfile.getPersonaName());
        user.setProfileUrl(steamProfile.getProfileUrl());
        user.setAvatarUrl(steamProfile.getAvatarFullUrl());
        user.setAvatarMediumUrl(steamProfile.getAvatarMediumUrl());
        user.setAvatarSmallUrl(steamProfile.getAvatarUrl());
        user.setCountry(steamProfile.getCountryCode());
        
        if (steamProfile.getAccountCreatedInstant() != null) {
            user.setAccountCreated(steamProfile.getAccountCreatedInstant());
        }
        
        // Fetch VAC ban status
        try {
            boolean vacBanned = steamAuthService.fetchVacBanStatus(steamId64);
            user.setVacBanned(vacBanned);
        } catch (Exception e) {
            logger.warn("Failed to fetch VAC ban status for user {}: {}", steamId64, e.getMessage());
            // Don't fail user creation if VAC check fails
        }
        
        // Update last login
        user.setLastLogin(Instant.now());
        
        User savedUser = userRepository.save(user);
        logger.info("User saved: {} ({})", savedUser.getPersonaName(), savedUser.getSteamId64());
        
        return savedUser;
    }
    
    /**
     * Find user by Steam ID64
     */
    public Optional<User> findBySteamId64(String steamId64) {
        return userRepository.findBySteamId64(steamId64);
    }
    
    /**
     * Find user by Steam ID3
     */
    public Optional<User> findBySteamId3(String steamId3) {
        return userRepository.findBySteamId3(steamId3);
    }
    
    /**
     * Get user DTO by Steam ID64
     */
    public Optional<UserDTO> getUserDTOBySteamId64(String steamId64) {
        return userRepository.findBySteamId64(steamId64)
                .map(UserDTO::new);
    }
    
    /**
     * Update last login timestamp
     */
    @Transactional
    public User updateLastLogin(String steamId64) {
        Optional<User> userOpt = userRepository.findBySteamId64(steamId64);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLogin(Instant.now());
            return userRepository.save(user);
        }
        return null;
    }
    
    /**
     * Get user with stats (for My Profile page)
     */
    public Optional<UserDTO> getUserWithStats(String steamId64) {
        Optional<User> userOpt = userRepository.findBySteamId64(steamId64);
        if (userOpt.isPresent()) {
            return Optional.of(new UserDTO(userOpt.get()));
        }
        return Optional.empty();
    }
}
