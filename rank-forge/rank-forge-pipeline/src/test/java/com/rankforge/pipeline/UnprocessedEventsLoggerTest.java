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
import java.nio.file.StandardOpenOption;
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
        
        // Mock eventStore to return empty (no games processed yet)
        // Only stub when file exists to avoid unnecessary stubbing exception
        when(eventStore.getGameEvent(eq(GameEventType.GAME_OVER), any()))
            .thenReturn(Optional.empty());
        
        logger.info("Reading log file: {}", logFilePath.toAbsolutePath());
        List<String> lines = Files.readAllLines(logFilePath);
        logger.info("Read {} lines from log file", lines.size());
        
        // Track processed and unprocessed lines
        Set<String> processedEventTypes = new HashSet<>();
        // Map from line index to log content for lines that haven't been processed yet
        Map<Integer, String> unprocessedLines = new HashMap<>();
        
        int processedCount = 0;
        int errorCount = 0;
        
        // Process each line - respect the parser's nextIndex to handle rewind logic
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            
            try {
                // Extract the actual log content from JSON wrapper
                JsonNode jsonNode = objectMapper.readTree(line);
                if (!jsonNode.has("log")) {
                    i++; // Move to next line
                    continue; // Skip lines without "log" field
                }
                
                String logContent = jsonNode.get("log").asText().trim();
                
                // Skip empty lines and non-game log lines
                if (logContent.isEmpty() || !logContent.startsWith("L ")) {
                    i++; // Move to next line
                    continue;
                }
                
                // Try to parse the line
                Optional<ParseLineResponse> response = parser.parseLine(line, lines, i);
                
                if (response.isPresent()) {
                    ParseLineResponse parseResponse = response.get();
                    GameEvent event = parseResponse.getGameEvent();
                    processedEventTypes.add(event.getGameEventType().name());
                    processedCount++;
                    
                    // Remove from unprocessed if it was previously marked as unprocessed
                    unprocessedLines.remove(i);
                    
                    // Respect the parser's nextIndex (important for rewind logic)
                    int nextIndex = parseResponse.getNextIndex();
                    // If nextIndex is same as current (parser returned currentIndex), 
                    // we need to manually advance to avoid infinite loop
                    i = (nextIndex == i) ? i + 1 : nextIndex;
                } else {
                    // This line was not processed - mark it as unprocessed for now
                    // It might be processed later if parser rewinds
                    if (!shouldSkipLine(logContent)) {
                        unprocessedLines.put(i, logContent);
                    }
                    i++; // Move to next line
                }
            } catch (Exception e) {
                errorCount++;
                logger.debug("Error processing line {}: {}", i, e.getMessage());
                i++; // Move to next line even on error
            }
        }
        
        // Now group the truly unprocessed lines by pattern
        Map<String, EventGroup> unprocessedEventGroups = new HashMap<>();
        for (String logContent : unprocessedLines.values()) {
            String eventPattern = normalizeEventPattern(logContent);
            EventGroup group = unprocessedEventGroups.computeIfAbsent(eventPattern, k -> new EventGroup());
            group.increment();
            group.addExample(logContent);
        }
        
        int unprocessedCount = unprocessedLines.size();
        
        // Log summary
        logger.info("=".repeat(80));
        logger.info("PROCESSING SUMMARY");
        logger.info("=".repeat(80));
        logger.info("Total lines processed: {}", lines.size());
        logger.info("Successfully processed events: {}", processedCount);
        logger.info("Unprocessed lines: {}", unprocessedCount);
        logger.info("Unprocessed event groups (unique patterns): {}", unprocessedEventGroups.size());
        logger.info("Error lines: {}", errorCount);
        logger.info("");
        logger.info("Processed event types: {}", processedEventTypes);
        logger.info("");
        
        // Log grouped unprocessed events
        logger.info("=".repeat(80));
        logger.info("UNPROCESSED EVENT PATTERNS ({} unique patterns)", unprocessedEventGroups.size());
        logger.info("=".repeat(80));
        
        // Sort by frequency (most common first) for easier analysis
        List<Map.Entry<String, EventGroup>> sortedUnprocessed = unprocessedEventGroups.entrySet().stream()
                .sorted(Map.Entry.<String, EventGroup>comparingByValue(
                    Comparator.comparing((EventGroup g) -> g.count).reversed()))
                .collect(Collectors.toList());
        
        for (Map.Entry<String, EventGroup> entry : sortedUnprocessed) {
            EventGroup group = entry.getValue();
            logger.info("[Count: {}] {}", group.count, entry.getKey());
        }
        
        logger.info("=".repeat(80));
        logger.info("END OF UNPROCESSED EVENTS REPORT");
        logger.info("=".repeat(80));
        
        // Save specific event patterns to test resource files
        //saveEventExamplesToFiles(unprocessedEventGroups, logFilePath);
    }
    
    /**
     * Save actual log line examples for specific event patterns to test resource files.
     */
    private void saveEventExamplesToFiles(Map<String, EventGroup> unprocessedEventGroups, Path logFilePath) {
        try {
            // Define patterns we want to extract (matching the user's specified patterns)
            Map<String, String> patternToFile = new LinkedHashMap<>();
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] attacked \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (<PARAM>\"<VALUE>\") (<PARAM>\"<VALUE>\") (<PARAM>\"<VALUE>\") (<PARAM>\"<VALUE>\") (hitgroup \"<VALUE>\")",
                    "unprocessed_attack_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\"",
                    "unprocessed_kill_basic_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (headshot)",
                    "unprocessed_kill_headshot_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (throughsmoke)",
                    "unprocessed_kill_throughsmoke_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (headshot throughsmoke)",
                    "unprocessed_kill_headshot_throughsmoke_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (attackerinair)",
                    "unprocessed_kill_attackerinair_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (headshot penetrated)",
                    "unprocessed_kill_headshot_penetrated_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] killed \"<VALUE>\" [<COORDS>] with \"<VALUE>\" (headshot attackerinair)",
                    "unprocessed_kill_headshot_attackerinair_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" flash-assisted killing \"<VALUE>\"",
                    "unprocessed_flash_assist_events.txt");
            patternToFile.put("L <DATE> - <TIME>: \"<VALUE>\" [<COORDS>] attacked \"<VALUE>\" [<COORDS>] with \"\"<VALUE>\"<NUM>\"<VALUE>\"<NUM>\"<VALUE>\"<NUM>\"<VALUE>\"<NUM>\"<VALUE>\"GENERIC)",
                    "unprocessed_attack_malformed_events.txt");
            
            // Create test resources directory if it doesn't exist
            // Path is relative to the module root (rank-forge-pipeline/)
            Path testResourcesDir = Paths.get("src/test/resources/com/rankforge/pipeline");
            if (!Files.exists(testResourcesDir)) {
                Files.createDirectories(testResourcesDir);
            }
            
            for (Map.Entry<String, String> patternFile : patternToFile.entrySet()) {
                String pattern = patternFile.getKey();
                String filename = patternFile.getValue();
                EventGroup group = unprocessedEventGroups.get(pattern);
                
                if (group != null && !group.examples.isEmpty()) {
                    Path outputFile = testResourcesDir.resolve(filename);
                    List<String> jsonLines = new ArrayList<>();
                    jsonLines.add("# Extracted unprocessed event examples for testing");
                    jsonLines.add("# Pattern: " + pattern);
                    jsonLines.add("# Count: " + group.count);
                    jsonLines.add("");
                    
                    // Wrap each example in JSON format like production logs
                    for (String example : group.examples) {
                        // Escape JSON special characters
                        String escaped = example.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                        String jsonLine = String.format("{\"log\":\"%s\",\"stream\":\"stdout\",\"time\":\"2026-01-07T16:00:00.000Z\"}", escaped);
                        jsonLines.add(jsonLine);
                    }
                    
                    Files.write(outputFile, jsonLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    logger.info("Saved {} examples of pattern '{}' to {}", 
                            group.examples.size(), pattern, outputFile);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to save event examples to files", e);
        }
    }
    
    /**
     * Normalize a log line to extract the event pattern.
     * Replaces variable parts (player names, IDs, coordinates, timestamps, weapons, items) with placeholders.
     */
    private String normalizeEventPattern(String logContent) {
        // Remove timestamp: "L MM/DD/YYYY - HH:MM:SS: " -> "L <DATE> - <TIME>: "
        String pattern = logContent.replaceAll("L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}:", "L <DATE> - <TIME>:");
        
        // Generalize accolade events: "ACCOLADE, FINAL: {3k}, PlayerName<ID>, ..." -> "ACCOLADE, FINAL: {<TYPE>}, <PLAYER><ID>, ..."
        // Format: ACCOLADE, FINAL: {type}, PlayerName<ID>, VALUE: X, POS: Y, SCORE: Z
        if (pattern.contains("ACCOLADE, FINAL:")) {
            // Replace accolade type: {3k} -> {<TYPE>}
            pattern = pattern.replaceAll("ACCOLADE, FINAL: \\{[^}]+\\}", "ACCOLADE, FINAL: {<TYPE>}");
            // Replace player name and ID in accolade: "PlayerName<ID>" -> "<PLAYER><ID>"
            // Match: any text before <, then <, then digits, then >
            pattern = pattern.replaceAll("([^<,\\t]+)<(\\d+)>", "<PLAYER><ID>");
        }
        
        // Replace player names and IDs: "PlayerName<ID><[U:1:123456]><TEAM>" -> "<PLAYER><ID><<STEAM_ID>><TEAM>"
        pattern = pattern.replaceAll("\"[^\"]+<\\d+><(?:\\[U:\\d+:\\d+\\]|BOT)><(?:CT|TERRORIST)>\"", "\"<PLAYER><ID><<STEAM_ID>><TEAM>\"");
        
        // Replace coordinates: "[123 456 789]" -> "[<COORDS>]"
        pattern = pattern.replaceAll("\\[-?\\d+ -?\\d+ -?\\d+\\]", "[<COORDS>]");
        
        // Generalize "picked up" events: "picked up \"knife\"" -> "picked up \"<WEAPON>\""
        pattern = pattern.replaceAll("picked up \"[^\"]+\"", "picked up \"<WEAPON>\"");
        
        // Generalize "purchased" events: "purchased \"item_assaultsuit\"" -> "purchased \"<ITEM>\""
        pattern = pattern.replaceAll("purchased \"[^\"]+\"", "purchased \"<ITEM>\"");
        
        // Generalize "threw" events: "threw smokegrenade" -> "threw <PROJECTILE>"
        // Also handles: "threw molotov", "threw decoy", etc.
        pattern = pattern.replaceAll("threw \\w+", "threw <PROJECTILE>");
        
        // Generalize sv_throw_* events: "sv_throw_flashgrenade" -> "sv_throw_<PROJECTILE>"
        pattern = pattern.replaceAll("sv_throw_\\w+", "sv_throw_<PROJECTILE>");
        
        // Generalize projectile spawned events: "Molotov projectile spawned" -> "projectile spawned"
        // Also handles: "Flashbang projectile spawned", "Smokegrenade projectile spawned", etc.
        pattern = pattern.replaceAll("\\w+ projectile spawned", "projectile spawned");
        
        // Generalize money change events: "money change 123-456 = $789" -> "money change <NUM>-<NUM> = $<NUM>"
        pattern = pattern.replaceAll("money change \\d+-\\d+ = \\$\\d+", "money change <NUM>-<NUM> = \\$<NUM>");
        
        // Generalize purchase info in money change: "(purchase: weapon_hegrenade)" -> "(purchase: <ITEM>)"
        pattern = pattern.replaceAll("\\(purchase: [^\\)]+\\)", "(purchase: <ITEM>)");
        
        // Generalize "left buyzone" events: "left buyzone with [ weapon_knife weapon_usp_silencer ]" -> "left buyzone with [ <WEAPONS> ]"
        pattern = pattern.replaceAll("left buyzone with \\[ [^\\]]+ \\]", "left buyzone with [ <WEAPONS> ]");
        
        // Replace numbers in parentheses: "(damage \"123\")" -> "(damage \"<NUM>\")"
        pattern = pattern.replaceAll("\\([^\"]+\"\\d+\"\\)", "(<PARAM>\"<NUM>\")");
        
        // Replace other quoted strings that might be specific values: "some value" -> "<VALUE>"
        // But be careful not to replace already normalized patterns
        pattern = pattern.replaceAll("\"([^\"]+)\"", "\"<VALUE>\"");
        
        // Collapse sequences of numbers (with optional decimals and signs) into a single placeholder
        // This groups events like "sv_throw_<PROJECTILE> 1.2 3.4 -5.6 ..." into "sv_throw_<PROJECTILE> <NUMBERS>"
        // Match sequences of 2+ numbers (with optional decimals and signs) separated by spaces
        // Use a more greedy approach to capture the entire sequence
        pattern = pattern.replaceAll("(-?\\d+(?:\\.\\d+)?(?:\\s+-?\\d+(?:\\.\\d+)?)+)", "<NUMBERS>");
        
        // Replace standalone numbers that might be IDs or values (but not in already normalized patterns)
        pattern = pattern.replaceAll("\\b\\d+\\b", "<NUM>");
        
        // Clean up multiple spaces
        pattern = pattern.replaceAll("\\s+", " ");
        
        // Clean up multiple <NUMBERS> placeholders (shouldn't happen, but just in case)
        pattern = pattern.replaceAll("<NUMBERS>\\s*<NUMBERS>", "<NUMBERS>");
        
        return pattern.trim();
    }
    
    /**
     * Helper class to group similar events together
     */
    private static class EventGroup {
        int count = 0;
        List<String> examples = new ArrayList<>();
        
        void increment() {
            count++;
        }
        
        void addExample(String example) {
            examples.add(example); // Save all examples for test files
        }
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
            lowerContent.contains("server_cvar") ||
            lowerContent.contains("steamauth") ||
            lowerContent.contains("cvar") ||
            lowerContent.contains("loading map") ||
            lowerContent.contains("switched from team") ||
            lowerContent.contains("vote") ||
            lowerContent.contains("rcon from") ||
            lowerContent.contains("steam userid validated") ||
            lowerContent.contains("disconnected") ||
            lowerContent.contains("starting freeze period") ||
            lowerContent.contains("left buyzone") ||
            lowerContent.contains("connected, address") ||
            lowerContent.contains("say_team") ||
            lowerContent.contains("killed other") ||
            lowerContent.trim().isEmpty()) {
            return true;
        }
        
        return false;
    }
}
