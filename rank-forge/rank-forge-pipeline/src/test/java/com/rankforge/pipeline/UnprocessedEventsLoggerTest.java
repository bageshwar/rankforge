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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.GameEvent;
import com.rankforge.core.events.GameEventType;
import com.rankforge.core.internal.ParseLineResponse;
import com.rankforge.core.stores.EventStore;
import com.rankforge.core.util.ObjectMapperFactory;
import com.rankforge.pipeline.persistence.AccoladeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test class to identify and log all unique unprocessed event lines from a production log file.
 * This helps identify which events we are not currently processing.
 * 
 * Usage: Supply a sample production log file path and run this test to see all unique
 * log lines that don't match any of our known event patterns.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unprocessed Events Logger")
class UnprocessedEventsLoggerTest {
    
    private static final Logger logger = LoggerFactory.getLogger(UnprocessedEventsLoggerTest.class);
    
    @Mock
    private EventStore eventStore;
    
    @Mock
    private AccoladeStore accoladeStore;
    
    private CS2LogParser parser;
    private ObjectMapper objectMapper;
    
    // Set this to the path of your production log file
    // Example: "data/c1bfae12f00e0ef7fbacdcd0d6504637a03d70ee991c011ac3eeb3ee82b943a7-json.log"
    // The path is relative to the project root (rankforge/)
    private static final String LOG_FILE_PATH = System.getProperty("log.file.path", 
            "../../../data/c1bfae12f00e0ef7fbacdcd0d6504637a03d70ee991c011ac3eeb3ee82b943a7-json.log");
    
    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.createObjectMapper();
        parser = new CS2LogParser(objectMapper, eventStore, accoladeStore);
        
