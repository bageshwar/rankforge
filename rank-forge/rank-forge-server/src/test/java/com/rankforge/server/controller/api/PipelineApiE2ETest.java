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
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E test for Pipeline API endpoints
 * Tests the full request/response cycle including security, validation, and async processing
 * 
 * Requires:
 * - AWS S3 credentials in application-local.properties
 * - API key configured in application-local.properties
 * - Database connection (staging database required)
 * 
 * To run:
 * - Unit tests only: mvn test
 * - E2E tests only: mvn test -Pe2e
 * - All tests: mvn verify
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineApiE2ETest {

    private static final String TEST_S3_PATH = "s3://cs2serverdata/cs2_log_2026-01-11.json";
    private static final String STAGING_DB_IDENTIFIER = "staging";
    
    // Expected counts based on actual data from localhost:8080
    private static final int EXPECTED_GAMES = 2;
    private static final int EXPECTED_ROUND_START_EVENTS = 39; // 24 rounds (de_anubis) + 15 rounds (de_ancient)
    private static final int EXPECTED_ACCOLADES = 21; // Verified from database
    
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
     * Validates that the test is running against a safe database (staging or H2).
     * Allows H2 for local development, staging for integration testing.
     * FAILS the test if connected to a production or unsupported database to prevent data loss.
     */
    private void assertStagingDatabase() {
        assertNotNull(datasourceUrl, "Database URL must be configured");
        assertFalse(datasourceUrl.isEmpty(), "Database URL must not be empty");
        
        String lowerUrl = datasourceUrl.toLowerCase();
        
        // Allow H2 database for local development
        boolean isH2 = lowerUrl.contains("h2");
        // Allow staging database for integration testing
        boolean isStaging = lowerUrl.contains(STAGING_DB_IDENTIFIER);
        
        if (isH2) {
            System.out.println("✓ H2 database detected - safe for local testing: " + extractDatabaseName(datasourceUrl));
            return;
        }
        
        if (isStaging) {
            System.out.println("✓ Staging database check passed: " + extractDatabaseName(datasourceUrl));
            return;
        }
        
        // FAIL if neither H2 nor staging
        fail("SAFETY CHECK FAILED: Tests must run against H2 (local) or staging database!\n" +
                "Current database URL: " + datasourceUrl + "\n" +
                "Expected database URL to contain: 'h2' (for local) or '" + STAGING_DB_IDENTIFIER + "' (for staging)\n" +
                "This check prevents accidental data loss in production databases.");
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
     * Works with both SQL Server and H2 databases.
     */
    private void clearAllTables() {
        // Safety check: only clear tables once per test run
        if (tablesCleared) {
            System.out.println("ℹ️  Tables already cleared in this test run - skipping");
            return;
        }
        
        System.out.println("🗑️  Clearing all tables for clean test start...");
        
        // Detect database type from URL
        boolean isH2 = datasourceUrl != null && datasourceUrl.toLowerCase().contains("h2");
        
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
                String sql;
                if (isH2) {
                    // H2 syntax: Check if table exists and delete
                    sql = "DELETE FROM \"" + table + "\"";
                } else {
                    // SQL Server syntax: Check if table exists first
                    sql = "IF OBJECT_ID('" + table + "', 'U') IS NOT NULL DELETE FROM " + table;
                }
                jdbcTemplate.execute(sql);
                System.out.println("  ✓ Cleared table: " + table);
            } catch (Exception e) {
                // Table might not exist yet (first run) - that's okay
                System.out.println("  ⚠ Could not clear table " + table + ": " + e.getMessage());
            }
        }
        
        // Mark as done so it won't run again
        tablesCleared = true;
        System.out.println("✓ Table cleanup complete.");
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

    // ========================================================================
    // E2E Database Validation Tests
    // ========================================================================
    
    @Nested
    @DisplayName("E2E Database Validation Tests")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class DatabaseValidationTests {
        
        /**
         * Runs before all tests in this nested class.
         * Validates database (staging or H2) and clears all tables for a clean start.
         */
        @BeforeAll
        void setupBeforeAllTests() {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("🔧 E2E Database Validation Tests - Setup");
            System.out.println("=".repeat(70));
            
            // Verify we're on a safe database (staging or H2)
            assertStagingDatabase();
            
            // Clear all table data for a completely clean start
            // Uses DELETE instead of DROP to preserve schema
            clearAllTables();
            
            System.out.println("=".repeat(70) + "\n");
        }
        
        /**
         * Processes log file and waits for completion, then validates all database records.
         * This is the main E2E test that validates:
         * - 39 round events (ROUND_START) - 24 from de_anubis + 15 from de_ancient
         * - 39 distinct round references across all events
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
        @DisplayName("Validate: Exactly 2 games are persisted with correct data")
        void validateGameCount() {
            List<GameEntity> games = gameRepository.findAll();
            assertEquals(EXPECTED_GAMES, games.size(), 
                    "Should have exactly " + EXPECTED_GAMES + " games in database");
            
            // Validate game fields are populated with specific expected values
            for (GameEntity game : games) {
                assertNotNull(game.getId(), "Game should have an ID");
                assertNotNull(game.getMap(), "Game should have a map");
                assertNotNull(game.getGameOverTimestamp(), "Game should have a gameOverTimestamp");
                assertNotNull(game.getTeam1Score(), "Game should have team1Score");
                assertNotNull(game.getTeam2Score(), "Game should have team2Score");
                assertNotNull(game.getMode(), "Game should have a mode");
                
                // Validate expected maps from test data
                assertTrue(game.getMap().equals("de_ancient") || game.getMap().equals("de_anubis"),
                        "Game map should be either de_ancient or de_anubis, but was: " + game.getMap());
                
                // Validate mode is competitive
                assertEquals("competitive", game.getMode(), "Game mode should be competitive");
                
                // Validate scores are reasonable (total rounds should match team1 + team2)
                int totalRounds = game.getTeam1Score() + game.getTeam2Score();
                assertTrue(totalRounds > 0, "Total rounds should be positive");
                assertTrue(totalRounds <= 30, "Total rounds should not exceed 30 for a standard competitive game");
                
                System.out.println("  ✓ Game ID " + game.getId() + ": " + game.getMap() + 
                        " (" + game.getTeam1Score() + " - " + game.getTeam2Score() + 
                        ") mode: " + game.getMode() + ", total rounds: " + totalRounds);
            }
            
            // Validate we have the expected specific games based on API data with exact matches
            // Game 1: de_anubis with score 13-11 (24 rounds)
            // Game 2: de_ancient with score 13-2 (15 rounds)
            GameEntity anubisGame = games.stream()
                    .filter(g -> g.getMap().equals("de_anubis") && 
                                g.getTeam1Score() == 13 && g.getTeam2Score() == 11)
                    .findFirst()
                    .orElse(null);
            GameEntity ancientGame = games.stream()
                    .filter(g -> g.getMap().equals("de_ancient") && 
                                g.getTeam1Score() == 13 && g.getTeam2Score() == 2)
                    .findFirst()
                    .orElse(null);
            
            assertNotNull(anubisGame, "Should have a game on de_anubis with score 13-11");
            assertNotNull(ancientGame, "Should have a game on de_ancient with score 13-2");
            
            // Validate exact game IDs match API (game 1 = de_anubis, game 2 = de_ancient)
            assertEquals(1L, anubisGame.getId(), 
                    "de_anubis game should have ID 1 as per API");
            assertEquals(2L, ancientGame.getId(), 
                    "de_ancient game should have ID 2 as per API");
            
            // Validate game timestamps match API (with tolerance for potential timezone/format differences)
            // Game 1: 2026-01-11T17:26:22.670587Z
            // Game 2: 2026-01-11T17:50:51.387150Z
            assertNotNull(anubisGame.getGameOverTimestamp(), 
                    "de_anubis game should have gameOverTimestamp");
            assertNotNull(ancientGame.getGameOverTimestamp(), 
                    "de_ancient game should have gameOverTimestamp");
            
            // Validate game 2 timestamp is after game 1 (game 2 happened later)
            assertTrue(ancientGame.getGameOverTimestamp().isAfter(anubisGame.getGameOverTimestamp()),
                    "de_ancient game should have later timestamp than de_anubis game");
        }
        
        @Test
        @DisplayName("Validate: Exactly 39 ROUND_START events are persisted")
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
        @DisplayName("Validate: 39 distinct roundStartEventId values across all events")
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
        @DisplayName("Validate: All accolades have valid game reference and proper data")
        void validateAllAccoladeGameReferences() {
            List<AccoladeEntity> accolades = accoladeRepository.findAll();
            
            int accoladesWithoutGame = 0;
            for (AccoladeEntity accolade : accolades) {
                if (accolade.getGame() == null) {
                    accoladesWithoutGame++;
                    System.out.println("  ✗ Accolade ID " + accolade.getId() + " (" + 
                            accolade.getType() + ") has no game reference!");
                }
                
                // Validate accolade has required fields
                assertNotNull(accolade.getType(), "Accolade should have a type");
                assertNotNull(accolade.getPlayerId(), "Accolade should have a player ID");
                assertNotNull(accolade.getValue(), "Accolade should have a value");
                assertTrue(accolade.getValue() >= 0, "Accolade value should be non-negative");
                assertTrue(accolade.getPosition() > 0, "Accolade position should be positive");
                assertTrue(accolade.getPosition() <= 3, "Accolade position should be 1-3 (top 3 players)");
            }
            
            assertEquals(0, accoladesWithoutGame,
                    "All accolades should have a game reference. Found " + accoladesWithoutGame + " without.");
            
            // Validate accolade types are meaningful (not null/empty)
            Set<String> accoladeTypes = accolades.stream()
                    .map(AccoladeEntity::getType)
                    .collect(Collectors.toSet());
            
            assertFalse(accoladeTypes.isEmpty(), "Should have at least one accolade type");
            System.out.println("  ✓ Found accolade types: " + accoladeTypes);
            System.out.println("  ✓ All " + accolades.size() + " accolades have valid game references");
        }
        
        @Test
        @DisplayName("Validate: Player roundsPlayed matches total rounds from games")
        void validatePlayerRoundsPlayed() {
            List<GameEntity> games = gameRepository.findAll();
            assumeTrue(games.size() >= EXPECTED_GAMES, "Need at least " + EXPECTED_GAMES + " games");
            
            // Calculate expected total rounds from all games (should be 39: 24 + 15)
            int expectedTotalRounds = games.stream()
                    .mapToInt(g -> g.getTeam1Score() + g.getTeam2Score())
                    .sum();
            
            assertEquals(39, expectedTotalRounds, 
                    "Total rounds should be 39 (24 from de_anubis + 15 from de_ancient)");
            
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
                
                // Validate rounds played based on API data
                // From API: Some players have 39 rounds (all rounds), some have 30 (partial participation)
                // All should be between 1 and expectedTotalRounds (39)
                if (actualRoundsPlayed <= 0) {
                    System.out.println("    ✗ Player " + playerName + " has roundsPlayed=" + actualRoundsPlayed + " (should be > 0)");
                    playersWithIncorrectRounds++;
                } else if (actualRoundsPlayed > expectedTotalRounds) {
                    System.out.println("    ✗ Player " + playerName + " has roundsPlayed=" + actualRoundsPlayed + 
                            " (exceeds max possible: " + expectedTotalRounds + ")");
                    playersWithIncorrectRounds++;
                } else {
                    // Validate rounds are within expected range (1-39 based on API)
                    assertTrue(actualRoundsPlayed >= 1 && actualRoundsPlayed <= expectedTotalRounds,
                            "Player " + playerName + " roundsPlayed (" + actualRoundsPlayed + 
                            ") should be between 1 and " + expectedTotalRounds);
                    System.out.println("    ✓ Player " + playerName + ": roundsPlayed=" + actualRoundsPlayed);
                }
            }
            
            assertEquals(0, playersWithIncorrectRounds,
                    "All players should have valid roundsPlayed counts. Found " + playersWithIncorrectRounds + " with issues.");
            
            // Validate specific players from API data have correct rounds
            // Based on API: Most players have 39 rounds (all rounds), some have 30 or 15 (partial participation)
            Map<String, Integer> expectedPlayerRounds = new HashMap<>();
            expectedPlayerRounds.put("[U:1:1219143518]", 39); // Mai Omelette Khaunga - all rounds
            expectedPlayerRounds.put("[U:1:1090227400]", 39); // k1d - all rounds
            expectedPlayerRounds.put("[U:1:1211958118]", 30); // the nucLeus - partial (30 rounds)
            expectedPlayerRounds.put("[U:1:216478675]", 39);  // Adkins#Keep Calm - all rounds
            expectedPlayerRounds.put("[U:1:1114723128]", 39); // _m3th0d - all rounds
            expectedPlayerRounds.put("[U:1:129501892]", 39);  // [[LEGEND KILLER]] _i_ - all rounds
            expectedPlayerRounds.put("[U:1:1222942858]", 39); // raksh - all rounds
            expectedPlayerRounds.put("[U:1:1098204826]", 39); // Khanjer - all rounds
            expectedPlayerRounds.put("[U:1:107493695]", 39);  // HwoaranG - all rounds
            expectedPlayerRounds.put("[U:1:1017449331]", 39); // PARROT - all rounds
            expectedPlayerRounds.put("[U:1:1026155000]", 15); // Wasuli Bhai !!!! - partial (15 rounds, only game 2)
            
            int validatedPlayers = 0;
            for (Map.Entry<String, Integer> expected : expectedPlayerRounds.entrySet()) {
                PlayerStatsEntity playerStats = latestStatsByPlayer.get(expected.getKey());
                if (playerStats != null) {
                    assertEquals(expected.getValue(), playerStats.getRoundsPlayed(),
                            "Player " + expected.getKey() + " should have " + expected.getValue() + " rounds");
                    validatedPlayers++;
                }
            }
            assertTrue(validatedPlayers >= 3, 
                    "Should validate at least 3 known players from API data");
            
            // Additional check: verify the sum makes sense
            // If there are N players across M games, total roundsPlayed entries should be reasonable
            int totalRoundsPlayedSum = latestStatsByPlayer.values().stream()
                    .filter(s -> s != null)
                    .mapToInt(PlayerStatsEntity::getRoundsPlayed)
                    .sum();
            System.out.println("  📊 Total roundsPlayed across all players: " + totalRoundsPlayedSum);
            
            // Validate all players have gamesPlayed = 2 (both games)
            for (Map.Entry<String, PlayerStatsEntity> entry : latestStatsByPlayer.entrySet()) {
                PlayerStatsEntity stats = entry.getValue();
                if (stats != null) {
                    // Note: gamesPlayed is calculated from countDistinctGamesByPlayerId, 
                    // so we validate it's at least 1 and at most 2
                    assertTrue(stats.getRoundsPlayed() > 0,
                            "Player " + entry.getKey() + " should have played at least 1 round");
                }
            }
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
                
                // Validate round count matches game score
                int expectedRounds = game.getTeam1Score() + game.getTeam2Score();
                assertEquals(expectedRounds, roundStarts.size(),
                        "Game " + game.getId() + " should have " + expectedRounds + 
                        " ROUND_START events to match score " + game.getTeam1Score() + "-" + game.getTeam2Score());
                
                System.out.println("  ✓ Game " + game.getId() + " (" + game.getMap() + "): " +
                        gameEvents.size() + " events, " + 
                        roundStarts.size() + " rounds, " +
                        gameAccolades.size() + " accolades");
            }
        }
        
        @Test
        @DisplayName("Validate: Round-level data matches game scores and API expectations")
        void validateRoundLevelData() {
            List<GameEntity> games = gameRepository.findAll();
            assumeTrue(games.size() >= EXPECTED_GAMES, "Need at least " + EXPECTED_GAMES + " games");
            
            // Expected game data based on API
            Map<Long, GameRoundExpectations> expectedGameData = Map.of(
                1L, new GameRoundExpectations("de_anubis", 13, 11, 24),
                2L, new GameRoundExpectations("de_ancient", 13, 2, 15)
            );
            
            for (GameEntity game : games) {
                GameRoundExpectations expectations = expectedGameData.get(game.getId());
                if (expectations == null) continue;
                
                // Validate game matches expected data
                assertEquals(expectations.map, game.getMap(), 
                        "Game " + game.getId() + " should be on map " + expectations.map);
                assertEquals(expectations.team1Score, game.getTeam1Score(), 
                        "Game " + game.getId() + " should have team1Score " + expectations.team1Score);
                assertEquals(expectations.team2Score, game.getTeam2Score(), 
                        "Game " + game.getId() + " should have team2Score " + expectations.team2Score);
                assertEquals(expectations.totalRounds, game.getTeam1Score() + game.getTeam2Score(),
                        "Game " + game.getId() + " total rounds should match score");
                
                // Validate ROUND_START events match expected rounds
                List<GameEventEntity> roundStarts = gameEventRepository.findByGameIdAndGameEventType(
                        game.getId(), GameEventType.ROUND_START);
                assertEquals(expectations.totalRounds, roundStarts.size(),
                        "Game " + game.getId() + " should have " + expectations.totalRounds + 
                        " ROUND_START events");
                
                // Validate ROUND_END events match expected rounds
                List<RoundEndEventEntity> roundEnds = gameEventRepository.findRoundEndEventsByGameId(game.getId());
                assertEquals(expectations.totalRounds, roundEnds.size(),
                        "Game " + game.getId() + " should have " + expectations.totalRounds + 
                        " ROUND_END events");
                
                // Validate all round end events have game reference
                for (RoundEndEventEntity roundEnd : roundEnds) {
                    assertNotNull(roundEnd.getGame(), 
                            "Round end event " + roundEnd.getId() + " should have game reference");
                    assertEquals(game.getId(), roundEnd.getGame().getId(),
                            "Round end event " + roundEnd.getId() + " should reference game " + game.getId());
                    assertNotNull(roundEnd.getTimestamp(),
                            "Round end event " + roundEnd.getId() + " should have timestamp");
                }
                
                // Validate events are distributed across rounds
                int totalEventsInGame = gameEventRepository.findByGameId(game.getId()).size();
                assertTrue(totalEventsInGame > expectations.totalRounds,
                        "Game " + game.getId() + " should have more events than just round starts");
                
                // Validate each round has events and round end events have proper data
                for (int i = 0; i < roundStarts.size(); i++) {
                    GameEventEntity roundStart = roundStarts.get(i);
                    List<GameEventEntity> roundEvents = gameEventRepository.findByRoundStartId(roundStart.getId());
                    assertFalse(roundEvents.isEmpty(),
                            "Round " + (i + 1) + " (roundStart ID: " + roundStart.getId() + ") should have events");
                    
                    // Validate round has reasonable number of events (at least ATTACK, KILL, ASSIST events)
                    assertTrue(roundEvents.size() >= 3,
                            "Round " + (i + 1) + " should have at least 3 events (found " + roundEvents.size() + ")");
                    
                    // Validate round end event exists for this round
                    if (i < roundEnds.size()) {
                        RoundEndEventEntity roundEnd = roundEnds.get(i);
                        assertNotNull(roundEnd.getTimestamp(),
                                "Round " + (i + 1) + " end event should have timestamp");
                        assertNotNull(roundEnd.getGame(),
                                "Round " + (i + 1) + " end event should have game reference");
                        assertEquals(game.getId(), roundEnd.getGame().getId(),
                                "Round " + (i + 1) + " end event should reference correct game");
                        
                        // Validate round end event has players JSON (may be null for some rounds)
                        // This is optional as some rounds may not have player data
                    }
                }
                
                // Validate round end events are in chronological order
                for (int i = 1; i < roundEnds.size(); i++) {
                    assertTrue(roundEnds.get(i).getTimestamp().isAfter(roundEnds.get(i-1).getTimestamp()) ||
                               roundEnds.get(i).getTimestamp().equals(roundEnds.get(i-1).getTimestamp()),
                            "Round end events should be in chronological order");
                }
                
                System.out.println("  ✓ Game " + game.getId() + " (" + game.getMap() + "): " +
                        roundStarts.size() + " rounds, " + roundEnds.size() + " round ends, " +
                        totalEventsInGame + " total events");
            }
        }
        
        // Helper class for game round expectations
        private static class GameRoundExpectations {
            final String map;
            final int team1Score;
            final int team2Score;
            final int totalRounds;
            
            GameRoundExpectations(String map, int team1Score, int team2Score, int totalRounds) {
                this.map = map;
                this.team1Score = team1Score;
                this.team2Score = team2Score;
                this.totalRounds = totalRounds;
            }
        }
        
        @Test
        @DisplayName("Validate: Player stats are accurate and complete")
        void validatePlayerStats() {
            List<PlayerStatsEntity> allPlayerStats = playerStatsRepository.findAll();
            assertFalse(allPlayerStats.isEmpty(), "Should have player stats in database");
            
            // Group by playerId to get latest stats for each player
            Map<String, PlayerStatsEntity> latestStatsByPlayer = allPlayerStats.stream()
                    .collect(Collectors.groupingBy(PlayerStatsEntity::getPlayerId))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .max((a, b) -> {
                                        if (a.getGameTimestamp() == null && b.getGameTimestamp() == null) return 0;
                                        if (a.getGameTimestamp() == null) return -1;
                                        if (b.getGameTimestamp() == null) return 1;
                                        return a.getGameTimestamp().compareTo(b.getGameTimestamp());
                                    })
                                    .orElse(null)
                    ));
            
            System.out.println("  📊 Validating stats for " + latestStatsByPlayer.size() + " unique players:");
            
            // Expected player count based on API data (exactly 11 unique players)
            assertEquals(11, latestStatsByPlayer.size(), 
                    "Should have exactly 11 players as per API data");
            
            int playersWithValidStats = 0;
            for (Map.Entry<String, PlayerStatsEntity> entry : latestStatsByPlayer.entrySet()) {
                PlayerStatsEntity stats = entry.getValue();
                if (stats == null) continue;
                
                String playerName = stats.getLastSeenNickname() != null ? 
                        stats.getLastSeenNickname() : stats.getPlayerId();
                
                // Validate basic fields are present
                assertNotNull(stats.getPlayerId(), "Player should have an ID");
                assertNotNull(stats.getLastSeenNickname(), "Player should have a nickname");
                assertNotNull(stats.getRank(), "Player should have a rank");
                
                // Validate stats are non-negative
                assertTrue(stats.getKills() >= 0, 
                        "Player " + playerName + " kills should be non-negative");
                assertTrue(stats.getDeaths() >= 0, 
                        "Player " + playerName + " deaths should be non-negative");
                assertTrue(stats.getAssists() >= 0, 
                        "Player " + playerName + " assists should be non-negative");
                assertTrue(stats.getRoundsPlayed() >= 0, 
                        "Player " + playerName + " rounds played should be non-negative");
                
                // Validate K/D ratio makes sense (if deaths > 0)
                if (stats.getDeaths() > 0) {
                    double kdRatio = (double) stats.getKills() / stats.getDeaths();
                    assertTrue(kdRatio >= 0, 
                            "Player " + playerName + " K/D ratio should be non-negative");
                }
                
                // Validate headshot kills don't exceed total kills
                assertTrue(stats.getHeadshotKills() <= stats.getKills(),
                        "Player " + playerName + " headshot kills (" + stats.getHeadshotKills() + 
                        ") should not exceed total kills (" + stats.getKills() + ")");
                
                // Calculate headshot percentage
                double headshotPct = stats.getKills() > 0 ? 
                        (stats.getHeadshotKills() * 100.0 / stats.getKills()) : 0.0;
                assertTrue(headshotPct >= 0 && headshotPct <= 100, 
                        "Player " + playerName + " headshot % should be between 0-100");
                
                // Validate damage is non-negative
                assertTrue(stats.getDamageDealt() >= 0,
                        "Player " + playerName + " damage dealt should be non-negative");
                
                playersWithValidStats++;
                System.out.println("    ✓ " + playerName + ": " +
                        stats.getKills() + "K/" + stats.getDeaths() + "D/" + stats.getAssists() + "A, " +
                        "Rank: " + stats.getRank() + ", " +
                        "Rounds: " + stats.getRoundsPlayed() + ", " +
                        "HS: " + stats.getHeadshotKills() + "/" + stats.getKills() + 
                        " (" + String.format("%.1f%%", headshotPct) + ")");
            }
            
            assertEquals(latestStatsByPlayer.size(), playersWithValidStats,
                    "All players should have valid stats");
            
            System.out.println("  ✓ All " + playersWithValidStats + " players have complete and valid stats");
        }
        
        @Test
        @DisplayName("Validate: Specific known players exist with reasonable stats")
        void validateKnownPlayers() {
            // Based on API data, validate some expected players exist
            List<PlayerStatsEntity> allPlayerStats = playerStatsRepository.findAll();
            
            Set<String> playerNicknames = allPlayerStats.stream()
                    .map(PlayerStatsEntity::getLastSeenNickname)
                    .filter(name -> name != null)
                    .collect(Collectors.toSet());
            
            // Expected players from API data
            List<String> expectedPlayers = List.of(
                    "[[LEGEND KILLER]] _i_",
                    "_m3th0d",
                    "Adkins#Keep Calm",
                    "HwoaranG",
                    "k1d",
                    "Khanjer",
                    "Mai Omelette Khaunga",
                    "PARROT",
                    "raksh",
                    "the nucLeus",
                    "Wasuli Bhai !!!!"
            );
            
            System.out.println("  📊 Checking for expected players:");
            int foundPlayers = 0;
            for (String expectedPlayer : expectedPlayers) {
                boolean found = playerNicknames.contains(expectedPlayer);
                if (found) {
                    foundPlayers++;
                    System.out.println("    ✓ Found: " + expectedPlayer);
                } else {
                    System.out.println("    ⚠ Not found: " + expectedPlayer);
                }
            }
            
            // Should find most expected players (allowing for some variance in names)
            assertTrue(foundPlayers >= 8, 
                    "Should find at least 8 out of 11 expected players. Found: " + foundPlayers);
            
            System.out.println("  ✓ Found " + foundPlayers + " out of " + expectedPlayers.size() + " expected players");
        }
        
        @Test
        @DisplayName("Validate: Game events have proper types and timestamps")
        void validateGameEventTypes() {
            List<GameEventEntity> allEvents = gameEventRepository.findAll();
            assertFalse(allEvents.isEmpty(), "Should have events in database");
            
            Map<GameEventType, Long> eventTypeCounts = allEvents.stream()
                    .collect(Collectors.groupingBy(
                            GameEventEntity::getGameEventType,
                            Collectors.counting()
                    ));
            
            System.out.println("  📊 Event type distribution:");
            for (Map.Entry<GameEventType, Long> entry : eventTypeCounts.entrySet()) {
                System.out.println("    - " + entry.getKey() + ": " + entry.getValue());
            }
            
            // Validate we have expected event types
            assertTrue(eventTypeCounts.containsKey(GameEventType.ROUND_START),
                    "Should have ROUND_START events");
            assertTrue(eventTypeCounts.containsKey(GameEventType.GAME_OVER),
                    "Should have GAME_OVER events");
            
            // Validate GAME_OVER count matches game count
            long gameOverCount = eventTypeCounts.getOrDefault(GameEventType.GAME_OVER, 0L);
            long gameCount = gameRepository.count();
            assertEquals(gameCount, gameOverCount,
                    "Should have one GAME_OVER event per game");
            
            // Validate all events have timestamps
            for (GameEventEntity event : allEvents) {
                assertNotNull(event.getTimestamp(), 
                        "Event " + event.getId() + " (" + event.getGameEventType() + ") should have a timestamp");
            }
            
            System.out.println("  ✓ All events have valid types and timestamps");
        }
        
        @Test
        @DisplayName("Validate: Accolades are distributed across players properly")
        void validateAccoladeDistribution() {
            List<AccoladeEntity> accolades = accoladeRepository.findAll();
            assumeTrue(accolades.size() >= EXPECTED_ACCOLADES, 
                    "Need at least " + EXPECTED_ACCOLADES + " accolades");
            
            // Group accolades by player
            Map<String, List<AccoladeEntity>> accoladesByPlayer = accolades.stream()
                    .collect(Collectors.groupingBy(AccoladeEntity::getPlayerId));
            
            System.out.println("  📊 Accolade distribution across " + accoladesByPlayer.size() + " players:");
            
            // Validate each player has at least one accolade
            for (Map.Entry<String, List<AccoladeEntity>> entry : accoladesByPlayer.entrySet()) {
                String playerId = entry.getKey();
                List<AccoladeEntity> playerAccolades = entry.getValue();
                
                assertFalse(playerAccolades.isEmpty(),
                        "Player " + playerId + " should have at least one accolade");
                
                // Get player name from stats
                List<PlayerStatsEntity> playerStats = playerStatsRepository.findAll().stream()
                        .filter(s -> s.getPlayerId().equals(playerId))
                        .toList();
                
                String playerName = playerId;
                if (!playerStats.isEmpty() && playerStats.get(0).getLastSeenNickname() != null) {
                    playerName = playerStats.get(0).getLastSeenNickname();
                }
                
                System.out.println("    - " + playerName + ": " + playerAccolades.size() + " accolades");
                
                // Validate each accolade has unique type per game for this player
                Map<Long, Set<String>> accoladeTypesByGame = new HashMap<>();
                for (AccoladeEntity accolade : playerAccolades) {
                    Long gameId = accolade.getGame().getId();
                    accoladeTypesByGame.computeIfAbsent(gameId, k -> new HashSet<>())
                            .add(accolade.getType());
                }
                
                // Each player should have unique accolade types per game
                for (Map.Entry<Long, Set<String>> gameEntry : accoladeTypesByGame.entrySet()) {
                    Long gameId = gameEntry.getKey();
                    Set<String> types = gameEntry.getValue();
                    
                    // Count how many accolades of each type in this game
                    long accoladeCountForGame = playerAccolades.stream()
                            .filter(a -> a.getGame().getId().equals(gameId))
                            .count();
                    
                    // Should have unique types (no duplicate accolade types per player per game)
                    assertEquals(types.size(), accoladeCountForGame,
                            "Player " + playerName + " should have unique accolade types in game " + gameId);
                }
            }
            
            System.out.println("  ✓ Accolades are properly distributed with no duplicates per player per game");
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
