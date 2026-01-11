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

import com.rankforge.pipeline.GameRankingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing log files asynchronously
 * Downloads from S3 and processes using pipeline components
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class LogProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogProcessingService.class);
    
    private final S3Service s3Service;
    private final PipelineService pipelineService;

    @Autowired
    public LogProcessingService(S3Service s3Service, PipelineService pipelineService) {
        this.s3Service = s3Service;
        this.pipelineService = pipelineService;
    }

    /**
     * Processes a log file from S3 asynchronously
     * 
     * @param s3Path S3 path to the log file
     * @return CompletableFuture containing the job ID for tracking the processing
     */
    @Async
    public CompletableFuture<String> processLogFileAsync(String s3Path) {
        // Generate job ID immediately so it can be returned to the caller
        String jobId = UUID.randomUUID().toString();
        logger.info("Starting async log processing job {} for S3 path: {}", jobId, s3Path);
        
        // Process asynchronously but return jobId immediately
        CompletableFuture.runAsync(() -> {
            GameRankingSystem rankingSystem = null;
            try {
                // Download file from S3 into memory
                List<String> lines = s3Service.downloadFileAsLines(s3Path);
                logger.info("Downloaded {} lines from S3 for job {}", lines.size(), jobId);
                
                // Create pipeline components for this job
                rankingSystem = pipelineService.createGameRankingSystem();
                
                // Process lines using pipeline
                processLogLines(rankingSystem, lines, s3Path);
                
                logger.info("Successfully completed log processing job {} for S3 path: {}", jobId, s3Path);
                
            } catch (Exception e) {
                logger.error("Error processing log file for job {} from S3 path: {}", jobId, s3Path, e);
                // Error is logged but doesn't fail the request since it's async
            } finally {
                // Always close resources to prevent connection leaks
                if (rankingSystem != null) {
                    try {
                        rankingSystem.close();
                        logger.debug("Cleaned up resources for job {}", jobId);
                    } catch (Exception e) {
                        logger.error("Error cleaning up resources for job {}", jobId, e);
                    }
                }
            }
        });
        
        // Return the jobId immediately
        return CompletableFuture.completedFuture(jobId);
    }

    /**
     * Processes log lines using the GameRankingSystem
     */
    private void processLogLines(GameRankingSystem rankingSystem, List<String> lines, String source) {
        logger.debug("Processing {} log lines from {}", lines.size(), source);
        rankingSystem.processLines(lines);
        logger.info("Completed processing {} log lines from {}", lines.size(), source);
    }
}
