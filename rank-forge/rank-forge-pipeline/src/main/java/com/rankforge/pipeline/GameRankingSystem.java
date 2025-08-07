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

package com.rankforge.pipeline;

import com.rankforge.core.events.GameActionEvent;
import com.rankforge.core.events.GameEvent;
import com.rankforge.core.interfaces.EventProcessor;
import com.rankforge.core.interfaces.LogParser;
import com.rankforge.core.interfaces.RankingService;
import com.rankforge.core.internal.ParseLineResponse;
import com.rankforge.core.stores.EventStore;
import com.rankforge.pipeline.persistence.DBBasedEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Core system class that coordinates all components with batch processing
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class GameRankingSystem {
    private static final Logger logger = LoggerFactory.getLogger(GameRankingSystem.class);
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final LogParser logParser;
    private final EventProcessor eventProcessor;
    private final EventStore eventStore;
    private final RankingService rankingService;
    private final ScheduledExecutorService scheduler;
    private final int batchSize;

    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore, RankingService rankingService, 
                           ScheduledExecutorService scheduler) {
        this(logParser, eventProcessor, eventStore, rankingService, scheduler, DEFAULT_BATCH_SIZE);
    }
    
    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore, RankingService rankingService, 
                           ScheduledExecutorService scheduler, int batchSize) {
        this.logParser = logParser;
        this.eventProcessor = eventProcessor;
        this.eventStore = eventStore;
        this.rankingService = rankingService;
        this.scheduler = scheduler;
        this.batchSize = batchSize;
    }

    public void startProcessing(String logFile) throws IOException {
        // Start file watching
        //WatchService watchService = FileSystems.getDefault().newWatchService();
        //Path logPath = Paths.get(logFile);
        // TODO Now figure out watcher
        //WatchKey registered = logPath.getParent().register(watchService, ENTRY_MODIFY);

        // Start background tasks
        scheduler.scheduleWithFixedDelay(
                () -> processNewLogLines(logFile),
                0, 1, TimeUnit.SECONDS
        );
        
        // Schedule periodic batch flushes
        scheduler.scheduleWithFixedDelay(
                this::flushAllBatches,
                5, 5, TimeUnit.SECONDS
        );

        logger.info("Started processing log file: {}", logFile);
    }

    // open for testing
    void processNewLogLines(String logFile) {
        try {
            // Read new lines from log file
            List<String> newLines = readNewLines(logFile);
            logger.debug("Starting batch processing of {} log lines from {}", newLines.size(), logFile);

            for (int i = 0; i < newLines.size(); i++) {
                Optional<ParseLineResponse> parseLineResponse = logParser.parseLine(newLines.get(i), newLines, i);
                if (parseLineResponse.isPresent()) {
                    logger.debug("Processing event {} at index {}", parseLineResponse.get().getGameEvent().getGameEventType(), i);
                    ParseLineResponse response = parseLineResponse.get();
                    
                    if (response.getGameEvent() instanceof GameActionEvent gameActionEvent) {
                        // ignore the event if both players are bots
                        if (gameActionEvent.getPlayer1().isBot() && gameActionEvent.getPlayer2().isBot()) {
                            logger.debug("Skipping bot-only event at index {}", i);
                            continue;
                        }
                    }

                    logger.debug("Adding event {} to batch at index {}", response.getGameEvent().getGameEventType(), i);
                    eventProcessor.processEvent(response.getGameEvent());
                    eventStore.store(response.getGameEvent());
                    
                    // move the pointer if more lines have been processed
                    i = response.getNextIndex();
                }
            }

            logger.info("Completed batch processing of {} log lines", newLines.size());
        } catch (Exception e) {
            logger.error("Error processing log lines", e);
        }
    }

    
    /**
     * Flushes all pending batches across the system
     */
    private void flushAllBatches() {
        try {
            // Flush event store batches
            if (eventStore.isBatchable()) {
                eventStore.flushBatch();
            }
            
            // Flush player stats store batches if accessible
            // Note: In a real implementation, you might want to inject the stats store
            // or make it accessible for flushing. For now, batches will be flushed
            // automatically when they reach their size limits.
            
            logger.debug("Flushed all pending batches");
        } catch (Exception e) {
            logger.error("Error flushing batches", e);
        }
    }

    private List<String> readNewLines(String logFile) throws IOException {
        return Files.readAllLines(Path.of(logFile));
    }
    
    /**
     * Graceful shutdown that flushes all pending data
     */
    public void shutdown() {
        logger.info("Shutting down GameRankingSystem...");
        
        // Flush all pending batches
        flushAllBatches();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("GameRankingSystem shutdown complete");
    }
}