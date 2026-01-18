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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.events.*;
import com.rankforge.core.interfaces.LogParser;
import com.rankforge.core.internal.ParseLineResponse;
import com.rankforge.core.models.Player;
import com.rankforge.core.stores.EventStore;
import com.rankforge.pipeline.persistence.AccoladeStore;
import com.rankforge.pipeline.persistence.EventProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for CS2 game server log files.
 * 
 * THREADING MODEL:
 * ----------------
 * This class is designed to be INSTANCE-PER-PARSING-SESSION. It is NOT thread-safe and should NOT be shared.
 * 
 * Architecture:
 * - Each HTTP request to process a log file creates a NEW GameRankingSystem instance
 * - Each GameRankingSystem creates a NEW CS2LogParser instance
 * - Each parser processes lines SEQUENTIALLY within a single thread
 * - State fields (matchStarted, currentBombPlanter, etc.) are scoped to ONE log file processing session
 * 
 * Flow:
 *   HTTP Request 1 → @Async → New GameRankingSystem → New CS2LogParser (instance 1)
 *   HTTP Request 2 → @Async → New GameRankingSystem → New CS2LogParser (instance 2)
 * 
 * Therefore:
 * - Instance fields are safe to use for tracking state across events in a single log file
 * - No synchronization is needed since each instance is used by only one thread
 * - Parallel log processing is achieved by creating multiple parser instances, not sharing one
 * 
 * @see com.rankforge.server.service.LogProcessingService#processLogFileAsync
 * @see com.rankforge.server.service.PipelineService#createGameRankingSystem
 * 
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class CS2LogParser implements LogParser {
    private static final Logger logger = LoggerFactory.getLogger(CS2LogParser.class);

    // Regular expressions for different types of log events
    private static final Pattern KILL_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "\"(?<killerName>.+?)" +                           // Killer name
                    "<\\d+>" +
                    "<(?:BOT|(?<killerSteamId>\\[U:\\d+:\\d+\\]))>" + // Killer steam ID (if not BOT)
                    "<(?<killerTeam>CT|TERRORIST)>\" " +               // Killer team
                    "\\[(?<killerX>-?\\d+) (?<killerY>-?\\d+) (?<killerZ>-?\\d+)\\] killed (?:other )?" +  // Killer position, "other" is optional
                    "\"(?<victimName>.+?)" +                          // Victim name
                    "<\\d+>" +
                    "<(?:BOT|(?<victimSteamId>\\[U:\\d+:\\d+\\]))>" + // Victim steam ID (if not BOT)
                    "<(?<victimTeam>CT|TERRORIST)>\" " +               // Victim team
                    "\\[(?<victimX>-?\\d+) (?<victimY>-?\\d+) (?<victimZ>-?\\d+)\\] with " +  // Victim position
                    "\"(?<weapon>[^\"]+)\"" +                         // Weapon used
                    "(?<modifiers>(?: \\([^)]+\\))*)?\\n?"           // Optional modifiers like (headshot), (penetrated), (throughsmoke), etc.
    );


    // L 04/20/2024 - 17:52:34: "MYTH<9><[U:1:1598851733]><CT>" assisted killing "Wasuli Bhai !!!<4><[U:1:1026155000]><TERRORIST>"
    // Note: Assist events do NOT have coordinates in the log format
    private static final Pattern ASSIST_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "\"(?<assistingPlayerName>.+?)" +                 // Assisting player name
                    "<\\d+>" +                                          // Player number
                    "<(?:BOT|(?<assistingPlayerSteamId>\\[U:\\d+:\\d+\\]))>" + // Steam ID or BOT
                    "<(?<assistingPlayerTeam>CT|TERRORIST)>\" " +       // Assisting player team
                    "(?<assistType>(?:flash-)?assisted) killing " +     // Assist type (flash or regular)
                    "\"(?<victimName>.+?)" +                         // Victim name
                    "<\\d+>" +                                         // Victim number
                    "<(?:BOT|(?<victimSteamId>\\[U:\\d+:\\d+\\]))>" + // Victim Steam ID or BOT
                    "<(?<victimTeam>CT|TERRORIST)>\"\\n?"
    );

    // L 04/20/2024 - 16:21:52: "theWhiteNinja<1><[U:1:1135799416]><TERRORIST>" [-538 758 -23] attacked "Buckshot<5><BOT><CT>" [81 907 80] with "ak47" (damage "109") (damage_armor "15") (health "0") (armor "76") (hitgroup "head")
    /*private static final Pattern ATTACK_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "\"(?<attackerName>.+?)" +                 // Assisting player name
                    "<\\d+>" +                                          // Player number
                    "<(?:BOT|(?<attackerSteamId>\\[U:\\d+:\\d+\\]))>" + // Steam ID or BOT
                    "<(?:CT|TERRORIST)>\" " +                           // Team
                    "\\[-?\\d+ -?\\d+ -?\\d+\\] attacked " +
                    "\"(?<victimName>.+?)" +                         // Victim name
                    "<\\d+>" +                                         // Victim number
                    "<(?:BOT|(?<victimSteamId>\\[U:\\d+:\\d+\\]))>" + // Victim Steam ID or BOT
                    "<(?:CT|TERRORIST)>\"\\n?" +
                    "\\[-?\\d+ -?\\d+ -?\\d+\\] with " +
                    "\"(?<weapon>[^\"]+)\"" +                         // Weapon used
                    "\\(damage \"(?<damage>\\d+)\"\\) " +
                    "\\(damage_armor \"(?<damageArmor>\\d+)\"\\) " +
                    "\\(health \"(?<healthRemaining>\\d+)\"\\) " +
                    "\\(armor \"(?<armorRemaining>\\d+)\"\\) " +
                    "\\(hitgroup \"(?<hitgroup>\\w+)\"\\)\\n?"

    );*/

    private static final Pattern ATTACK_PATTERN = Pattern.compile(
            "L (?<time>\\d{2}\\/\\d{2}\\/\\d{4} - \\d{2}:\\d{2}:\\d{2}): " +
                    // Attacker info with Steam ID in new format and team
                    "\"(?<attackerName>[^<]+)<(?<attackerId>\\d+)><(?<attackerSteamId>\\[U:\\d+:\\d+\\]|BOT)><(?<attackerTeam>\\w+)>\" " +
                    // Attacker position
                    "\\[(?<attackerX>-?\\d+) (?<attackerY>-?\\d+) (?<attackerZ>-?\\d+)\\] " +
                    // Action
                    "attacked " +
                    // Victim info
                    "\"(?<victimName>[^<]+)<(?<victimId>\\d+)><(?<victimSteamId>\\[U:\\d+:\\d+\\]|BOT)><(?<victimTeam>\\w+)>\" " +
                    // Victim position
                    "\\[(?<victimX>-?\\d+) (?<victimY>-?\\d+) (?<victimZ>-?\\d+)\\] " +
                    // Weapon and damage details
                    "with \"(?<weapon>[^\"]+)\" " +
                    "\\(damage \"(?<damage>\\d+)\"\\) " +
                    "\\(damage_armor \"(?<damageArmor>\\d+)\"\\) " +
                    "\\(health \"(?<healthRemaining>\\d+)\"\\) " +
                    "\\(armor \"(?<armorRemaining>\\d+)\"\\) " +
                    "\\(hitgroup \"(?<hitgroup>[^\"]+)\"\\)\\n?"  // Optional newline at end
    );

    private static final Pattern ROUND_END_PATTERN = Pattern.compile(
            "L (?<time>\\d{2}\\/\\d{2}\\/\\d{4} - \\d{2}:\\d{2}:\\d{2}): " +
                    "World triggered \"Round_End\""
    );

    private static final Pattern GAME_OVER_LOG_PATTERN = Pattern.compile(
            "L (\\d{2}/\\d{2}/\\d{4}) - (\\d{2}:\\d{2}:\\d{2}): " +
                    "Game Over: (?<gameMode>\\w+) mg_active (?<map>\\w+_\\w+) " +
                    "score (?<scoreTeam1>\\d+):(?<scoreTeam2>\\d+) " +
                    "after (?<duration>\\d+) min\\r?\\n?"
    );
    
    // Pattern for parsing accolade lines
    // Format: L MM/DD/YYYY - HH:MM:SS: ACCOLADE, FINAL: {type},	PlayerName<id>,	VALUE: X,	POS: Y,	SCORE: Z
    // Example: L 01/07/2026 - 17:18:27: ACCOLADE, FINAL: {5k},	Khanjer<0>,	VALUE: 1.000000,	POS: 1,	SCORE: 40.000000
    // Note: Fields are separated by tabs (\t) and commas in the actual log format
    private static final Pattern ACCOLADE_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +  // Timestamp prefix
            "ACCOLADE, FINAL: \\{(?<type>[^}]+)\\}" +           // Type between {}
            "[,\\s\\t]+" +                                       // Separator (comma, space, or tab)
            "(?<playerName>[^<]+)" +                             // Player name (everything before <)
            "<(?<playerId>\\d+)>" +                               // Player ID between <>
            "[,\\s\\t]+" +                                       // Separator
            "VALUE: (?<value>\\d+(?:\\.\\d+)?)" +                // Value (integer or decimal)
            "[,\\s\\t]+" +                                       // Separator
            "POS: (?<position>\\d+)" +                           // Position
            "[,\\s\\t]+" +                                       // Separator
            "SCORE: (?<score>\\d+(?:\\.\\d+)?)" +                // Score (integer or decimal)
            "\\s*"                                               // Optional trailing whitespace
    );
    
    // Bomb event patterns
    // Format: L MM/DD/YYYY - HH:MM:SS: "PlayerName<ID><[U:1:123]><TEAM>" triggered "Planted_The_Bomb" at bombsite X
    private static final Pattern BOMB_PLANT_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "\"(?<playerName>[^<]+)" +
                    "<\\d+>" +
                    "<(?:BOT|(?<steamId>\\[U:\\d+:\\d+\\]))>" +
                    "<(?:CT|TERRORIST)>\" " +
                    "triggered \"Planted_The_Bomb\" at bombsite (?<bombsite>[AB])\\r?\\n?"
    );
    
    // Format: L MM/DD/YYYY - HH:MM:SS: "PlayerName<ID><[U:1:123]><CT>" triggered "Begin_Bomb_Defuse_With_Kit"
    // OR: "Begin_Bomb_Defuse_Without_Kit"
    private static final Pattern BOMB_DEFUSE_START_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "\"(?<playerName>[^<]+)" +
                    "<\\d+>" +
                    "<(?:BOT|(?<steamId>\\[U:\\d+:\\d+\\]))>" +
                    "<CT>\" " +
                    "triggered \"Begin_Bomb_Defuse_(?:With|Without)_Kit\"\\r?\\n?"
    );
    
    // Format: L MM/DD/YYYY - HH:MM:SS: Team "CT" triggered "SFUI_Notice_Bomb_Defused" (CT "12") (T "7")
    private static final Pattern BOMB_DEFUSED_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "Team \"CT\" triggered \"SFUI_Notice_Bomb_Defused\".*\\r?\\n?"
    );
    
    // Format: L MM/DD/YYYY - HH:MM:SS: Team "TERRORIST" triggered "SFUI_Notice_Target_Bombed" (CT "1") (T "5")
    private static final Pattern BOMB_EXPLODED_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "Team \"TERRORIST\" triggered \"SFUI_Notice_Target_Bombed\".*\\r?\\n?"
    );
    
    // Pattern to extract appServerId from ResetBreakpadAppId log line
    // Actual format from Docker logs: "ResetBreakpadAppId: Setting dedicated server app id: 2347773"
    // This is NOT a CS2 log line (no "L MM/DD/YYYY - HH:MM:SS:" prefix)
    // This appears at the beginning of log files, before any games start
    // Example: ResetBreakpadAppId: Setting dedicated server app id: 2347773
    private static final Pattern RESET_BREAKPAD_APP_ID_PATTERN = Pattern.compile(
            "ResetBreakpadAppId:\\s*Setting\\s+dedicated\\s+server\\s+app\\s+id:\\s*(?<appServerId>\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;
    private final EventStore eventStore;
    private final AccoladeStore accoladeStore;
    private final EventProcessingContext eventProcessingContext;
    private final List<Integer> roundStartLineIndices;
    private boolean matchStarted;
    private int matchProcessingIndex;
    
    // Bomb event state tracking (per round)
    // These are cleared on each Round_Start and used to attribute team-level bomb events to players
    private Player currentBombPlanter;      // Player who planted the bomb in current round
    private String currentBombsite;         // Bombsite where bomb was planted (A or B)
    private Player currentBombDefuser;      // Player attempting to defuse (most recent)

    /**
     * Constructor with EventProcessingContext (preferred for production use).
     */
    public CS2LogParser(ObjectMapper objectMapper, EventStore eventStore, AccoladeStore accoladeStore, 
                       EventProcessingContext eventProcessingContext) {
        this.objectMapper = objectMapper;
        this.eventStore = eventStore;
        this.accoladeStore = accoladeStore;
        this.eventProcessingContext = eventProcessingContext;
        this.roundStartLineIndices = new ArrayList<>();
        matchStarted = false;
        matchProcessingIndex = 0;
    }
    
    /**
     * Backward-compatible constructor for tests.
     * Creates a new EventProcessingContext instance.
     * @deprecated Use the constructor with EventProcessingContext parameter for production code.
     */
    @Deprecated
    public CS2LogParser(ObjectMapper objectMapper, EventStore eventStore, AccoladeStore accoladeStore) {
        this(objectMapper, eventStore, accoladeStore, new EventProcessingContext());
    }

    @Override
    public Optional<ParseLineResponse> parseLine(String line, List<String> lines, int currentIndex) {
        try {
            /*
            Keep skipping log lines till we have a Game Over , keep making note of previous round starts
            Once a Game Over is found, rewind back "n" rounds and process from there.
            If GameOver is found, skip the next incoming game over and reset the clock
             */
            String original = line;
            JsonNode jsonNode = objectMapper.readTree(line);

            Instant timestamp = parseTimestamp(jsonNode.get("time").asText());
            String logContent = jsonNode.get("log").asText();
            
            // Parse ResetBreakpadAppId log line early (before any games start)
            // This identifies which dedicated server the logs came from
            // Format: "ResetBreakpadAppId: Setting dedicated server app id: 2347773"
            // This MUST be extracted before any game events are processed
            Matcher resetBreakpadMatcher = RESET_BREAKPAD_APP_ID_PATTERN.matcher(logContent);
            if (resetBreakpadMatcher.find()) {
                try {
                    Long appServerId = Long.parseLong(resetBreakpadMatcher.group("appServerId"));
                    eventProcessingContext.setAppServerId(appServerId);
                    logger.info("✅ APP_SERVER_ID: Extracted appServerId={} from ResetBreakpadAppId log line at index={}", 
                            appServerId, currentIndex);
                    logger.debug("✅ APP_SERVER_ID: Full logContent='{}'", logContent);
                    logger.debug("✅ APP_SERVER_ID: Context now has appServerId={}, verified: {}", 
                            appServerId, eventProcessingContext.getAppServerId());
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse appServerId from line: {}", logContent, e);
                }
                // Return empty - this is just metadata, not a game event
                return Optional.empty();
            }
            
            // Debug: Log if we're about to process a game event and appServerId is not set
            if (logContent.contains("Game Over") || logContent.contains("GAME_OVER")) {
                Long currentAppServerId = eventProcessingContext.getAppServerId();
                if (currentAppServerId == null) {
                    logger.error("❌ APP_SERVER_ID: About to process GAME_OVER event but appServerId is NULL in context! Line index={}", 
                            currentIndex);
                    logger.error("❌ APP_SERVER_ID: LogContent='{}'", logContent);
                    logger.error("❌ APP_SERVER_ID: Data ingestion will FAIL - ResetBreakpadAppId must appear before game events");
                } else {
                    logger.debug("✅ APP_SERVER_ID: GAME_OVER detected, appServerId={} is available in context", currentAppServerId);
                }
            }
            
            line = logContent;

            if (matchProcessingIndex == currentIndex && matchStarted) {
                logger.debug("Resetting match state at {} after processing all rounds", currentIndex);
                // reset all state, we have processed all rounds of this match
                this.matchStarted = false;
                this.matchProcessingIndex = 0;
                return Optional.of(new ParseLineResponse(new GameProcessedEvent(timestamp, Map.of()), currentIndex + 1));
            }

            // track rounds till match is not started (then process them)
            if (line.contains("World triggered \"Round_Start\"")) {
                // We're processing a game if matchProcessingIndex > 0 and currentIndex < matchProcessingIndex
                // (we're between the rewind point and the game over)
                boolean isProcessingGame = matchProcessingIndex > 0 && currentIndex < matchProcessingIndex;
                
                // If matchStarted is true but we have no rounds tracked AND we're not currently processing a game,
                // we're starting a new game after a previous game finished. Reset matchStarted to false so we can 
                // track round starts for this new game.
                if (matchStarted && roundStartLineIndices.size() == 0 && !isProcessingGame) {
                    logger.debug("Detected new game: resetting matchStarted=false to track round starts at {}", currentIndex);
                    this.matchStarted = false;
                    this.matchProcessingIndex = 0;
                }
                
                if (!matchStarted) {
                    this.roundStartLineIndices.add(currentIndex);
                    logger.debug("Tracking round start at {} (total tracked: {})", 
                            currentIndex, roundStartLineIndices.size());
                    return Optional.empty();
                }
            }

            Matcher gameOverMatcher = GAME_OVER_LOG_PATTERN.matcher(line);
            if (gameOverMatcher.matches()) {
                logger.info("Game over detected at index {}: {}", currentIndex, line);
                if (shouldProcessGameOverEvent(lines, currentIndex, timestamp)) {
                    return Optional.of(parseGameOverEvent(gameOverMatcher, timestamp, lines, currentIndex));
                } else {
                    logger.info("Skipping Game at index {}: {}", currentIndex, line);
                    // Reset state so we can track round starts for the next game
                    this.roundStartLineIndices.clear();
                    this.matchStarted = false;
                    return Optional.empty();
                }

            }

            // Don't start the scoring till the match is started
            if (!matchStarted) {
                return Optional.empty();
            }

            if (line.contains("World triggered \"Round_Start\"")) {
                return Optional.of(parseRoundStartEvent(timestamp, lines, currentIndex));
            }

            // Try to match different event patterns
            // Check attack pattern first - it's more specific than kill
            Matcher attackMatcher = ATTACK_PATTERN.matcher(line);
            if (attackMatcher.matches()) {
                return Optional.of(parseAttackEvent(attackMatcher, timestamp, lines, currentIndex));
            }

            Matcher killMatcher = KILL_PATTERN.matcher(line);
            if (killMatcher.matches()) {
                return Optional.of(parseKillEvent(killMatcher, timestamp, lines, currentIndex));
            }

            Matcher assistMatcher = ASSIST_PATTERN.matcher(line);
            if (assistMatcher.matches()) {
                return Optional.of(parseAssistEvent(assistMatcher, timestamp, lines, currentIndex));
            }

            // Check for bomb events
            Matcher bombPlantMatcher = BOMB_PLANT_PATTERN.matcher(line);
            if (bombPlantMatcher.matches()) {
                return Optional.of(parseBombPlantEvent(bombPlantMatcher, timestamp, currentIndex));
            }

            Matcher bombDefuseStartMatcher = BOMB_DEFUSE_START_PATTERN.matcher(line);
            if (bombDefuseStartMatcher.matches()) {
                storeBombDefuser(bombDefuseStartMatcher);
                return Optional.empty(); // Don't create event, just track for later
            }

            Matcher bombDefusedMatcher = BOMB_DEFUSED_PATTERN.matcher(line);
            if (bombDefusedMatcher.matches()) {
                return Optional.of(createBombDefusedEvent(timestamp, currentIndex));
            }

            Matcher bombExplodedMatcher = BOMB_EXPLODED_PATTERN.matcher(line);
            if (bombExplodedMatcher.matches()) {
                return Optional.of(createBombExplodedEvent(timestamp, currentIndex));
            }

            if (line.contains("World triggered \"Round_End\"")) {
                return Optional.of(parseRoundEndEvent(timestamp, lines, currentIndex));
            }

            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to parse log line: {}", line, e);
            return Optional.empty();
        }
    }

    private boolean shouldProcessGameOverEvent(List<String> lines, int currentIndex, Instant timestamp) {
        // find if this was a serious game
        int i = currentIndex - 1; // Start from line before game over
        int accoladesCount = 0;

        // Find the start of accolades section (going backwards)
        while (i >= 0 && !lines.get(i).contains("ACCOLADE")) {
            i--;
        }

        // Count all accolade lines
        while (i >= 0 && lines.get(i).contains("ACCOLADE")) {
            accoladesCount++;
            i--;
        }

        logger.info("After Game over, accolades: {}", accoladesCount);

        if (accoladesCount < 6) {
            // not enough players
            return false;
        } else {
            // find existing processed event
            Optional<GameEvent> event = eventStore.getGameEvent(GameEventType.GAME_OVER, timestamp);
            if (event.isPresent()) {
                logger.info("Game already processed in previous runs. line: {} , at {}", currentIndex, timestamp);
                return false;
            } else {
                return true;
            }
        }

    }

    private ParseLineResponse parseRoundStartEvent(Instant timestamp, List<String> lines, int currentIndex) throws JsonProcessingException {
        // Clear bomb state for new round
        this.currentBombPlanter = null;
        this.currentBombsite = null;
        this.currentBombDefuser = null;
        
        RoundStartEvent roundStartEvent = new RoundStartEvent(timestamp, Map.of());
        return new ParseLineResponse(roundStartEvent, currentIndex);
    }

    private ParseLineResponse parseGameOverEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) throws JsonProcessingException {
        this.matchStarted = true;
        
        int scoreTeam1 = Integer.parseInt(matcher.group("scoreTeam1"));
        int scoreTeam2 = Integer.parseInt(matcher.group("scoreTeam2"));
        
        // Extract duration from the log pattern
        Integer duration = null;
        try {
            String durationStr = matcher.group("duration");
            if (durationStr != null && !durationStr.isEmpty()) {
                duration = Integer.parseInt(durationStr);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to parse duration from game over event", e);
        }
        
        // Parse and queue accolades from log lines (will be linked to GameEntity later)
        parseAndQueueAccolades(lines, currentIndex);
        
        // rewind back team1+team2 score rounds to start the tracking
        int totalRounds = scoreTeam1 + scoreTeam2;
        
        if (this.roundStartLineIndices.size() < totalRounds) {
            throw new IllegalStateException(String.format(
                    "Catastrophic failure: Not enough round starts tracked. Expected at least %d rounds (score %d:%d), " +
                    "but only tracked %d round starts. Log file may be incomplete or missing Round_Start events.",
                    totalRounds, scoreTeam1, scoreTeam2, this.roundStartLineIndices.size()));
        }
        
        // We have tracked enough rounds, rewind back to the start of the match
        int roundToStart = this.roundStartLineIndices.size() - totalRounds;
        int indexToStart = this.roundStartLineIndices.get(roundToStart) - 1;
        this.matchProcessingIndex = currentIndex;
        this.roundStartLineIndices.clear();
        logger.info("In game over, moving pointer back {} rounds to {}, game over at {}, duration: {} min", 
                totalRounds, indexToStart, matchProcessingIndex, duration);
        return new ParseLineResponse(new GameOverEvent(
                timestamp,
                new HashMap<String, String>(),
                matcher.group("map"),
                matcher.group("gameMode"),
                scoreTeam1, scoreTeam2,
                duration
        ), indexToStart);
    }
    
    /**
     * Parse accolades from log lines after game over event and queue them for batch persistence.
     * Accolades are added to the EventProcessingContext and will be linked to GameEntity
     * when it's created during GAME_OVER event processing.
     * Uses regex pattern matching similar to other event parsers (KILL_PATTERN, ASSIST_PATTERN, etc.)
     */
    private void parseAndQueueAccolades(List<String> lines, int gameOverIndex) {
        List<AccoladeStore.Accolade> accolades = new ArrayList<>();
        
        try {
            // Find accolades by going backwards from game over line
            // Start from the line before game over (game over is at gameOverIndex)
            int i = gameOverIndex - 1;
            
            // Find the start of accolades section
            while (i >= 0 && !lines.get(i).contains("ACCOLADE")) {
                i--;
            }
            
            // Parse all accolade lines using regex pattern matching
            while (i >= 0 && lines.get(i).contains("ACCOLADE")) {
                String line = lines.get(i);
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    String logLine = jsonNode.get("log").asText();
                    
                    // Use regex pattern to parse accolade (similar to parseKillEvent, parseAssistEvent)
                    Matcher accoladeMatcher = ACCOLADE_PATTERN.matcher(logLine);
                    if (accoladeMatcher.matches()) {
                        String type = accoladeMatcher.group("type");
                        String playerName = accoladeMatcher.group("playerName").trim();
                        String playerId = accoladeMatcher.group("playerId");
                        double value = Double.parseDouble(accoladeMatcher.group("value"));
                        int position = Integer.parseInt(accoladeMatcher.group("position"));
                        double score = Double.parseDouble(accoladeMatcher.group("score"));
                        
                        AccoladeStore.Accolade accolade = new AccoladeStore.Accolade(
                                type, playerName, playerId, value, position, score
                        );
                        accolades.add(accolade);
                        logger.debug("Parsed accolade: type={}, player={}, value={}, pos={}, score={}", 
                                type, playerName, value, position, score);
                    } else {
                        logger.debug("Accolade line did not match pattern: {}", logLine);
                    }
                } catch (Exception e) {
                    logger.debug("Failed to parse accolade line: {}", line, e);
                }
                i--;
            }
            
            // Queue accolades for deferred persistence (will be linked to GameEntity later)
            if (!accolades.isEmpty()) {
                accoladeStore.queueAccolades(accolades);
                logger.debug("Parsed and queued {} accolades for game over event", accolades.size());
            } else {
                logger.debug("No accolades found for game over event");
            }
        } catch (Exception e) {
            logger.warn("Failed to parse and queue accolades", e);
        }
    }

    private ParseLineResponse parseRoundEndEvent(Instant timestamp, List<String> lines, int currentIndex) throws JsonProcessingException {
        RoundEndEvent roundEndEvent = new RoundEndEvent(timestamp, Map.of());

        int movedIndex = currentIndex;
        while (movedIndex < lines.size() - 1) {
            if (lines.get(++movedIndex).contains("JSON_BEGIN")) {
                break;
            }

            // If this is the last round of the match, the tabular scores won't be printed.
            if (lines.get(movedIndex).contains("ACCOLADE")) {
                return new ParseLineResponse(roundEndEvent, movedIndex);
            }
        }

        movedIndex = movedIndex + 6;

        while (movedIndex < lines.size() - 1) {
            String line = lines.get(++movedIndex);
            if (line.contains("JSON_END")) {
                break;
            } else if (line.contains("player_")) {
                String text = objectMapper.readTree(line).get("log").asText();
                int colonIndex = text.lastIndexOf(":");
                String[] values = text.substring(colonIndex + 3).split(",");
                roundEndEvent.getPlayers().add(values[0].trim());
            }
        }

        return new ParseLineResponse(roundEndEvent, movedIndex);
    }

    private ParseLineResponse parseAssistEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) {
        // Assist events do NOT have coordinates in the log format
        // Extract team information
        String assistingPlayerTeam = matcher.group("assistingPlayerTeam");
        String victimTeam = matcher.group("victimTeam");
        
        AssistEvent assistEvent = new AssistEvent(
                timestamp,
                Map.of(),
                new Player(matcher.group("assistingPlayerName"), matcher.group("assistingPlayerSteamId"), assistingPlayerTeam),
                new Player(matcher.group("victimName"), matcher.group("victimSteamId"), victimTeam),
                null,
                matcher.group("assistType").contains("flash")
                        ? AssistEvent.AssistType.Flash
                        : AssistEvent.AssistType.Regular
        );
        // Coordinates remain null for assist events
        
        return new ParseLineResponse(assistEvent, currentIndex);
    }

    private Instant parseTimestamp(String group) {
        return Instant.parse(group);
    }

    private ParseLineResponse parseKillEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) {
        // TODO Fix parse for killing bots, its steamId gets set as Null right now
        Integer killerX = parseCoordinate(matcher.group("killerX"));
        Integer killerY = parseCoordinate(matcher.group("killerY"));
        Integer killerZ = parseCoordinate(matcher.group("killerZ"));
        Integer victimX = parseCoordinate(matcher.group("victimX"));
        Integer victimY = parseCoordinate(matcher.group("victimY"));
        Integer victimZ = parseCoordinate(matcher.group("victimZ"));
        
        // Check for headshot in modifiers (could be (headshot), (headshot penetrated), (headshot throughsmoke), etc.)
        String modifiers = matcher.group("modifiers");
        boolean isHeadshot = modifiers != null && modifiers.contains("headshot");
        
        // Extract team information
        String killerTeam = matcher.group("killerTeam");
        String victimTeam = matcher.group("victimTeam");
        
        KillEvent killEvent = new KillEvent(
                timestamp, Map.of(),
                new Player(matcher.group("killerName"), matcher.group("killerSteamId"), killerTeam),
                new Player(matcher.group("victimName"), matcher.group("victimSteamId"), victimTeam),
                matcher.group("weapon"),
                isHeadshot
        );
        killEvent.setPlayer1X(killerX);
        killEvent.setPlayer1Y(killerY);
        killEvent.setPlayer1Z(killerZ);
        killEvent.setPlayer2X(victimX);
        killEvent.setPlayer2Y(victimY);
        killEvent.setPlayer2Z(victimZ);
        
        return new ParseLineResponse(killEvent, currentIndex);
    }
    
    private Integer parseCoordinate(String coordStr) {
        try {
            return coordStr != null ? Integer.parseInt(coordStr) : null;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse coordinate: {}", coordStr);
            return null;
        }
    }

    /**
     * Package-private method for testing individual event parsing without state machine checks.
     * This method directly attempts to parse a log line as various event types.
     * 
     * @param logLine The extracted log content (not JSON wrapped)
     * @param timestamp The timestamp for the event
     * @return Optional containing the parsed event, or empty if no pattern matched
     */
    Optional<GameEvent> parseEventForTesting(String logLine, Instant timestamp) {
        // Try attack pattern
        Matcher attackMatcher = ATTACK_PATTERN.matcher(logLine);
        if (attackMatcher.matches()) {
            ParseLineResponse response = parseAttackEvent(attackMatcher, timestamp, null, 0);
            return Optional.of(response.getGameEvent());
        }
        
        // Try kill pattern
        Matcher killMatcher = KILL_PATTERN.matcher(logLine);
        if (killMatcher.matches()) {
            ParseLineResponse response = parseKillEvent(killMatcher, timestamp, null, 0);
            return Optional.of(response.getGameEvent());
        }
        
        // Try assist pattern
        Matcher assistMatcher = ASSIST_PATTERN.matcher(logLine);
        if (assistMatcher.matches()) {
            ParseLineResponse response = parseAssistEvent(assistMatcher, timestamp, null, 0);
            return Optional.of(response.getGameEvent());
        }
        
        return Optional.empty();
    }

    private ParseLineResponse parseAttackEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) {
        Integer attackerX = parseCoordinate(matcher.group("attackerX"));
        Integer attackerY = parseCoordinate(matcher.group("attackerY"));
        Integer attackerZ = parseCoordinate(matcher.group("attackerZ"));
        Integer victimX = parseCoordinate(matcher.group("victimX"));
        Integer victimY = parseCoordinate(matcher.group("victimY"));
        Integer victimZ = parseCoordinate(matcher.group("victimZ"));
        
        // Extract team information
        String attackerTeam = matcher.group("attackerTeam");
        String victimTeam = matcher.group("victimTeam");
        
        AttackEvent attackEvent = new AttackEvent(
                timestamp, Map.of(),
                new Player(matcher.group("attackerName"), matcher.group("attackerSteamId"), attackerTeam),
                new Player(matcher.group("victimName"), matcher.group("victimSteamId"), victimTeam),
                matcher.group("weapon"),
                matcher.group("damage"),
                matcher.group("damageArmor"),
                matcher.group("hitgroup"),
                matcher.group("healthRemaining")
        );
        attackEvent.setPlayer1X(attackerX);
        attackEvent.setPlayer1Y(attackerY);
        attackEvent.setPlayer1Z(attackerZ);
        attackEvent.setPlayer2X(victimX);
        attackEvent.setPlayer2Y(victimY);
        attackEvent.setPlayer2Z(victimZ);
        
        return new ParseLineResponse(attackEvent, currentIndex);
    }

    private ParseLineResponse parseBombPlantEvent(Matcher matcher, Instant timestamp, int currentIndex) {
        String playerName = matcher.group("playerName").trim();
        String steamId = matcher.group("steamId");
        String bombsite = matcher.group("bombsite");
        
        // Store for later attribution
        this.currentBombPlanter = new Player(steamId != null ? steamId : playerName, playerName);
        this.currentBombsite = bombsite;
        
        BombEvent bombEvent = new BombEvent(
            timestamp,
            Map.of("bombsite", bombsite),
            steamId != null ? steamId : playerName,
            BombEvent.BombEventType.PLANT,
            0
        );
        return new ParseLineResponse(bombEvent, currentIndex);
    }

    private void storeBombDefuser(Matcher matcher) {
        String playerName = matcher.group("playerName").trim();
        String steamId = matcher.group("steamId");
        // Store most recent defuser (overwrites previous if multiple attempts)
        this.currentBombDefuser = new Player(steamId != null ? steamId : playerName, playerName);
    }

    private ParseLineResponse createBombDefusedEvent(Instant timestamp, int currentIndex) {
        if (this.currentBombDefuser == null) {
            logger.warn("Bomb defused but no defuser tracked - returning empty event");
            return new ParseLineResponse(new GameProcessedEvent(timestamp, Map.of()), currentIndex);
        }
        
        BombEvent bombEvent = new BombEvent(
            timestamp,
            this.currentBombsite != null ? Map.of("bombsite", this.currentBombsite) : Map.of(),
            this.currentBombDefuser.getSteamId(),
            BombEvent.BombEventType.DEFUSE,
            0
        );
        return new ParseLineResponse(bombEvent, currentIndex);
    }

    private ParseLineResponse createBombExplodedEvent(Instant timestamp, int currentIndex) {
        if (this.currentBombPlanter == null) {
            logger.warn("Bomb exploded but no planter tracked - returning empty event");
            return new ParseLineResponse(new GameProcessedEvent(timestamp, Map.of()), currentIndex);
        }
        
        BombEvent bombEvent = new BombEvent(
            timestamp,
            this.currentBombsite != null ? Map.of("bombsite", this.currentBombsite) : Map.of(),
            this.currentBombPlanter.getSteamId(),
            BombEvent.BombEventType.EXPLODE,
            0
        );
        return new ParseLineResponse(bombEvent, currentIndex);
    }
}
