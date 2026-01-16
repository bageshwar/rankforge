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
import com.rankforge.server.entity.Clan;
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
    private final PlayerRankingService playerRankingService;

    @Autowired
    public LogProcessingService(S3Service s3Service, PipelineService pipelineService, PlayerRankingService playerRankingService) {
        this.s3Service = s3Service;
        this.pipelineService = pipelineService;
        this.playerRankingService = playerRankingService;
    }

    /**
     * Processes a log file from S3 asynchronously
     * Backward-compatible overload (uses null for clan)
     * 
     * @param s3Path S3 path to the log file
     * @return CompletableFuture containing the job ID for tracking the processing
     */
    @Async
    public CompletableFuture<String> processLogFileAsync(String s3Path) {
        return processLogFileAsync(s3Path, null);
    }
    
    /**
     * Processes a log file from S3 asynchronously
     * 
     * @param s3Path S3 path to the log file
     * @param clan Clan associated with the API key (null if global key was used)
     * @return CompletableFuture containing the job ID for tracking the processing
     */
    @Async
    public CompletableFuture<String> processLogFileAsync(String s3Path, Clan clan) {
        // Generate job ID immediately so it can be returned to the caller
        String jobId = UUID.randomUUID().toString();
        logger.info("Starting async log processing job {} for S3 path: {} (clan: {})", 
                jobId, s3Path, clan != null ? clan.getId() : "global-key");
        
        // Process asynchronously but return jobId immediately
        CompletableFuture.runAsync(() -> {
            GameRankingSystem rankingSystem = null;
            try {
                // Download file from S3 into memory
                List<String> lines = s3Service.downloadFileAsLines(s3Path);
                logger.info("Downloaded {} lines from S3 for job {}", lines.size(), jobId);
                
                // If per-clan key was used, validate appServerId during processing
                // This validation happens in the pipeline when appServerId is extracted
                // Pass clan info to pipeline service
                rankingSystem = pipelineService.createGameRankingSystem(clan);
                
                // Process lines using pipeline
                processLogLines(rankingSystem, lines, s3Path, clan);
                
                // Evict cache for the clan after processing completes
                if (clan != null && clan.getId() != null) {
                    try {
                        playerRankingService.evictCacheForClan(clan.getId());
                        logger.info("Evicted leaderboard cache for clan {} after processing job {}", clan.getId(), jobId);
                    } catch (Exception e) {
                        logger.warn("Failed to evict cache for clan {} after processing job {}: {}", 
                                clan.getId(), jobId, e.getMessage());
                        // Don't fail the job if cache eviction fails
                    }
                }
                
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
     * If clan is provided, validates appServerId matches during processing
     */
    private void processLogLines(GameRankingSystem rankingSystem, List<String> lines, String source, Clan clan) {
        logger.info("Processing {} log lines from {} (clan: {})", 
                lines.size(), source, clan != null ? clan.getId() : "global-key");
        try {
            rankingSystem.processLines(lines, clan);
            logger.info("Completed processing {} log lines from {}", lines.size(), source);
        } catch (Exception e) {
            logger.error("Exception during processLines for {} lines from {}", lines.size(), source, e);
            throw e;
        }
    }
}
