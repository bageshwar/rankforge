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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.GameActionEvent;
import com.rankforge.core.interfaces.EventProcessor;
import com.rankforge.core.interfaces.LogParser;
import com.rankforge.core.internal.ParseLineResponse;
import com.rankforge.core.stores.EventStore;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main system for processing game logs and computing rankings.
 * 
 * THREADING MODEL:
 * ----------------
 * This class is designed to be INSTANCE-PER-REQUEST. Each log processing request creates a new instance.
 * 
 * Architecture:
 * - Created per HTTP request in LogProcessingService.processLogFileAsync()
 * - Each instance owns its own CS2LogParser, EventProcessor, and other components
 * - NOT thread-safe - should not be shared across threads
 * - Implements AutoCloseable for proper resource cleanup (EntityManager, etc.)
 * 
 * Lifecycle:
 *   1. Created: pipelineService.createGameRankingSystem()
 *   2. Used: rankingSystem.processLines(lines)
 *   3. Closed: rankingSystem.close() in finally block
 * 
 * This design allows:
 * - Parallel processing of multiple log files (each gets its own instance)
 * - Stateful parsing without synchronization concerns
 * - Clean resource management per request
 * 
 * @see com.rankforge.server.service.LogProcessingService#processLogFileAsync
 * @see com.rankforge.server.service.PipelineService#createGameRankingSystem
 * 
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class GameRankingSystem {
    private static final Logger logger = LoggerFactory.getLogger(GameRankingSystem.class);

    private final LogParser logParser;
    private final EventProcessor eventProcessor;
    private final EventStore eventStore;
    private final ScheduledExecutorService scheduler;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    
    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore,
                           ScheduledExecutorService scheduler) {
        this(logParser, eventProcessor, eventStore, scheduler, null, new ObjectMapper());
    }
    
    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore,
                           ScheduledExecutorService scheduler,
                           EntityManager entityManager) {
        this(logParser, eventProcessor, eventStore, scheduler, entityManager, new ObjectMapper());
    }
    
    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore,
                           ScheduledExecutorService scheduler,
                           EntityManager entityManager,
                           ObjectMapper objectMapper) {
        this.logParser = logParser;
        this.eventProcessor = eventProcessor;
        this.eventStore = eventStore;
        this.scheduler = scheduler;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
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

        logger.info("Started processing log file: {}", logFile);
    }

    // open for testing
    void processNewLogLines(String logFile) {
        try {
            List<String> newLines = readNewLines(logFile);
            processLines(newLines);
        } catch (Exception e) {
            logger.error("Error processing log lines", e);
        }
    }

    /**
     * Process a list of log lines directly
     * @param lines the log lines to process
     */
    public void processLines(List<String> lines) {
        logger.info("Starting batch processing of {} log lines", lines.size());
        // appServerId extraction is handled by CS2LogParser.parseLine() - it will be extracted when ResetBreakpadAppId line is encountered

        for (int i = 0; i < lines.size(); i++) {
            Optional<ParseLineResponse> parseLineResponse = logParser.parseLine(lines.get(i), lines, i);
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

                //logger.debug("Adding event {} to batch at index {}", response.getGameEvent().getGameEventType(), i);
                eventProcessor.processEvent(response.getGameEvent());
                eventStore.store(response.getGameEvent());
                
                // move the pointer if more lines have been processed
                i = response.getNextIndex();
            }
        }

        logger.info("Completed batch processing of {} log lines", lines.size());
    }

    private List<String> readNewLines(String logFile) throws IOException {
        return Files.readAllLines(Path.of(logFile));
    }
    
    /**
     * Closes resources associated with this GameRankingSystem.
     * This should be called after processing is complete to prevent connection leaks.
     */
    public void close() {
        if (entityManager != null && entityManager.isOpen()) {
            try {
                // Ensure any active transaction is rolled back before closing
                if (entityManager.getTransaction().isActive()) {
                    logger.warn("Active transaction found during close, rolling back");
                    entityManager.getTransaction().rollback();
                }
                entityManager.close();
                logger.debug("Closed EntityManager for GameRankingSystem");
            } catch (Exception e) {
                logger.error("Error closing EntityManager", e);
            }
        }
        
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                scheduler.shutdown();
                logger.debug("Shutdown scheduler for GameRankingSystem");
            } catch (Exception e) {
                logger.error("Error shutting down scheduler", e);
            }
        }
    }
}