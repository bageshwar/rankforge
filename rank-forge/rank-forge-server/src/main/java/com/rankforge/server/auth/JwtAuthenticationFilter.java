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

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Authentication Filter
 * Extracts and validates JWT tokens from Authorization header
 * Author bageshwar.pn
 * Date 2026
 */
@Component
@Order(2) // Run after API key filter (Order 1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/",
        "/api/rankings/",
        "/api/games/",
        "/api/players/",
        "/api/pipeline/",
        "/api/admin/",
        "/health",
        "/error"
    );
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip JWT validation for public endpoints
        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract JWT token from Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String token = null;
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        }
        
        if (token == null || token.isEmpty()) {
            logger.debug("No JWT token found for protected endpoint: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized. Missing or invalid JWT token.\"}");
            return;
        }
        
        // Validate token
        if (!jwtService.validateToken(token)) {
            logger.warn("Invalid JWT token for endpoint: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized. Invalid or expired JWT token.\"}");
            return;
        }
        
        // Extract claims and set as request attribute for controllers to use
        try {
            Claims claims = jwtService.extractAllClaims(token);
            request.setAttribute("steamId64", claims.getSubject());
            request.setAttribute("steamId3", claims.get("steamId3", String.class));
            request.setAttribute("userId", claims.get("userId"));
            request.setAttribute("personaName", claims.get("personaName", String.class));
            
            logger.debug("JWT token validated for user: {}", claims.getSubject());
        } catch (Exception e) {
            logger.error("Error extracting JWT claims: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized. Invalid JWT token.\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if path is public (doesn't require authentication)
     */
    private boolean isPublicPath(String path) {
        // Exact match for specific endpoints
        if (path.equals("/api/users/me")) {
            return false; // This requires authentication
        }
        
        // Check if path starts with any public prefix
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        
        // Static resources and root paths
        if (path.startsWith("/static/") || 
            path.startsWith("/assets/") || 
            path.equals("/") ||
            path.startsWith("/index.html")) {
            return true;
        }
        
        return false;
    }
}
