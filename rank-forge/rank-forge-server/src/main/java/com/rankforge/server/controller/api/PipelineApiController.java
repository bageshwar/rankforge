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

import com.rankforge.server.dto.ProcessLogRequest;
import com.rankforge.server.dto.ProcessLogResponse;
import com.rankforge.server.service.LogProcessingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Requires X-API-Key header for authentication
     * 
     * @param request Request containing S3 path to log file
     * @return Response with job ID and status
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessLogResponse> processLogFile(@Valid @RequestBody ProcessLogRequest request) {
        logger.info("Received log processing request for S3 path: {}", request.getS3Path());
        
        try {
            // Validate S3 path format
            if (request.getS3Path() == null || request.getS3Path().trim().isEmpty()) {
                logger.warn("Invalid S3 path provided: {}", request.getS3Path());
                return ResponseEntity.badRequest()
                        .body(new ProcessLogResponse(null, "error", "S3 path is required"));
            }
            
            // Start async processing
            String jobId = logProcessingService.processLogFileAsync(request.getS3Path());
            
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
     * Health check endpoint for pipeline API
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pipeline API is healthy");
    }
}
