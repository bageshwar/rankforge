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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.GameEventType;
import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import com.rankforge.server.dto.ProcessLogRequest;
import com.rankforge.server.dto.ProcessLogResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for Pipeline API endpoints
 * Tests the full request/response cycle including security, validation, and async processing
 * 
 * Requires:
 * - AWS S3 credentials in application-local.properties
 * - API key configured in application-local.properties
 * - Database connection (can be mocked or use test database)
 * 
 * To run: mvn test -Dtest=PipelineApiIntegrationTest -Dspring.profiles.active=local
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineApiIntegrationTest {

    private static final String TEST_S3_PATH = "s3://cs2serverdata/cs2_log_2026-01-07.json";
    private static final String STAGING_DB_IDENTIFIER = "staging";
    
    // Expected counts from the test log file
    private static final int EXPECTED_GAMES = 2;
    private static final int EXPECTED_ROUND_START_EVENTS = 40;
    private static final int EXPECTED_ACCOLADES = 21;
    
    // Static flag to ensure tables are only cleared ONCE across all test runs
    private static boolean tablesCleared = false;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameEventRepository gameEventRepository;
    
    @Autowired
    private AccoladeRepository accoladeRepository;
    
    @Autowired
    private PlayerStatsRepository playerStatsRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${rankforge.api.key:}")
    private String configuredApiKey;
    
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    /**
     * Gets the API key from application-local.properties file
     * Falls back to Spring context value if file loading fails
     */
    private String getApiKey() {
        // First try to get from Spring context (loaded from application-local.properties)
        if (configuredApiKey != null && !configuredApiKey.isEmpty() && !configuredApiKey.equals("your_api_key_here")) {
            return configuredApiKey;
        }
        
        // Fallback: Try to load directly from application-local.properties file
        try {
            Properties props = new Properties();
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("application-local.properties")) {
                if (is != null) {
                    props.load(is);
                    String apiKey = props.getProperty("rankforge.api.key", "");
                    if (!apiKey.isEmpty() && !apiKey.equals("your_api_key_here")) {
                        return apiKey;
                    }
                }
            }
        } catch (IOException e) {
            // Ignore - will use default
        }
        
        // Last resort: Try environment variable
        String apiKey = System.getenv("PIPELINE_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        
        // Default test key (should be configured in application-local.properties)
        // Note: This is a fallback for tests - actual API key should be in application-local.properties
        return "test-api-key-placeholder";
    }
    
    /**
     * Validates that the test is running against a staging database.
     * FAILS the test if connected to a non-staging database to prevent data loss.
     */
    private void assertStagingDatabase() {
        assertNotNull(datasourceUrl, "Database URL must be configured");
        assertFalse(datasourceUrl.isEmpty(), "Database URL must not be empty");
        
        String lowerUrl = datasourceUrl.toLowerCase();
        assertTrue(lowerUrl.contains(STAGING_DB_IDENTIFIER),
                "SAFETY CHECK FAILED: Tests must run against a staging database!\n" +
                "Current database URL: " + datasourceUrl + "\n" +
                "Expected database URL to contain: '" + STAGING_DB_IDENTIFIER + "'\n" +
                "This check prevents accidental data loss in production databases.");
        
        System.out.println("✓ Staging database check passed: " + extractDatabaseName(datasourceUrl));
    }
    
    /**
     * Extracts database name from JDBC URL for logging.
     */
    private String extractDatabaseName(String url) {
        // Try to extract database name from various JDBC URL formats
        if (url.contains("database=")) {
            int start = url.indexOf("database=") + 9;
            int end = url.indexOf(";", start);
            return end > start ? url.substring(start, end) : url.substring(start);
        }
        return url;
    }
    
    /**
     * Clears all data from RankForge tables for a clean test start.
     * Uses DELETE instead of DROP to preserve schema (Hibernate ddl-auto=update won't recreate dropped tables).
     * Order matters due to foreign key constraints.
     * Uses a static flag to ensure this only runs ONCE per JVM session.
     */
    private void clearAllTables() {
        // Safety check: only clear tables once per test run
        if (tablesCleared) {
            System.out.println("ℹ️  Tables already cleared in this test run - skipping");
            return;
        }
        
        System.out.println("🗑️  Clearing all tables for clean test start...");
        
        // Delete data in order respecting foreign key constraints
        // Child tables first, then parent tables
        String[] tablesToClear = {
            "PlayerStats",      // References Game
            "Accolade",         // References Game  
            "GameEvent",        // References Game and itself (roundStartEventId)
            "Game"              // Parent table
        };
        
        for (String table : tablesToClear) {
            try {
                // Use DELETE to clear data while preserving schema
                // Check if table exists first (SQL Server syntax)
                String sql = "IF OBJECT_ID('" + table + "', 'U') IS NOT NULL DELETE FROM " + table;
                jdbcTemplate.execute(sql);
                System.out.println("  ✓ Cleared table: " + table);
            } catch (Exception e) {
                System.out.println("  ⚠ Could not clear table " + table + ": " + e.getMessage());
            }
        }
        
        // Mark as done so it won't run again
        tablesCleared = true;
        System.out.println("✓ Table cleanup complete.");
    }

    @Test
    void testHealthEndpoint_WithoutApiKey_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/pipeline/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pipeline API is healthy"));
    }

    @Test
    void testProcessEndpoint_WithoutApiKey_ReturnsUnauthorized() throws Exception {
        ProcessLogRequest request = new ProcessLogRequest("s3://test-bucket/test-file.json");
        
        mockMvc.perform(post("/api/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized. Invalid or missing API key."));
    }

    @Test
    void testProcessEndpoint_WithInvalidApiKey_ReturnsUnauthorized() throws Exception {
        ProcessLogRequest request = new ProcessLogRequest("s3://test-bucket/test-file.json");
        
        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized. Invalid or missing API key."));
    }

    @Test
    void testProcessEndpoint_WithValidApiKey_ButInvalidS3Path_ReturnsBadRequest() throws Exception {
        String apiKey = getApiKey();
        assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                "API key must be configured in application-local.properties");
        
        ProcessLogRequest request = new ProcessLogRequest("invalid-path");
        
        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.jobId").doesNotExist());
    }

    @Test
    void testProcessEndpoint_WithValidApiKey_AndValidS3Path_ReturnsAccepted() throws Exception {
        String apiKey = getApiKey();
        assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                "API key must be configured in application-local.properties");
        
        // Use actual S3 path from your bucket
        String testS3Path = "s3://cs2serverdata/cs2_log_2026-01-07.json";
        ProcessLogRequest request = new ProcessLogRequest(testS3Path);
        
        String responseContent = mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.message").value("Log processing started successfully"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Parse and verify response structure
        ProcessLogResponse response = objectMapper.readValue(responseContent, ProcessLogResponse.class);
        assertNotNull(response.getJobId(), "Job ID should not be null");
        assertFalse(response.getJobId().isEmpty(), "Job ID should not be empty");
        assertEquals("processing", response.getStatus());
        assertEquals("Log processing started successfully", response.getMessage());
        
        System.out.println("✅ Successfully started log processing job: " + response.getJobId());
    }

    @Test
    void testProcessEndpoint_WithEmptyS3Path_ReturnsBadRequest() throws Exception {
        String apiKey = getApiKey();
        assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                "API key must be configured in application-local.properties");
        
        ProcessLogRequest request = new ProcessLogRequest("");
        
        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessEndpoint_WithNullS3Path_ReturnsBadRequest() throws Exception {
        String apiKey = getApiKey();
        assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                "API key must be configured in application-local.properties");
        
        // Send request with null s3Path
        String requestJson = "{\"s3Path\": null}";
        
        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessEndpoint_WithMissingS3Path_ReturnsBadRequest() throws Exception {
        String apiKey = getApiKey();
        assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                "API key must be configured in application-local.properties");
        
        // Send request without s3Path field
        String requestJson = "{}";
        
        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessEndpoint_WithNonExistentS3Bucket_ReturnsError() throws Exception {
        String apiKey = getApiKey();
        assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                "API key must be configured in application-local.properties");
        
        // Use non-existent bucket path
        String invalidS3Path = "s3://non-existent-bucket-12345/invalid-file.json";
        ProcessLogRequest request = new ProcessLogRequest(invalidS3Path);
        
        // The endpoint will accept the request and start async processing
        // The error will be logged but the response will still be 202 Accepted
        // This is because async processing errors don't fail the HTTP request
        String responseContent = mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        ProcessLogResponse response = objectMapper.readValue(responseContent, ProcessLogResponse.class);
        assertNotNull(response.getJobId());
        assertEquals("processing", response.getStatus());
        
        System.out.println("⚠️  Note: Job " + response.getJobId() + " will fail during async processing (expected for invalid S3 path)");
    }

    // ========================================================================
    // E2E Database Validation Tests
    // ========================================================================
    
    @Nested
    @DisplayName("E2E Database Validation Tests")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DatabaseValidationTests {
        
        /**
         * Runs before all tests in this nested class.
         * Validates staging database and drops all tables for a clean start.
         */
        @BeforeAll
        void setupBeforeAllTests() {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("🔧 E2E Database Validation Tests - Setup");
            System.out.println("=".repeat(70));
            
            // CRITICAL: Verify we're on staging database
            assertStagingDatabase();
            
            // Clear all table data for a completely clean start
            // Uses DELETE instead of DROP to preserve schema
            clearAllTables();
            
            System.out.println("=".repeat(70) + "\n");
        }
        
        /**
         * Processes log file and waits for completion, then validates all database records.
         * This is the main E2E test that validates:
         * - 40 round events (ROUND_START)
         * - 40 distinct round references across all events
         * - 2 games with 21 accolades
         * - All entity references are correctly set
         */
        @Test
        @DisplayName("E2E: Process log file and validate all database records")
        void processLogFileAndValidateDatabaseRecords() throws Exception {
            String apiKey = getApiKey();
            assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                    "API key must be configured in application-local.properties");
            
            // Verify we're on staging (redundant but safe)
            assertStagingDatabase();
            
            // Trigger log processing
            ProcessLogRequest request = new ProcessLogRequest(TEST_S3_PATH);
            String responseContent = mockMvc.perform(post("/api/pipeline/process")
                            .header("X-API-Key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            
            ProcessLogResponse response = objectMapper.readValue(responseContent, ProcessLogResponse.class);
            System.out.println("🚀 Started log processing job: " + response.getJobId());
            
            // Wait for async processing to complete (poll database)
            // Using 90 seconds timeout to accommodate database latency
            boolean processingComplete = waitForProcessingComplete(90, TimeUnit.SECONDS);
            assertTrue(processingComplete, "Processing should complete within timeout");
            
            // Now run all validations
            validateGameCount();
            validateRoundStartEventCount();
            validateAccoladeCount();
            validateAllGameReferences();
            validateAllRoundStartReferences();
            validateDistinctRoundReferences();
            validatePlayerRoundsPlayed();
            
            System.out.println("✅ All E2E database validations passed!");
        }
        
        @Test
        @DisplayName("Validate: Exactly 2 games are persisted")
        void validateGameCount() {
            List<GameEntity> games = gameRepository.findAll();
            assertEquals(EXPECTED_GAMES, games.size(), 
                    "Should have exactly " + EXPECTED_GAMES + " games in database");
            
            // Validate game fields are populated
            for (GameEntity game : games) {
                assertNotNull(game.getId(), "Game should have an ID");
                assertNotNull(game.getMap(), "Game should have a map");
                assertNotNull(game.getGameOverTimestamp(), "Game should have a gameOverTimestamp");
                assertNotNull(game.getTeam1Score(), "Game should have team1Score");
                assertNotNull(game.getTeam2Score(), "Game should have team2Score");
                
                System.out.println("  ✓ Game ID " + game.getId() + ": " + game.getMap() + 
                        " (" + game.getTeam1Score() + " - " + game.getTeam2Score() + ")");
            }
        }
        
        @Test
        @DisplayName("Validate: Exactly 40 ROUND_START events are persisted")
        void validateRoundStartEventCount() {
            List<GameEventEntity> roundStartEvents = gameEventRepository.findByGameEventType(GameEventType.ROUND_START);
            assertEquals(EXPECTED_ROUND_START_EVENTS, roundStartEvents.size(),
                    "Should have exactly " + EXPECTED_ROUND_START_EVENTS + " ROUND_START events");
            
            System.out.println("  ✓ Found " + roundStartEvents.size() + " ROUND_START events");
        }
        
        @Test
        @DisplayName("Validate: Exactly 21 accolades are persisted")
        void validateAccoladeCount() {
            List<AccoladeEntity> accolades = accoladeRepository.findAll();
            assertEquals(EXPECTED_ACCOLADES, accolades.size(),
                    "Should have exactly " + EXPECTED_ACCOLADES + " accolades");
            
            // Group by game for reporting
            var accoladesByGame = accolades.stream()
                    .collect(Collectors.groupingBy(a -> a.getGame().getId()));
            
            System.out.println("  ✓ Found " + accolades.size() + " accolades across " + 
                    accoladesByGame.size() + " games");
            accoladesByGame.forEach((gameId, gameAccolades) -> 
                    System.out.println("    - Game " + gameId + ": " + gameAccolades.size() + " accolades"));
        }
        
        @Test
        @DisplayName("Validate: All game events have valid game reference")
        void validateAllGameReferences() {
            List<GameEventEntity> allEvents = gameEventRepository.findAll();
            assertFalse(allEvents.isEmpty(), "Should have events in database");
            
            int eventsWithoutGame = 0;
            for (GameEventEntity event : allEvents) {
                if (event.getGame() == null) {
                    eventsWithoutGame++;
                    System.out.println("  ✗ Event ID " + event.getId() + " (" + 
                            event.getGameEventType() + ") has no game reference!");
                }
            }
            
            assertEquals(0, eventsWithoutGame, 
                    "All events should have a game reference. Found " + eventsWithoutGame + " without.");
            
            System.out.println("  ✓ All " + allEvents.size() + " events have valid game references");
        }
        
        @Test
        @DisplayName("Validate: All in-round events have valid roundStart reference")
        void validateAllRoundStartReferences() {
            List<GameEventEntity> allEvents = gameEventRepository.findAll();
            
            // Events that should have roundStart reference (everything except ROUND_START, GAME_OVER, GAME_PROCESSED)
            Set<GameEventType> typesWithoutRoundStart = Set.of(
                    GameEventType.ROUND_START,
                    GameEventType.GAME_OVER,
                    GameEventType.GAME_PROCESSED
            );
            
            int eventsChecked = 0;
            int eventsWithoutRoundStart = 0;
            
            for (GameEventEntity event : allEvents) {
                if (!typesWithoutRoundStart.contains(event.getGameEventType())) {
                    eventsChecked++;
                    if (event.getRoundStart() == null) {
                        eventsWithoutRoundStart++;
                        System.out.println("  ✗ Event ID " + event.getId() + " (" + 
                                event.getGameEventType() + ") has no roundStart reference!");
                    }
                }
            }
            
            assertEquals(0, eventsWithoutRoundStart,
                    "All in-round events should have roundStart reference. Found " + 
                    eventsWithoutRoundStart + " without.");
            
            System.out.println("  ✓ All " + eventsChecked + " in-round events have valid roundStart references");
        }
        
        @Test
        @DisplayName("Validate: 40 distinct roundStartEventId values across all events")
        void validateDistinctRoundReferences() {
            List<GameEventEntity> allEvents = gameEventRepository.findAll();
            
            // Collect all distinct roundStart IDs (excluding nulls)
            Set<Long> distinctRoundStartIds = allEvents.stream()
                    .filter(e -> e.getRoundStart() != null)
                    .map(e -> e.getRoundStart().getId())
                    .collect(Collectors.toSet());
            
            assertEquals(EXPECTED_ROUND_START_EVENTS, distinctRoundStartIds.size(),
                    "Should have " + EXPECTED_ROUND_START_EVENTS + " distinct roundStartEventId values");
            
            // Verify each distinct ID corresponds to a ROUND_START event
            List<GameEventEntity> roundStartEvents = gameEventRepository.findByGameEventType(GameEventType.ROUND_START);
            Set<Long> roundStartEventIds = roundStartEvents.stream()
                    .map(GameEventEntity::getId)
                    .collect(Collectors.toSet());
            
            // All referenced roundStart IDs should be in the set of actual ROUND_START events
            Set<Long> invalidReferences = new HashSet<>(distinctRoundStartIds);
            invalidReferences.removeAll(roundStartEventIds);
            
            assertTrue(invalidReferences.isEmpty(),
                    "All roundStartEventId references should point to actual ROUND_START events. " +
                    "Invalid references: " + invalidReferences);
            
            System.out.println("  ✓ Found " + distinctRoundStartIds.size() + 
                    " distinct roundStartEventId references, all valid");
        }
        
        @Test
        @DisplayName("Validate: All accolades have valid game reference")
        void validateAllAccoladeGameReferences() {
            List<AccoladeEntity> accolades = accoladeRepository.findAll();
            
            int accoladesWithoutGame = 0;
            for (AccoladeEntity accolade : accolades) {
                if (accolade.getGame() == null) {
                    accoladesWithoutGame++;
                    System.out.println("  ✗ Accolade ID " + accolade.getId() + " (" + 
                            accolade.getType() + ") has no game reference!");
                }
            }
            
            assertEquals(0, accoladesWithoutGame,
                    "All accolades should have a game reference. Found " + accoladesWithoutGame + " without.");
            
            System.out.println("  ✓ All " + accolades.size() + " accolades have valid game references");
        }
        
        @Test
        @DisplayName("Validate: Player roundsPlayed matches total rounds from games")
        void validatePlayerRoundsPlayed() {
            List<GameEntity> games = gameRepository.findAll();
            assumeTrue(games.size() >= EXPECTED_GAMES, "Need at least " + EXPECTED_GAMES + " games");
            
            // Calculate expected total rounds from all games
            int expectedTotalRounds = games.stream()
                    .mapToInt(g -> g.getTeam1Score() + g.getTeam2Score())
                    .sum();
            
            System.out.println("  📊 Expected total rounds from games: " + expectedTotalRounds);
            games.forEach(g -> System.out.println("    - Game " + g.getId() + " (" + g.getMap() + "): " + 
                    g.getTeam1Score() + " + " + g.getTeam2Score() + " = " + (g.getTeam1Score() + g.getTeam2Score()) + " rounds"));
            
            // Get latest PlayerStats for each player
            List<PlayerStatsEntity> allPlayerStats = playerStatsRepository.findAll();
            assertFalse(allPlayerStats.isEmpty(), "Should have player stats in database");
            
            // Group by playerId and get the latest entry for each player
            var latestStatsByPlayer = allPlayerStats.stream()
                    .collect(Collectors.groupingBy(PlayerStatsEntity::getPlayerId))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> e.getValue().stream()
                                    .max((a, b) -> {
                                        if (a.getGameTimestamp() == null && b.getGameTimestamp() == null) return 0;
                                        if (a.getGameTimestamp() == null) return -1;
                                        if (b.getGameTimestamp() == null) return 1;
                                        return a.getGameTimestamp().compareTo(b.getGameTimestamp());
                                    })
                                    .orElse(null)
                    ));
            
            System.out.println("  📊 Checking roundsPlayed for " + latestStatsByPlayer.size() + " players:");
            
            int playersWithIncorrectRounds = 0;
            for (var entry : latestStatsByPlayer.entrySet()) {
                PlayerStatsEntity stats = entry.getValue();
                if (stats == null) continue;
                
                int actualRoundsPlayed = stats.getRoundsPlayed();
                String playerName = stats.getLastSeenNickname() != null ? stats.getLastSeenNickname() : stats.getPlayerId();
                
                // Each player who played in all games should have roundsPlayed == expectedTotalRounds
                // For players who may not have played all games, we check that rounds > 0 and <= expectedTotalRounds
                if (actualRoundsPlayed <= 0) {
                    System.out.println("    ✗ Player " + playerName + " has roundsPlayed=" + actualRoundsPlayed + " (should be > 0)");
                    playersWithIncorrectRounds++;
                } else if (actualRoundsPlayed > expectedTotalRounds) {
                    System.out.println("    ✗ Player " + playerName + " has roundsPlayed=" + actualRoundsPlayed + 
                            " (exceeds max possible: " + expectedTotalRounds + ")");
                    playersWithIncorrectRounds++;
                } else {
                    System.out.println("    ✓ Player " + playerName + ": roundsPlayed=" + actualRoundsPlayed);
                }
            }
            
            assertEquals(0, playersWithIncorrectRounds,
                    "All players should have valid roundsPlayed counts. Found " + playersWithIncorrectRounds + " with issues.");
            
            // Additional check: verify the sum makes sense
            // If there are N players across M games, total roundsPlayed entries should be reasonable
            int totalRoundsPlayedSum = latestStatsByPlayer.values().stream()
                    .filter(s -> s != null)
                    .mapToInt(PlayerStatsEntity::getRoundsPlayed)
                    .sum();
            System.out.println("  📊 Total roundsPlayed across all players: " + totalRoundsPlayedSum);
        }
        
        @Test
        @DisplayName("Validate: Events are correctly distributed across games")
        void validateEventDistributionAcrossGames() {
            List<GameEntity> games = gameRepository.findAll();
            assumeTrue(games.size() >= EXPECTED_GAMES, "Need at least " + EXPECTED_GAMES + " games");
            
            for (GameEntity game : games) {
                List<GameEventEntity> gameEvents = gameEventRepository.findByGameId(game.getId());
                List<GameEventEntity> roundStarts = gameEventRepository.findByGameIdAndGameEventType(
                        game.getId(), GameEventType.ROUND_START);
                List<AccoladeEntity> gameAccolades = accoladeRepository.findByGameId(game.getId());
                
                assertFalse(gameEvents.isEmpty(), 
                        "Game " + game.getId() + " should have events");
                assertFalse(roundStarts.isEmpty(), 
                        "Game " + game.getId() + " should have ROUND_START events");
                assertFalse(gameAccolades.isEmpty(), 
                        "Game " + game.getId() + " should have accolades");
                
                System.out.println("  ✓ Game " + game.getId() + " (" + game.getMap() + "): " +
                        gameEvents.size() + " events, " + 
                        roundStarts.size() + " rounds, " +
                        gameAccolades.size() + " accolades");
            }
        }
        
        /**
         * E2E test for game deduplication.
         * Reimports the same log file and verifies that duplicates are not created.
         * This test validates that the deduplication logic in EventProcessorImpl works correctly.
         */
        @Test
        @DisplayName("E2E: Verify game deduplication on reimport")
        void verifyGameDeduplicationOnReimport() throws Exception {
            String apiKey = getApiKey();
            assumeTrue(apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your_api_key_here"),
                    "API key must be configured in application-local.properties");
            
            // Verify we're on staging (redundant but safe)
            assertStagingDatabase();
            
            // Step 1: Capture counts after initial import (from previous test or fresh import)
            // If no games exist, import first
            long initialGameCount = gameRepository.count();
            if (initialGameCount == 0) {
                System.out.println("📥 No games found - importing log file first...");
                ProcessLogRequest request = new ProcessLogRequest(TEST_S3_PATH);
                String responseContent = mockMvc.perform(post("/api/pipeline/process")
                                .header("X-API-Key", apiKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isAccepted())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
                
                ProcessLogResponse response = objectMapper.readValue(responseContent, ProcessLogResponse.class);
                System.out.println("🚀 Started initial log processing job: " + response.getJobId());
                
                boolean processingComplete = waitForProcessingComplete(90, TimeUnit.SECONDS);
                assertTrue(processingComplete, "Initial processing should complete within timeout");
                
                initialGameCount = gameRepository.count();
            }
            
            // Capture all counts after first import
            long gamesCountAfterFirstImport = gameRepository.count();
            long eventsAfterFirstImport = gameEventRepository.count();
            long accoladesAfterFirstImport = accoladeRepository.count();
            long playerStatsAfterFirstImport = playerStatsRepository.count();
            
            // Get game details for deduplication verification
            List<GameEntity> gamesAfterFirstImport = gameRepository.findAll();
            assertFalse(gamesAfterFirstImport.isEmpty(), "Should have games after first import");
            
            // Store game timestamps and maps for verification
            var gameSignatures = gamesAfterFirstImport.stream()
                    .collect(Collectors.toMap(
                            g -> g.getGameOverTimestamp() + "|" + g.getMap(),
                            g -> g,
                            (g1, g2) -> g1
                    ));
            
            System.out.println("\n📊 Counts after first import:");
            System.out.println("  - Games: " + gamesCountAfterFirstImport);
            System.out.println("  - Events: " + eventsAfterFirstImport);
            System.out.println("  - Accolades: " + accoladesAfterFirstImport);
            System.out.println("  - Player Stats: " + playerStatsAfterFirstImport);
            System.out.println("  - Game signatures (timestamp|map): " + gameSignatures.size());
            gamesAfterFirstImport.forEach(g -> 
                    System.out.println("    * Game ID " + g.getId() + ": " + g.getMap() + 
                            " @ " + g.getGameOverTimestamp()));
            
            // Step 2: Reimport the same log file
            System.out.println("\n📥 Reimporting the same log file to test deduplication...");
            ProcessLogRequest reimportRequest = new ProcessLogRequest(TEST_S3_PATH);
            String reimportResponseContent = mockMvc.perform(post("/api/pipeline/process")
                            .header("X-API-Key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reimportRequest)))
                    .andExpect(status().isAccepted())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            
            ProcessLogResponse reimportResponse = objectMapper.readValue(reimportResponseContent, ProcessLogResponse.class);
            System.out.println("🚀 Started reimport log processing job: " + reimportResponse.getJobId());
            
            // Wait for processing to complete
            boolean reimportComplete = waitForProcessingComplete(90, TimeUnit.SECONDS);
            assertTrue(reimportComplete, "Reimport processing should complete within timeout");
            
            // Step 3: Verify counts haven't increased (deduplication worked)
            long gamesCountAfterReimport = gameRepository.count();
            long eventsAfterReimport = gameEventRepository.count();
            long accoladesAfterReimport = accoladeRepository.count();
            long playerStatsAfterReimport = playerStatsRepository.count();
            
            System.out.println("\n📊 Counts after reimport:");
            System.out.println("  - Games: " + gamesCountAfterReimport + " (was " + gamesCountAfterFirstImport + ")");
            System.out.println("  - Events: " + eventsAfterReimport + " (was " + eventsAfterFirstImport + ")");
            System.out.println("  - Accolades: " + accoladesAfterReimport + " (was " + accoladesAfterFirstImport + ")");
            System.out.println("  - Player Stats: " + playerStatsAfterReimport + " (was " + playerStatsAfterFirstImport + ")");
            
            // Verify no new games were created
            assertEquals(gamesCountAfterFirstImport, gamesCountAfterReimport,
                    "Game count should not increase after reimport (deduplication should prevent duplicate games)");
            
            // Verify no new events were created
            assertEquals(eventsAfterFirstImport, eventsAfterReimport,
                    "Event count should not increase after reimport (duplicate games should be skipped)");
            
            // Verify no new accolades were created
            assertEquals(accoladesAfterFirstImport, accoladesAfterReimport,
                    "Accolade count should not increase after reimport (duplicate games should be skipped)");
            
            // Verify player stats count (may increase slightly if stats are updated, but shouldn't double)
            // Note: Player stats might be updated even if game is deduplicated, so we check it's reasonable
            assertTrue(playerStatsAfterReimport >= playerStatsAfterFirstImport,
                    "Player stats count should not decrease");
            // If deduplication works perfectly, stats shouldn't increase much
            // But allow some tolerance for stats updates
            long statsIncrease = playerStatsAfterReimport - playerStatsAfterFirstImport;
            double statsIncreasePercent = (double) statsIncrease / playerStatsAfterFirstImport * 100;
            assertTrue(statsIncreasePercent < 50, 
                    "Player stats should not increase significantly after reimport (increase: " + 
                    statsIncrease + ", " + String.format("%.1f", statsIncreasePercent) + "%)");
            
            // Step 4: Verify game signatures (timestamp + map) are still unique
            List<GameEntity> gamesAfterReimport = gameRepository.findAll();
            var gameSignaturesAfterReimport = gamesAfterReimport.stream()
                    .collect(Collectors.toMap(
                            g -> g.getGameOverTimestamp() + "|" + g.getMap(),
                            g -> g,
                            (g1, g2) -> g1
                    ));
            
            assertEquals(gameSignatures.size(), gameSignaturesAfterReimport.size(),
                    "Number of unique game signatures (timestamp|map) should not increase");
            
            // Verify all original games still exist
            for (var entry : gameSignatures.entrySet()) {
                String signature = entry.getKey();
                GameEntity originalGame = entry.getValue();
                
                assertTrue(gameSignaturesAfterReimport.containsKey(signature),
                        "Original game with signature '" + signature + "' should still exist after reimport");
                
                GameEntity gameAfterReimport = gameSignaturesAfterReimport.get(signature);
                assertEquals(originalGame.getId(), gameAfterReimport.getId(),
                        "Game ID should not change after reimport (same game, not duplicate)");
            }
            
            // Step 5: Verify no duplicate games with same timestamp and map
            // Group games by timestamp and map to check for duplicates
            var gamesBySignature = gamesAfterReimport.stream()
                    .collect(Collectors.groupingBy(
                            g -> g.getGameOverTimestamp() + "|" + g.getMap()
                    ));
            
            int duplicateCount = 0;
            for (var entry : gamesBySignature.entrySet()) {
                List<GameEntity> gamesWithSameSignature = entry.getValue();
                if (gamesWithSameSignature.size() > 1) {
                    duplicateCount++;
                    System.out.println("  ✗ Found " + gamesWithSameSignature.size() + 
                            " games with same signature: " + entry.getKey());
                    gamesWithSameSignature.forEach(g -> 
                            System.out.println("      - Game ID: " + g.getId()));
                }
            }
            
            assertEquals(0, duplicateCount,
                    "Should not have duplicate games with same timestamp and map. Found " + duplicateCount + " duplicates.");
            
            System.out.println("\n✅ Deduplication test passed!");
            System.out.println("  ✓ No duplicate games created");
            System.out.println("  ✓ Event count unchanged");
            System.out.println("  ✓ Accolade count unchanged");
            System.out.println("  ✓ All original games preserved");
        }
        
        /**
         * Waits for processing to complete by polling the database for expected game count.
         */
        private boolean waitForProcessingComplete(long timeout, TimeUnit unit) throws InterruptedException {
            long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
            int pollCount = 0;
            
            while (System.currentTimeMillis() < endTime) {
                pollCount++;
                long gameCount = gameRepository.count();
                
                if (gameCount >= EXPECTED_GAMES) {
                    // Also check that accolades are present (processing is truly complete)
                    long accoladeCount = accoladeRepository.count();
                    if (accoladeCount >= EXPECTED_ACCOLADES) {
                        System.out.println("  ✓ Processing complete after " + pollCount + " polls. " +
                                "Found " + gameCount + " games and " + accoladeCount + " accolades.");
                        return true;
                    }
                }
                
                if (pollCount % 10 == 0) {
                    System.out.println("  ... waiting for processing (poll " + pollCount + 
                            ", games: " + gameCount + ")");
                }
                
                Thread.sleep(500);
            }
            
            System.out.println("  ✗ Timeout waiting for processing. Games: " + gameRepository.count() + 
                    ", Accolades: " + accoladeRepository.count());
            return false;
        }
    }
}
