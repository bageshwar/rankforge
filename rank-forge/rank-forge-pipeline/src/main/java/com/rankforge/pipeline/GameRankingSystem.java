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
 * Core system class that coordinates all components with batch processing
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
    
    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore,
                           ScheduledExecutorService scheduler) {
        this(logParser, eventProcessor, eventStore, scheduler, null);
    }
    
    public GameRankingSystem(LogParser logParser, EventProcessor eventProcessor, 
                           EventStore eventStore,
                           ScheduledExecutorService scheduler,
                           EntityManager entityManager) {
        this.logParser = logParser;
        this.eventProcessor = eventProcessor;
        this.eventStore = eventStore;
        this.scheduler = scheduler;
        this.entityManager = entityManager;
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