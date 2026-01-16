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

import com.rankforge.server.entity.Clan;
import com.rankforge.server.dto.ProcessLogRequest;
import com.rankforge.server.dto.ProcessLogResponse;
import com.rankforge.server.service.LogProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST API Controller for pipeline log processing
 * Author bageshwar.pn
 * Date 2024
 */
@RestController
@RequestMapping("/api/pipeline")
@CrossOrigin(origins = "*") // Allow CORS for frontend development
public class PipelineApiController {

    private static final Logger logger = LoggerFactory.getLogger(PipelineApiController.class);
    
    private final LogProcessingService logProcessingService;

    @Autowired
    public PipelineApiController(LogProcessingService logProcessingService) {
        this.logProcessingService = logProcessingService;
    }

    /**
     * Process a log file from S3
     * Requires X-API-Key header for authentication (per-clan or global key)
     * If per-clan key is used, validates that clan is active and appServerId matches
     * 
     * @param request Request containing S3 path to log file
     * @param httpRequest HTTP request (to get validated clan from SecurityConfig)
     * @return Response with job ID and status
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessLogResponse> processLogFile(@Valid @RequestBody ProcessLogRequest request,
                                                              HttpServletRequest httpRequest) {
        logger.info("Received log processing request for S3 path: {}", request.getS3Path());
        
        try {
            // Validate S3 path format
            if (request.getS3Path() == null || request.getS3Path().trim().isEmpty()) {
                logger.warn("Invalid S3 path provided: {}", request.getS3Path());
                return ResponseEntity.badRequest()
                        .body(new ProcessLogResponse(null, "error", "S3 path is required"));
            }
            
            // Validate S3 path format synchronously before starting async processing
            validateS3PathFormat(request.getS3Path());
            
            // Check if per-clan key was used (clan stored in request attribute by SecurityConfig)
            Clan clan = (Clan) httpRequest.getAttribute("clan");
            if (clan != null) {
                // Per-clan key was used - validate clan is active
                if (!clan.isActive()) {
                    logger.warn("Clan {} is not active (appServerId not configured). Cannot process logs.", clan.getId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ProcessLogResponse(null, "error", 
                                    "Clan is not active. Please configure appServerId before ingesting logs."));
                }
                
                // Store clan in request attribute for LogProcessingService to use
                // The appServerId validation will happen during log processing
                logger.info("Processing logs for clan {} (appServerId: {})", clan.getId(), clan.getAppServerId());
            } else {
                // Global key was used (backward compatibility)
                logger.info("Processing logs using global API key (deprecated)");
            }
            
            // Start async processing and get job ID immediately
            // Pass clan info to LogProcessingService
            CompletableFuture<String> futureJobId = logProcessingService.processLogFileAsync(
                    request.getS3Path(), clan);
            
            // Get the job ID (it's already completed, so this returns immediately)
            String jobId = futureJobId.join();
            
            ProcessLogResponse response = new ProcessLogResponse(
                    jobId,
                    "processing",
                    "Log processing started successfully"
            );
            
            logger.info("Started log processing job {} for S3 path: {}", jobId, request.getS3Path());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid S3 path format: {}", request.getS3Path(), e);
            return ResponseEntity.badRequest()
                    .body(new ProcessLogResponse(null, "error", "Invalid S3 path format: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error starting log processing for S3 path: {}", request.getS3Path(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProcessLogResponse(null, "error", "Failed to start log processing: " + e.getMessage()));
        }
    }

    /**
     * Validates S3 path format synchronously before starting async processing
     * This ensures we return 400 Bad Request immediately for invalid paths
     * 
     * @param s3Path S3 path to validate
     * @throws IllegalArgumentException if path format is invalid
     */
    private void validateS3PathFormat(String s3Path) {
        if (s3Path == null || s3Path.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 path cannot be null or empty");
        }
        
        String path = s3Path.trim();
        
        // Remove s3:// prefix if present
        if (path.startsWith("s3://")) {
            path = path.substring(5);
        }
        
        // Must have bucket/key format (at least one slash)
        int firstSlash = path.indexOf('/');
        if (firstSlash == -1) {
            throw new IllegalArgumentException("Invalid S3 path format. Expected: s3://bucket/key or bucket/key");
        }
        
        String bucket = path.substring(0, firstSlash);
        String key = path.substring(firstSlash + 1);
        
        if (bucket.isEmpty() || key.isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 path format. Bucket and key cannot be empty");
        }
    }

    /**
     * Health check endpoint for pipeline API
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pipeline API is healthy");
    }
}
