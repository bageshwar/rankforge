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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for CS2 server log format
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
                    "<(?:CT|TERRORIST)>\" " +
                    "\\[-?\\d+ -?\\d+ -?\\d+\\] killed " +
                    "\"(?<victimName>.+?)" +                          // Victim name
                    "<\\d+>" +
                    "<(?:BOT|(?<victimSteamId>\\[U:\\d+:\\d+\\]))>" + // Victim steam ID (if not BOT)
                    "<(?:CT|TERRORIST)>\" " +
                    "\\[-?\\d+ -?\\d+ -?\\d+\\] with " +
                    "\"(?<weapon>[^\"]+)\"" +                         // Weapon used
                    "(?<isHeadshot> \\(headshot\\))?\\n?"
    );


    // L 04/20/2024 - 17:52:34: "MYTH<9><[U:1:1598851733]><CT>" assisted killing "Wasuli Bhai !!!<4><[U:1:1026155000]><TERRORIST>
    private static final Pattern ASSIST_PATTERN = Pattern.compile(
            "L \\d{2}/\\d{2}/\\d{4} - \\d{2}:\\d{2}:\\d{2}: " +
                    "\"(?<assistingPlayerName>.+?)" +                 // Assisting player name
                    "<\\d+>" +                                          // Player number
                    "<(?:BOT|(?<assistingPlayerSteamId>\\[U:\\d+:\\d+\\]))>" + // Steam ID or BOT
                    "<(?:CT|TERRORIST)>\" " +                           // Team
                    "(?<assistType>(?:flash-)?assisted) killing " +     // Assist type (flash or regular)
                    "\"(?<victimName>.+?)" +                         // Victim name
                    "<\\d+>" +                                         // Victim number
                    "<(?:BOT|(?<victimSteamId>\\[U:\\d+:\\d+\\]))>" + // Victim Steam ID or BOT
                    "<(?:CT|TERRORIST)>\"\\n?"
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
                    "\"(?<attackerName>[^<]+)<(?<attackerId>\\d+)><(?<attackerSteamId>\\[U:\\d:\\d+\\]|BOT)><(?<attackerTeam>\\w+)>\" " +
                    // Attacker position
                    "\\[(?<attackerX>-?\\d+) (?<attackerY>-?\\d+) (?<attackerZ>-?\\d+)\\] " +
                    // Action
                    "attacked " +
                    // Victim info
                    "\"(?<victimName>[^<]+)<(?<victimId>\\d+)><(?<victimSteamId>\\[U:\\d:\\d+\\]|BOT)><(?<victimTeam>\\w+)>\" " +
                    // Victim position
                    "\\[(?<victimX>-?\\d+) (?<victimY>-?\\d+) (?<victimZ>-?\\d+)\\] " +
                    // Weapon and damage details
                    "with \"(?<weapon>\\w+)\" " +
                    "\\(damage \"(?<damage>\\d+)\"\\) " +
                    "\\(damage_armor \"(?<damageArmor>\\d+)\"\\) " +
                    "\\(health \"(?<healthRemaining>\\d+)\"\\) " +
                    "\\(armor \"(?<armorRemaining>\\d+)\"\\) " +
                    "\\(hitgroup \"(?<hitgroup>\\w+)\"\\)"
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

    private final ObjectMapper objectMapper;
    private final EventStore eventStore;
    private final List<Integer> roundStartLineIndices;
    private boolean matchStarted;
    private int matchProcessingIndex;

    public CS2LogParser(ObjectMapper objectMapper, EventStore eventStore) {
        this.objectMapper = objectMapper;
        this.eventStore = eventStore;
        this.roundStartLineIndices = new ArrayList<>();
        matchStarted = false;
        matchProcessingIndex = 0;
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
            line = jsonNode.get("log").asText();
            
            //logger.debug("Parsing line {} (matchStarted={}, matchProcessingIndex={}): {}",
            //        currentIndex, matchStarted, matchProcessingIndex, line);

            if (matchProcessingIndex == currentIndex && matchStarted) {
                logger.info("Resetting round at {}", currentIndex);
                // reset all state, we have processed all rounds of this match
                this.matchStarted = false;
                this.matchProcessingIndex = 0;
                return Optional.of(new ParseLineResponse(new GameProcessedEvent(timestamp, Map.of()), currentIndex + 1));
            }

            // track rounds till match is not started (then process them)
            if (!matchStarted && line.contains("World triggered \"Round_Start\"")) {
                this.roundStartLineIndices.add(currentIndex);
                logger.debug("Tracking round start at {} (total tracked: {})", currentIndex, roundStartLineIndices.size());
                return Optional.empty();
            }

            Matcher gameOverMatcher = GAME_OVER_LOG_PATTERN.matcher(line);
            if (gameOverMatcher.matches()) {
                logger.info("Game over detected at index {}: {}", currentIndex, line);
                if (shouldProcessGameOverEvent(lines, currentIndex, timestamp)) {
                    return Optional.of(parseGameOverEvent(gameOverMatcher, timestamp, lines, currentIndex));
                } else {
                    logger.info("Skipping Game at index {}: {}", currentIndex, line);
                    this.roundStartLineIndices.clear();
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
            Matcher killMatcher = KILL_PATTERN.matcher(line);
            if (killMatcher.matches()) {
                return Optional.of(parseKillEvent(killMatcher, timestamp, lines, currentIndex));
            }

            Matcher assistMatcher = ASSIST_PATTERN.matcher(line);
            if (assistMatcher.matches()) {
                return Optional.of(parseAssistEvent(assistMatcher, timestamp, lines, currentIndex));
            }

            Matcher attackMatcher = ATTACK_PATTERN.matcher(line);
            if (attackMatcher.find()) {
                return Optional.of(parseAttackEvent(attackMatcher, timestamp, lines, currentIndex));
            }

            if (line.contains("World triggered \"Round_End\"")) {
                return Optional.of(parseRoundEndEvent(timestamp, lines, currentIndex));
            }

            // TODO Parse Accolade

            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to parse log line: {}", line, e);
            return Optional.empty();
        }
    }

    private boolean shouldProcessGameOverEvent(List<String> lines, int currentIndex, Instant timestamp) {
        // find if this was a serious game
        int i = currentIndex;
        int accoladesCount = 0;

        while(!lines.get(i).contains("ACCOLADE")) {
            i--;
        }

        // found accolades
        while(lines.get(i).contains("ACCOLADE")) {
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
        RoundStartEvent roundEndEvent = new RoundStartEvent(timestamp, Map.of());
        return new ParseLineResponse(roundEndEvent, currentIndex);
    }

    private ParseLineResponse parseGameOverEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) throws JsonProcessingException {
        this.matchStarted = true;
        int scoreTeam1 = Integer.parseInt(matcher.group("scoreTeam1"));
        int scoreTeam2 = Integer.parseInt(matcher.group("scoreTeam2"));
        // rewind back team1+team2 score rounds to start the tracking
        int roundToStart = this.roundStartLineIndices.size() - (scoreTeam1 + scoreTeam2);
        int indexToStart = this.roundStartLineIndices.get(roundToStart);
        this.matchProcessingIndex = currentIndex;
        this.roundStartLineIndices.clear();
        logger.info("In game over, moving pointer back {} rounds to {}, game over at {}", (scoreTeam1 + scoreTeam2), indexToStart, matchProcessingIndex);
        return new ParseLineResponse(new GameOverEvent(
                timestamp,
                Map.of(),
                matcher.group("map"),
                matcher.group("gameMode"),
                scoreTeam1, scoreTeam2
        ), indexToStart);
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
        return new ParseLineResponse(new AssistEvent(
                timestamp,
                Map.of(),
                new Player(matcher.group("assistingPlayerName"), matcher.group("assistingPlayerSteamId")),
                new Player(matcher.group("victimName"), matcher.group("victimSteamId")),
                null,
                matcher.group("assistType").contains("flash")
                        ? AssistEvent.AssistType.Flash
                        : AssistEvent.AssistType.Regular

        ), currentIndex);
    }

    private Instant parseTimestamp(String group) {
        return Instant.parse(group);
    }

    private ParseLineResponse parseKillEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) {
        // TODO Fix parse for killing bots, its steamId gets set as Null right now
        return new ParseLineResponse(new KillEvent(
                timestamp, Map.of(),
                new Player(matcher.group("killerName"), matcher.group("killerSteamId")),
                new Player(matcher.group("victimName"), matcher.group("victimSteamId")),
                matcher.group("weapon"),
                matcher.group(0).contains("headshot")
        ), currentIndex);
    }

    private ParseLineResponse parseAttackEvent(Matcher matcher, Instant timestamp, List<String> lines, int currentIndex) {
        return new ParseLineResponse(new AttackEvent(
                timestamp, Map.of(),
                new Player(matcher.group("attackerName"), matcher.group("attackerSteamId")),
                new Player(matcher.group("victimName"), matcher.group("victimSteamId")),
                matcher.group("weapon"),
                matcher.group("damage"),
                matcher.group("damageArmor"),
                matcher.group("hitgroup")
        ), currentIndex);
    }
}
