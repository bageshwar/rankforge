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

import com.rankforge.core.events.GameActionEvent;
import com.rankforge.core.interfaces.LogParser;
import com.rankforge.core.internal.ParseLineResponse;
import com.rankforge.pipeline.GameRankingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
            try {
                // Download file from S3 into memory
                List<String> lines = s3Service.downloadFileAsLines(s3Path);
                logger.info("Downloaded {} lines from S3 for job {}", lines.size(), jobId);
                
                // Create pipeline components for this job
                GameRankingSystem rankingSystem = pipelineService.createGameRankingSystem();
                
                // Process lines using pipeline
                processLogLines(rankingSystem, lines, s3Path);
                
                logger.info("Successfully completed log processing job {} for S3 path: {}", jobId, s3Path);
                
            } catch (Exception e) {
                logger.error("Error processing log file for job {} from S3 path: {}", jobId, s3Path, e);
                // Error is logged but doesn't fail the request since it's async
            }
        });
        
        // Return the jobId immediately
        return CompletableFuture.completedFuture(jobId);
    }

    /**
     * Processes log lines using the GameRankingSystem
     * This mimics the behavior of GameRankingSystem.processNewLogLines but works with in-memory lines
     */
    private void processLogLines(GameRankingSystem rankingSystem, List<String> lines, String source) {
        logger.debug("Starting batch processing of {} log lines from {}", lines.size(), source);
        
        // Access the log parser and event processor through reflection or create a helper method
        // Since processNewLogLines is package-private, we'll replicate the logic here
        // We need to get the logParser and eventProcessor from GameRankingSystem
        // For now, let's create a new method that processes lines directly
        
        try {
            // Use reflection to access private fields or create a public method
            // Actually, let's create a wrapper that processes lines
            processLinesDirectly(rankingSystem, lines);
            
            logger.info("Completed batch processing of {} log lines from {}", lines.size(), source);
        } catch (Exception e) {
            logger.error("Error processing log lines from {}", source, e);
            throw new RuntimeException("Failed to process log lines", e);
        }
    }

    /**
     * Processes lines directly by accessing GameRankingSystem components
     * This replicates the logic from GameRankingSystem.processNewLogLines
     */
    private void processLinesDirectly(GameRankingSystem rankingSystem, List<String> lines) {
        // We need to access the internal components
        // Since they're private, we'll use a helper method or reflection
        // For now, let's create a temporary file approach or extend GameRankingSystem
        // Actually, the best approach is to create a method in GameRankingSystem that accepts lines
        // But since we can't modify it, let's use reflection or create a wrapper
        
        // For simplicity, let's create a wrapper class that extends functionality
        // Or better: create a helper that uses the same logic
        
        // Since GameRankingSystem doesn't expose a method to process lines directly,
        // we'll need to create a helper that replicates the processing logic
        // Let me check if we can access the components via a method or if we need reflection
        
        // Actually, let's create a ProcessingHelper that can work with the components
        ProcessingHelper helper = new ProcessingHelper(rankingSystem);
        helper.processLines(lines);
    }

    /**
     * Helper class to process lines using GameRankingSystem components
     */
    private class ProcessingHelper {
        private static final Logger helperLogger = LoggerFactory.getLogger(ProcessingHelper.class);
        private final GameRankingSystem rankingSystem;
        
        ProcessingHelper(GameRankingSystem rankingSystem) {
            this.rankingSystem = rankingSystem;
        }
        
        void processLines(List<String> lines) {
            // Use reflection to access private fields
            try {
                java.lang.reflect.Field logParserField = GameRankingSystem.class.getDeclaredField("logParser");
                logParserField.setAccessible(true);
                LogParser logParser = (LogParser) logParserField.get(rankingSystem);
                
                java.lang.reflect.Field eventProcessorField = GameRankingSystem.class.getDeclaredField("eventProcessor");
                eventProcessorField.setAccessible(true);
                com.rankforge.core.interfaces.EventProcessor eventProcessor = 
                    (com.rankforge.core.interfaces.EventProcessor) eventProcessorField.get(rankingSystem);
                
                java.lang.reflect.Field eventStoreField = GameRankingSystem.class.getDeclaredField("eventStore");
                eventStoreField.setAccessible(true);
                com.rankforge.core.stores.EventStore eventStore = 
                    (com.rankforge.core.stores.EventStore) eventStoreField.get(rankingSystem);
                
                // Process lines using the same logic as GameRankingSystem.processNewLogLines
                for (int i = 0; i < lines.size(); i++) {
                    Optional<ParseLineResponse> parseLineResponse = logParser.parseLine(lines.get(i), lines, i);
                    if (parseLineResponse.isPresent()) {
                        helperLogger.debug("Processing event {} at index {}", 
                                parseLineResponse.get().getGameEvent().getGameEventType(), i);
                        ParseLineResponse response = parseLineResponse.get();
                        
                        if (response.getGameEvent() instanceof GameActionEvent gameActionEvent) {
                            // ignore the event if both players are bots
                            if (gameActionEvent.getPlayer1().isBot() && gameActionEvent.getPlayer2().isBot()) {
                                helperLogger.debug("Skipping bot-only event at index {}", i);
                                continue;
                            }
                        }

                        helperLogger.debug("Adding event {} to batch at index {}", 
                                response.getGameEvent().getGameEventType(), i);
                        eventProcessor.processEvent(response.getGameEvent());
                        eventStore.store(response.getGameEvent());
                        
                        // move the pointer if more lines have been processed
                        i = response.getNextIndex();
                    }
                }
            } catch (Exception e) {
                helperLogger.error("Error accessing GameRankingSystem components via reflection", e);
                throw new RuntimeException("Failed to process log lines", e);
            }
        }
    }
}