        // Mock eventStore to return empty (no games processed yet)
        when(eventStore.getGameEvent(eq(GameEventType.GAME_OVER), any()))
            .thenReturn(Optional.empty());
    }
    
    @Test
    @DisplayName("Log all unique unprocessed event lines from production log file")
    void logUnprocessedEvents() throws IOException {
        Path logFilePath = Paths.get(LOG_FILE_PATH);
        
        if (!Files.exists(logFilePath)) {
            logger.warn("Log file not found at: {}. Please set -Dlog.file.path=<path> or update LOG_FILE_PATH", LOG_FILE_PATH);
            logger.warn("Skipping test - no log file to process");
            return;
        }
        
        logger.info("Reading log file: {}", logFilePath.toAbsolutePath());
        List<String> lines = Files.readAllLines(logFilePath);
        logger.info("Read {} lines from log file", lines.size());
        
        // Track processed and unprocessed lines
        Set<String> processedEventTypes = new HashSet<>();
        Set<String> unprocessedLines = new LinkedHashSet<>(); // Use LinkedHashSet to preserve order
        Map<String, Integer> unprocessedLineCounts = new HashMap<>();
        
        int processedCount = 0;
        int unprocessedCount = 0;
        int errorCount = 0;
        
        // Process each line
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            try {
                // Extract the actual log content from JSON wrapper
                JsonNode jsonNode = objectMapper.readTree(line);
                if (!jsonNode.has("log")) {
                    continue; // Skip lines without "log" field
                }
                
                String logContent = jsonNode.get("log").asText().trim();
                
                // Skip empty lines and non-game log lines
                if (logContent.isEmpty() || !logContent.startsWith("L ")) {
                    continue;
                }
                
                // Try to parse the line
                Optional<ParseLineResponse> response = parser.parseLine(line, lines, i);
                
                if (response.isPresent()) {
                    GameEvent event = response.get().getGameEvent();
                    processedEventTypes.add(event.getGameEventType().name());
                    processedCount++;
                } else {
                    // This line was not processed - check if it's a known skip pattern
                    if (!shouldSkipLine(logContent)) {
                        unprocessedLines.add(logContent);
                        unprocessedLineCounts.put(logContent, unprocessedLineCounts.getOrDefault(logContent, 0) + 1);
                        unprocessedCount++;
                    }
                }
            } catch (Exception e) {
                errorCount++;
                logger.debug("Error processing line {}: {}", i, e.getMessage());
            }
        }
        
        // Log summary
        logger.info("=".repeat(80));
        logger.info("PROCESSING SUMMARY");
        logger.info("=".repeat(80));
        logger.info("Total lines processed: {}", lines.size());
        logger.info("Successfully processed events: {}", processedCount);
        logger.info("Unprocessed lines: {}", unprocessedCount);
        logger.info("Error lines: {}", errorCount);
        logger.info("");
        logger.info("Processed event types: {}", processedEventTypes);
        logger.info("");
        
        // Log unique unprocessed lines
        logger.info("=".repeat(80));
        logger.info("UNIQUE UNPROCESSED EVENT LINES ({} unique)", unprocessedLines.size());
        logger.info("=".repeat(80));
        
        // Sort by frequency (most common first) for easier analysis
        List<Map.Entry<String, Integer>> sortedUnprocessed = unprocessedLineCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        for (Map.Entry<String, Integer> entry : sortedUnprocessed) {
            logger.info("[Count: {}] {}", entry.getValue(), entry.getKey());
        }
        
        logger.info("=".repeat(80));
        logger.info("END OF UNPROCESSED EVENTS REPORT");
        logger.info("=".repeat(80));
    }
    
    /**
     * Determine if a log line should be skipped (not considered unprocessed).
     * These are lines that we intentionally don't process (e.g., system logs, non-game events).
     */
    private boolean shouldSkipLine(String logContent) {
        // Skip lines that are clearly not game events
        String lowerContent = logContent.toLowerCase();
        
        // Skip system/startup messages
        if (lowerContent.contains("redirecting stderr") ||
            lowerContent.contains("logging directory") ||
            lowerContent.contains("checking for available updates") ||
            lowerContent.contains("verifying installation") ||
            lowerContent.contains("downloading update") ||
            lowerContent.contains("extracting package") ||
            lowerContent.contains("installing update") ||
            lowerContent.contains("cleaning up") ||
            lowerContent.contains("update complete") ||
            lowerContent.contains("steam console client") ||
            lowerContent.contains("loading steam api") ||
            lowerContent.contains("connecting anonymously") ||
            lowerContent.contains("waiting for client config") ||
            lowerContent.contains("waiting for user info") ||
            lowerContent.contains("success! app") ||
            lowerContent.contains("unloading steam api") ||
            lowerContent.contains("pre-hook") ||
            lowerContent.contains("starting cs2 dedicated server") ||
            lowerContent.contains("loaded") ||
            lowerContent.contains("using breakpad") ||
            lowerContent.contains("setting breakpad") ||
            lowerContent.contains("forcing breakpad") ||
            lowerContent.contains("looking up breakpad") ||
            lowerContent.contains("calling breakpad") ||
            lowerContent.contains("console initialized") ||
            lowerContent.contains("steam appid") ||
            lowerContent.contains("initsteamlogin") ||
            lowerContent.contains("steam universe is invalid") ||
            lowerContent.contains("resetbreakpadappid") ||
            lowerContent.contains("visibility enabled") ||
            lowerContent.contains("usrlocal path not found") ||
            lowerContent.contains("trying to set dxlevel") ||
            lowerContent.contains("path id") ||
            lowerContent.contains("file path") ||
            lowerContent.contains("addons") ||
            lowerContent.contains("content") ||
            lowerContent.contains("default_write_path") ||
            lowerContent.contains("executable_path") ||
            lowerContent.contains("game") ||
            lowerContent.contains("gamebin") ||
            lowerContent.contains("gameroot") ||
            lowerContent.contains("mod") ||
            lowerContent.contains("---------------") ||
            lowerContent.contains("json_begin") ||
            lowerContent.contains("json_end") ||
            lowerContent.contains("player_") ||
            lowerContent.trim().isEmpty()) {
            return true;
        }
        
        return false;
    }
}
