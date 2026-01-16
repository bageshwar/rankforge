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

import com.rankforge.server.auth.JwtService;
import com.rankforge.server.auth.SteamAuthService;
import com.rankforge.server.dto.AuthResponseDTO;
import com.rankforge.server.dto.SteamUserProfileDTO;
import com.rankforge.server.dto.UserDTO;
import com.rankforge.server.entity.User;
import com.rankforge.server.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for Steam authentication
 * Author bageshwar.pn
 * Date 2026
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private SteamAuthService steamAuthService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;
    
    @Value("${steam.openid.frontend-callback:http://localhost:5173/auth/callback}")
    private String frontendCallbackUrl;
    
    /**
     * Initiate Steam login - redirects to Steam OpenID
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        String steamLoginUrl = steamAuthService.buildSteamLoginUrl();
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", steamLoginUrl);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Handle Steam OpenID callback
     * Validates the response, creates/updates user, generates JWT, and redirects to frontend
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(HttpServletRequest request) {
        try {
            // Extract OpenID parameters from request
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });
            
            // Validate and extract Steam ID
            String steamId64 = steamAuthService.validateAndExtractSteamId(params);
            logger.info("Steam authentication successful for: {}", steamId64);
            
            // Fetch user profile from Steam API
            SteamUserProfileDTO steamProfile = steamAuthService.fetchSteamProfile(steamId64);
            
            // Create or update user in database
            User user = userService.createOrUpdateUser(steamProfile);
            
            // Generate JWT token
            String token = jwtService.generateToken(user);
            Long expiresAt = Instant.now().plusMillis(jwtService.getExpiration()).getEpochSecond();
            
            // Create response DTO
            UserDTO userDTO = new UserDTO(user);
            AuthResponseDTO authResponse = new AuthResponseDTO(token, userDTO, expiresAt);
            
            // Redirect to frontend with token in query parameter
            String redirectUrl = frontendCallbackUrl + "?token=" + token;
            
            // Return redirect response
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .body(authResponse);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid Steam authentication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid Steam authentication: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("Error fetching Steam profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch Steam profile: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during Steam authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
    
    /**
     * Logout endpoint (client-side token removal, no server-side action needed)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Missing or invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            
            // Validate existing token
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }
            
            // Get user from token
            String steamId64 = jwtService.extractSteamId64(token);
            var userOpt = userService.findBySteamId64(steamId64);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
            
            // Generate new token
            User user = userOpt.get();
            String newToken = jwtService.generateToken(user);
            Long expiresAt = Instant.now().plusMillis(jwtService.getExpiration()).getEpochSecond();
            
            UserDTO userDTO = new UserDTO(user);
            AuthResponseDTO authResponse = new AuthResponseDTO(newToken, userDTO, expiresAt);
            
            return ResponseEntity.ok(authResponse);
            
        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to refresh token"));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth API is healthy");
    }
}
