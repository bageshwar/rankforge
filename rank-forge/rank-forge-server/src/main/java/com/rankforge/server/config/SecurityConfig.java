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

package com.rankforge.server.config;

import com.rankforge.server.entity.Clan;
import com.rankforge.server.service.ClanService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Security configuration for protected API endpoints.
 * Validates X-API-Key header for /api/pipeline/** and /api/admin/** endpoints.
 * For pipeline endpoints: validates per-clan API keys first, falls back to global key (deprecated).
 * For admin endpoints: uses global API key.
 * Author bageshwar.pn
 * Date 2024
 */
@Component
@Order(1)
public class SecurityConfig extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String PIPELINE_API_PATH = "/api/pipeline/";
    private static final String ADMIN_API_PATH = "/api/admin/";
    private static final String PIPELINE_HEALTH_ENDPOINT = "/api/pipeline/health";
    private static final String ADMIN_HEALTH_ENDPOINT = "/api/admin/health";
    private static final String CLAN_ATTRIBUTE = "clan"; // Request attribute to store validated clan

    @Value("${rankforge.api.key:}")
    private String configuredApiKey;
    
    @Autowired(required = false)
    private ClanService clanService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Check if this is a protected endpoint (pipeline or admin)
        boolean isPipelineEndpoint = requestPath.startsWith(PIPELINE_API_PATH) && 
                                    !requestPath.equals(PIPELINE_HEALTH_ENDPOINT);
        boolean isAdminEndpoint = requestPath.startsWith(ADMIN_API_PATH) && 
                                  !requestPath.equals(ADMIN_HEALTH_ENDPOINT);
        
        if (isPipelineEndpoint || isAdminEndpoint) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            
            if (apiKey == null || apiKey.isEmpty()) {
                String endpointType = isAdminEndpoint ? "admin" : "pipeline";
                logger.warn("Unauthorized access attempt to {} endpoint from {} - missing API key - path: {}", 
                        endpointType, request.getRemoteAddr(), requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized. Missing API key.\"}");
                return;
            }
            
            boolean authorized = false;
            
            // For pipeline endpoints: try per-clan key first, then fall back to global key
            if (isPipelineEndpoint) {
                // Try per-clan API key validation first
                if (clanService != null) {
                    Optional<Clan> clanOpt = clanService.validateApiKey(apiKey);
                    if (clanOpt.isPresent()) {
                        Clan clan = clanOpt.get();
                        // Store clan in request attribute for use in PipelineApiController
                        request.setAttribute(CLAN_ATTRIBUTE, clan);
                        authorized = true;
                        logger.debug("Authorized pipeline request using per-clan API key for clan {}", clan.getId());
                    }
                }
                
                // Fall back to global key (backward compatibility, deprecated)
                if (!authorized && configuredApiKey != null && !configuredApiKey.isEmpty() && apiKey.equals(configuredApiKey)) {
                    authorized = true;
                    logger.warn("Pipeline request authorized using DEPRECATED global API key. " +
                            "Please migrate to per-clan API keys. Path: {}", requestPath);
                }
            } else if (isAdminEndpoint) {
                // Admin endpoints: use global key only
                if (configuredApiKey == null || configuredApiKey.isEmpty()) {
                    logger.error("API key not configured. Please set rankforge.api.key property.");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"API key not configured\"}");
                    return;
                }
                
                if (apiKey.equals(configuredApiKey)) {
                    authorized = true;
                }
            }
            
            if (!authorized) {
                String endpointType = isAdminEndpoint ? "admin" : "pipeline";
                logger.warn("Unauthorized access attempt to {} endpoint from {} - invalid API key - path: {}", 
                        endpointType, request.getRemoteAddr(), requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized. Invalid API key.\"}");
                return;
            }
            
            String endpointType = isAdminEndpoint ? "admin" : "pipeline";
            logger.debug("Authorized request to {} endpoint: {}", endpointType, requestPath);
        }
        
        filterChain.doFilter(request, response);
    }
}
