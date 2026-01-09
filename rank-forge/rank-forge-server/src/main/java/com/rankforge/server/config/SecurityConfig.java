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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security configuration for pipeline API endpoints
 * Validates X-API-Key header for /api/pipeline/** endpoints
 * Author bageshwar.pn
 * Date 2024
 */
@Component
@Order(1)
public class SecurityConfig extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String PIPELINE_API_PATH = "/api/pipeline/";
    private static final String HEALTH_ENDPOINT = "/api/pipeline/health";

    @Value("${rankforge.api.key:}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only apply security to pipeline endpoints, but exclude health endpoint
        if (requestPath.startsWith(PIPELINE_API_PATH) && !requestPath.equals(HEALTH_ENDPOINT)) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            
            // Validate API key
            if (configuredApiKey == null || configuredApiKey.isEmpty()) {
                logger.error("API key not configured. Please set rankforge.api.key property.");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"API key not configured\"}");
                return;
            }
            
            if (apiKey == null || !apiKey.equals(configuredApiKey)) {
                logger.warn("Unauthorized access attempt to pipeline endpoint from {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized. Invalid or missing API key.\"}");
                return;
            }
            
            logger.debug("Authorized request to pipeline endpoint: {}", requestPath);
        }
        
        filterChain.doFilter(request, response);
    }
}
