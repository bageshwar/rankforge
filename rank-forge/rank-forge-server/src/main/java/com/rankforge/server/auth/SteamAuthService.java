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

package com.rankforge.server.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.server.dto.SteamUserProfileDTO;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Steam OpenID authentication and Steam Web API integration
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class SteamAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamAuthService.class);
    
    private static final String STEAM_OPENID_URL = "https://steamcommunity.com/openid/login";
    private static final String STEAM_API_BASE = "https://api.steampowered.com";
    
    @Value("${steam.openid.realm:http://localhost:8080}")
    private String openIdRealm;
    
    @Value("${steam.openid.return-url:http://localhost:8080/api/auth/callback}")
    private String openIdReturnUrl;
    
    @Value("${steam.api.key:}")
    private String steamApiKey;
    
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    public SteamAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.createDefault();
    }
    
    @PostConstruct
    public void validateConfiguration() {
        if (steamApiKey == null || steamApiKey.isEmpty()) {
            logger.warn("Steam API key is not configured. Steam authentication and profile fetching will fail.");
            logger.warn("Get your API key from: https://steamcommunity.com/dev/apikey");
        }
    }
    
    /**
     * Build Steam OpenID login URL
     */
    public String buildSteamLoginUrl() {
        Map<String, String> params = new HashMap<>();
        params.put("openid.ns", "http://specs.openid.net/auth/2.0");
        params.put("openid.mode", "checkid_setup");
        params.put("openid.return_to", openIdReturnUrl);
        params.put("openid.realm", openIdRealm);
        params.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
        params.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");
        
        StringBuilder url = new StringBuilder(STEAM_OPENID_URL);
        url.append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                url.append("&");
            }
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            url.append("=");
            url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        
        return url.toString();
    }
    
    /**
     * Validate Steam OpenID response and extract Steam ID64
     */
    public String validateAndExtractSteamId(Map<String, String> params) throws IOException {
        // Verify the response is from Steam
        if (!params.containsKey("openid.identity") || !params.containsKey("openid.return_to")) {
            throw new IllegalArgumentException("Invalid OpenID response: missing required parameters");
        }
        
        String identity = params.get("openid.identity");
        if (!identity.startsWith("https://steamcommunity.com/openid/id/")) {
            throw new IllegalArgumentException("Invalid OpenID identity: not from Steam");
        }
        
        // Extract Steam ID64 from identity URL
        String steamId64 = identity.substring("https://steamcommunity.com/openid/id/".length());
        
        // Validate Steam ID64 is numeric
        if (!steamId64.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid Steam ID64 format");
        }
        
        // Verify the OpenID response by making a verification request
        if (!verifyOpenIdResponse(params)) {
            throw new IllegalArgumentException("OpenID response verification failed");
        }
        
        return steamId64;
    }
    
    /**
     * Verify OpenID response with Steam
     */
    private boolean verifyOpenIdResponse(Map<String, String> params) throws IOException {
        // Build verification request
        Map<String, String> verifyParams = new HashMap<>(params);
        verifyParams.put("openid.mode", "check_authentication");
        
        StringBuilder url = new StringBuilder(STEAM_OPENID_URL);
        url.append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : verifyParams.entrySet()) {
            if (!first) {
                url.append("&");
            }
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            url.append("=");
            url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        
        // Make verification request
        HttpGet request = new HttpGet(url.toString());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            // Steam returns "is_valid:true" on success
            return responseBody.contains("is_valid:true");
        } catch (ParseException e) {
            throw new IOException("Failed to parse OpenID verification response", e);
        }
    }
    
    /**
     * Fetch user profile from Steam Web API
     */
    public SteamUserProfileDTO fetchSteamProfile(String steamId64) throws IOException {
        if (steamApiKey == null || steamApiKey.isEmpty()) {
            throw new IllegalStateException("Steam API key not configured");
        }
        
        String url = STEAM_API_BASE + "/ISteamUser/GetPlayerSummaries/v2/" +
                "?key=" + URLEncoder.encode(steamApiKey, StandardCharsets.UTF_8) +
                "&steamids=" + steamId64;
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() != 200) {
                throw new IOException("Steam API returned status: " + response.getCode());
            }
            
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode root = objectMapper.readTree(responseBody);
            
            JsonNode players = root.path("response").path("players");
            if (!players.isArray() || players.size() == 0) {
                throw new IOException("No player data found for Steam ID: " + steamId64);
            }
            
            JsonNode player = players.get(0);
            return objectMapper.treeToValue(player, SteamUserProfileDTO.class);
        } catch (ParseException e) {
            throw new IOException("Failed to parse Steam API response", e);
        }
    }
    
    /**
     * Fetch VAC ban status from Steam Web API
     */
    public boolean fetchVacBanStatus(String steamId64) throws IOException {
        if (steamApiKey == null || steamApiKey.isEmpty()) {
            logger.warn("Steam API key not configured, skipping VAC ban check");
            return false;
        }
        
        String url = STEAM_API_BASE + "/ISteamUser/GetPlayerBans/v1/" +
                "?key=" + URLEncoder.encode(steamApiKey, StandardCharsets.UTF_8) +
                "&steamids=" + steamId64;
        
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() != 200) {
                logger.warn("Steam API returned status: {} for VAC ban check", response.getCode());
                return false;
            }
            
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode root = objectMapper.readTree(responseBody);
            
            JsonNode players = root.path("players");
            if (!players.isArray() || players.size() == 0) {
                return false;
            }
            
            JsonNode player = players.get(0);
            return player.path("VACBanned").asBoolean(false);
        } catch (ParseException e) {
            logger.warn("Failed to parse VAC ban check response: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Convert Steam ID64 to Steam ID3 format [U:1:xxx]
     */
    public String convertToSteamId3(String steamId64) {
        try {
            long steamId64Long = Long.parseLong(steamId64);
            // Steam ID3 format: [U:1:accountId]
            // accountId = (steamId64 - 76561197960265728) / 2
            long accountId = (steamId64Long - 76561197960265728L) / 2;
            return "[U:1:" + accountId + "]";
        } catch (NumberFormatException e) {
            logger.error("Invalid Steam ID64 format: {}", steamId64);
            throw new IllegalArgumentException("Invalid Steam ID64 format", e);
        }
    }
}
