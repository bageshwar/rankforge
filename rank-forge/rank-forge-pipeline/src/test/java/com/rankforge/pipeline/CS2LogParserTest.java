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
import com.rankforge.core.events.*;
import com.rankforge.core.internal.ParseLineResponse;
import com.rankforge.core.models.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for CS2LogParser
 */
@ExtendWith(MockitoExtension.class)
class CS2LogParserTest {

    private CS2LogParser parser;
    private ObjectMapper objectMapper;
    private List<String> mockLines;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new CS2LogParser(objectMapper);
        mockLines = new ArrayList<>();
    }

    private String createJsonLogLine(String logContent, String timestamp) {
        return String.format("{\"time\":\"%s\",\"log\":\"%s\"}", 
            timestamp != null ? timestamp : "2024-04-20T17:52:34Z", 
            logContent.replace("\"", "\\\""));
    }

    @Nested
    @DisplayName("Kill Event Parsing Tests")
    class KillEventTests {

        @Test
        @DisplayName("Should parse regular kill event correctly")
        void shouldParseRegularKillEvent() {
            // Given
            String logContent = "L 04/20/2024 - 17:52:34: \"Player1<9><[U:1:123456]><CT>\" " +
                              "[-538 758 -23] killed \"Player2<4><[U:1:789012]><TERRORIST>\" " +
                              "[-81 907 80] with \"ak47\"";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof KillEvent);
            
            KillEvent killEvent = (KillEvent) response.getGameEvent();
            assertEquals(GameEventType.KILL, killEvent.type());
            assertEquals(Instant.parse("2024-04-20T17:52:34Z"), killEvent.getTimestamp());
            
            // Check killer
            Player killer = killEvent.getPlayer1();
            assertEquals("Player1", killer.getName());
            assertEquals("[U:1:123456]", killer.getSteamId());
            assertFalse(killer.isBot());
            
            // Check victim
            Player victim = killEvent.getPlayer2();
            assertEquals("Player2", victim.getName());
            assertEquals("[U:1:789012]", victim.getSteamId());
            assertFalse(victim.isBot());
            
            // Check weapon and headshot
            assertEquals("ak47", killEvent.getWeapon());
            assertFalse(killEvent.isHeadshot());
        }

        @Test
        @DisplayName("Should parse headshot kill event correctly")
        void shouldParseHeadshotKillEvent() {
            // Given
            String logContent = "L 04/20/2024 - 17:52:34: \"Player1<9><[U:1:123456]><CT>\" " +
                              "[-538 758 -23] killed \"Player2<4><[U:1:789012]><TERRORIST>\" " +
                              "[-81 907 80] with \"ak47\" (headshot)";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            KillEvent killEvent = (KillEvent) result.get().getGameEvent();
            assertTrue(killEvent.isHeadshot());
        }

        @Test
        @DisplayName("Should parse kill event with BOT player correctly")
        void shouldParseKillEventWithBot() {
            // Given
            String logContent = "L 04/20/2024 - 17:52:34: \"Player1<9><[U:1:123456]><CT>\" " +
                              "[-538 758 -23] killed \"Bot Player<4><BOT><TERRORIST>\" " +
                              "[-81 907 80] with \"ak47\"";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            KillEvent killEvent = (KillEvent) result.get().getGameEvent();
            
            // Check victim is BOT
            Player victim = killEvent.getPlayer2();
            assertEquals("Bot Player", victim.getName());
            assertNull(victim.getSteamId()); // BOT players have null steamId
            assertTrue(victim.isBot());
        }
    }

    @Nested
    @DisplayName("Assist Event Parsing Tests")
    class AssistEventTests {

        @Test
        @DisplayName("Should parse regular assist event correctly")
        void shouldParseRegularAssistEvent() {
            // Given
            String logContent = "L 04/20/2024 - 17:52:34: \"Player1<9><[U:1:123456]><CT>\" " +
                              "assisted killing \"Player2<4><[U:1:789012]><TERRORIST>\"";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof AssistEvent);
            
            AssistEvent assistEvent = (AssistEvent) response.getGameEvent();
            assertEquals(GameEventType.ASSIST, assistEvent.type());
            assertEquals(Instant.parse("2024-04-20T17:52:34Z"), assistEvent.getTimestamp());
            
            // Check assisting player
            Player assister = assistEvent.getPlayer1();
            assertEquals("Player1", assister.getName());
            assertEquals("[U:1:123456]", assister.getSteamId());
            
            // Check victim
            Player victim = assistEvent.getPlayer2();
            assertEquals("Player2", victim.getName());
            assertEquals("[U:1:789012]", victim.getSteamId());
            
            // Check assist type
            assertEquals(AssistEvent.AssistType.Regular, assistEvent.getAssistType());
        }

        @Test
        @DisplayName("Should parse flash assist event correctly")
        void shouldParseFlashAssistEvent() {
            // Given
            String logContent = "L 04/20/2024 - 17:52:34: \"Player1<9><[U:1:123456]><CT>\" " +
                              "flash-assisted killing \"Player2<4><[U:1:789012]><TERRORIST>\"";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            AssistEvent assistEvent = (AssistEvent) result.get().getGameEvent();
            assertEquals(AssistEvent.AssistType.Flash, assistEvent.getAssistType());
        }

        @Test
        @DisplayName("Should parse assist event with BOT correctly")
        void shouldParseAssistEventWithBot() {
            // Given
            String logContent = "L 04/20/2024 - 17:52:34: \"Bot Helper<9><BOT><CT>\" " +
                              "assisted killing \"Player2<4><[U:1:789012]><TERRORIST>\"";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            AssistEvent assistEvent = (AssistEvent) result.get().getGameEvent();
            
            Player assister = assistEvent.getPlayer1();
            assertEquals("Bot Helper", assister.getName());
            assertNull(assister.getSteamId());
            assertTrue(assister.isBot());
        }
    }

    @Nested
    @DisplayName("Attack Event Parsing Tests")
    class AttackEventTests {

        @Test
        @DisplayName("Should parse attack event correctly")
        void shouldParseAttackEvent() {

            // force match start
            String roundStartContent = "L 04/20/2024 - 17:00:00 : World triggered \"Round_Start\"";
            String jsonLine = createJsonLogLine(roundStartContent, "2024-04-20T17:00:00Z");
            mockLines.add(jsonLine);
            // trigger a parse so that round start gets accounted for
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);
            assertFalse(result.isPresent());

            // Given
            String logContent = "L 04/20/2024 - 16:21:52: \"theWhiteNinja<1><[U:1:1135799416]><TERRORIST>\" [-538 758 -23] attacked \"Buckshot<5><BOT><CT>\" [81 907 80] with \"ak47\" (damage \"109\") (damage_armor \"15\") (health \"0\") (armor \"76\") (hitgroup \"head\")";
            jsonLine = createJsonLogLine(logContent, "2024-04-20T16:21:52Z");
            mockLines.add(jsonLine);

            result = parser.parseLine(jsonLine, mockLines, 1);
            assertFalse(result.isPresent());

            String gameOverLogContent = "L 04/20/2024 - 18:30:45: Game Over: competitive mg_active de_dust2 score 1:0 after 45 min";
            jsonLine = createJsonLogLine(gameOverLogContent, "2024-04-20T18:30:45Z");
            mockLines.add(jsonLine);

            result = parser.parseLine(jsonLine, mockLines, 2);
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof GameOverEvent);

            // When
            result = parser.parseLine(mockLines.get(1), mockLines, 1);

            // Then
            assertTrue(result.isPresent());
            response = result.get();
            assertTrue(response.getGameEvent() instanceof AttackEvent);
            
            AttackEvent attackEvent = (AttackEvent) response.getGameEvent();
            assertEquals(GameEventType.ATTACK, attackEvent.type());
            assertEquals(Instant.parse("2024-04-20T16:21:52Z"), attackEvent.getTimestamp());
            
            // Check attacker
            Player attacker = attackEvent.getPlayer1();
            assertEquals("theWhiteNinja", attacker.getName());
            assertEquals("[U:1:1135799416]", attacker.getSteamId());
            assertFalse(attacker.isBot());
            
            // Check victim
            Player victim = attackEvent.getPlayer2();
            assertEquals("Buckshot", victim.getName());
            assertEquals("BOT", victim.getSteamId()); // BOT
            assertTrue(victim.isBot());
            
            // Check weapon and damage info
            assertEquals("ak47", attackEvent.getWeapon());
            assertEquals(109, attackEvent.getDamage());
            assertEquals(15, attackEvent.getArmorDamage());
            assertEquals("head", attackEvent.getHitGroup());
        }

        @Test
        @DisplayName("Should parse attack event with different hitgroup")
        void shouldParseAttackEventWithBodyShot() {
            // Given
            String logContent = "L 04/20/2024 - 16:21:52: \"Player1<1><[U:1:123456]><CT>\" " +
                              "[-538 758 -23] attacked \"Player2<5><[U:1:789012]><TERRORIST>\" [81 907 80] " +
                              "with \"m4a1\" (damage \"35\") (damage_armor \"8\") (health \"65\") " +
                              "(armor \"92\") (hitgroup \"chest\")";
            String jsonLine = createJsonLogLine(logContent, "2024-04-20T16:21:52Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            AttackEvent attackEvent = (AttackEvent) result.get().getGameEvent();
            
            assertEquals("m4a1", attackEvent.getWeapon());
            assertEquals(35, attackEvent.getDamage());
            assertEquals(8, attackEvent.getArmorDamage());
            assertEquals("chest", attackEvent.getHitGroup());
        }
    }

    @Nested
    @DisplayName("Round Event Parsing Tests")
    class RoundEventTests {

        @Test
        @DisplayName("Should parse round start event correctly")
        void shouldParseRoundStartEvent() {
            // Given - first trigger game over to enable match processing
            String gameOverLogContent = "L 04/20/2024 - 18:30:45: Game Over: competitive mg_active de_dust2 " +
                                      "score 16:10 after 45 min";
            String gameOverJsonLine = createJsonLogLine(gameOverLogContent, "2024-04-20T18:30:45Z");
            mockLines.add(gameOverJsonLine);
            
            // Trigger game over first
            parser.parseLine(gameOverJsonLine, mockLines, 0);
            
            String roundStartLogContent = "L 04/20/2024 - 17:52:34: World triggered \"Round_Start\"";
            String jsonLine = createJsonLogLine(roundStartLogContent, "2024-04-20T17:52:34Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 1);

            // Then
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof RoundStartEvent);
            
            RoundStartEvent roundStartEvent = (RoundStartEvent) response.getGameEvent();
            assertEquals(GameEventType.ROUND_START, roundStartEvent.type());
            assertEquals(Instant.parse("2024-04-20T17:52:34Z"), roundStartEvent.getTimestamp());
        }

        @Test
        @DisplayName("Should parse round end event correctly")
        void shouldParseRoundEndEvent() {
            // Given - first trigger game over to enable match processing
            String gameOverLogContent = "L 04/20/2024 - 18:30:45: Game Over: competitive mg_active de_dust2 " +
                                      "score 16:10 after 45 min";
            String gameOverJsonLine = createJsonLogLine(gameOverLogContent, "2024-04-20T18:30:45Z");
            mockLines.add(gameOverJsonLine);
            
            // Setup mock lines for round end parsing
            mockLines.add("{\"time\":\"2024-04-20T17:54:00Z\",\"log\":\"L 04/20/2024 - 17:54:00: World triggered \\\"Round_End\\\"\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:01Z\",\"log\":\"Some other log\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:02Z\",\"log\":\"JSON_BEGIN\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:03Z\",\"log\":\"header1\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:04Z\",\"log\":\"header2\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:05Z\",\"log\":\"header3\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:06Z\",\"log\":\"header4\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:07Z\",\"log\":\"header5\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:08Z\",\"log\":\"header6\"}");
            //mockLines.add("{\"time\":\"2024-04-20T17:54:09Z\",\"log\":\"\\\"player_1\\\":\\\" PlayerName1, 15, 8, 2, 3, 1500\\\"\"}");
            //mockLines.add("{\"time\":\"2024-04-20T17:54:09Z\",\"log\":\"\\\"player_1\\\":\\\" PlayerName1, 15, 8, 2, 3, 1500\\\"\"}");
            mockLines.add("{\"log\":\"L 08/01/2025 - 17:25:24: \\\"player_1\\\" : \\\"                   100,      3,  16000,      0,      0,      0,      0,   0.00,   0.00,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0\\\"\\n\",\"stream\":\"stdout\",\"time\":\"2025-08-01T17:30:01.538231956Z\"}\n");
            mockLines.add("{\"log\":\"L 08/01/2025 - 17:25:24: \\\"player_1\\\" : \\\"                   101,      3,  16000,      0,      0,      0,      0,   0.00,   0.00,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0,      0\\\"\\n\",\"stream\":\"stdout\",\"time\":\"2025-08-01T17:30:01.538231956Z\"}\n");
            
            // Trigger game over first
            parser.parseLine(gameOverJsonLine, mockLines, 0);
            
            // When
            Optional<ParseLineResponse> result = parser.parseLine(mockLines.get(1), mockLines, 1);

            // Then
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof RoundEndEvent);
            
            RoundEndEvent roundEndEvent = (RoundEndEvent) response.getGameEvent();
            assertEquals(GameEventType.ROUND_END, roundEndEvent.type());
            assertEquals(Instant.parse("2024-04-20T17:54:00Z"), roundEndEvent.getTimestamp());
            
            // Check that players were parsed
            List<String> players = roundEndEvent.getPlayers();
            assertEquals(2, players.size());
            assertEquals("100", players.get(0));
            assertEquals("101", players.get(1));
        }
    }

    @Nested
    @DisplayName("Game Event Parsing Tests")
    class GameEventTests {

        @Test
        @DisplayName("Should parse game over event correctly")
        void shouldParseGameOverEvent() {
            // Given - need to setup round starts first
            for (int i = 0; i < 26; i++) { // 16 + 10 = 26 rounds
                String roundStartContent = "L 04/20/2024 - 17:" + String.format("%02d", i) + ":00: World triggered \"Round_Start\"";
                String jsonLine = createJsonLogLine(roundStartContent, "2024-04-20T17:" + String.format("%02d", i) + ":00Z");
                mockLines.add(jsonLine);
                // trigger a parse so that round start gets accounted for
                parser.parseLine(jsonLine, mockLines, i);
            }
            
            String gameOverLogContent = "L 04/20/2024 - 18:30:45: Game Over: competitive mg_active de_dust2 " +
                                      "score 16:10 after 45 min";
            String jsonLine = createJsonLogLine(gameOverLogContent, "2024-04-20T18:30:45Z");
            mockLines.add(jsonLine);

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 26);

            // Then
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof GameOverEvent);
            
            GameOverEvent gameOverEvent = (GameOverEvent) response.getGameEvent();
            assertEquals(GameEventType.GAME_OVER, gameOverEvent.type());
            assertEquals(Instant.parse("2024-04-20T18:30:45Z"), gameOverEvent.getTimestamp());
            assertEquals("de_dust2", gameOverEvent.getMap());
            assertEquals("competitive", gameOverEvent.getMode());
            assertEquals(16, gameOverEvent.getTeam1Score());
            assertEquals(10, gameOverEvent.getTeam2Score());
            
            // Check that it rewinds to the correct round start
            assertEquals(0, response.getNextIndex()); // Should rewind to beginning
        }

        @Test
        @DisplayName("Should parse game processed event correctly")
        void shouldParseGameProcessedEvent() {
            // Given - setup a match that's already started
            String gameOverLogContent = "L 04/20/2024 - 18:30:45: Game Over: competitive mg_active de_dust2 " +
                                      "score 16:10 after 45 min";
            String gameOverJsonLine = createJsonLogLine(gameOverLogContent, "2024-04-20T18:30:45Z");
            mockLines.add(gameOverJsonLine);
            
            // Trigger game over to start match processing
            parser.parseLine(gameOverJsonLine, mockLines, 0);
            
            // Now simulate reaching the end of match processing
            String dummyLogContent = "L 04/20/2024 - 18:31:00: Some log line";
            String jsonLine = createJsonLogLine(dummyLogContent, "2024-04-20T18:31:00Z");
            mockLines.add(jsonLine);

            // When - parse at the same index as the original game over (simulating end of processing)
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isPresent());
            ParseLineResponse response = result.get();
            assertTrue(response.getGameEvent() instanceof GameProcessedEvent);
            
            GameProcessedEvent gameProcessedEvent = (GameProcessedEvent) response.getGameEvent();
            assertEquals(GameEventType.GAME_PROCESSED, gameProcessedEvent.type());
            assertEquals(Instant.parse("2024-04-20T18:31:00Z"), gameProcessedEvent.getTimestamp());
            assertEquals(1, response.getNextIndex()); // Should move to next line
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return empty for invalid JSON")
        void shouldReturnEmptyForInvalidJson() {
            // Given
            String invalidJson = "not valid json";

            // When
            Optional<ParseLineResponse> result = parser.parseLine(invalidJson, mockLines, 0);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty for unrecognized log patterns")
        void shouldReturnEmptyForUnrecognizedLogPatterns() {
            // Given
            String unrecognizedLogContent = "L 04/20/2024 - 17:52:34: Some unrecognized log message";
            String jsonLine = createJsonLogLine(unrecognizedLogContent, "2024-04-20T17:52:34Z");

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty for round start when match not started")
        void shouldReturnEmptyForRoundStartWhenMatchNotStarted() {
            // Given
            String roundStartLogContent = "L 04/20/2024 - 17:52:34: World triggered \"Round_Start\"";
            String jsonLine = createJsonLogLine(roundStartLogContent, "2024-04-20T17:52:34Z");

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle missing JSON fields gracefully")
        void shouldHandleMissingJsonFields() {
            // Given
            String incompleteJson = "{\"time\":\"2024-04-20T17:52:34Z\"}"; // missing log field

            // When
            Optional<ParseLineResponse> result = parser.parseLine(incompleteJson, mockLines, 0);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle malformed kill event")
        void shouldHandleMalformedKillEvent() {
            // Given
            String malformedKillLogContent = "L 04/20/2024 - 17:52:34: incomplete kill event";
            String jsonLine = createJsonLogLine(malformedKillLogContent, "2024-04-20T17:52:34Z");

            // When
            Optional<ParseLineResponse> result = parser.parseLine(jsonLine, mockLines, 0);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle round end event without JSON data")
        void shouldHandleRoundEndEventWithoutJsonData() {
            // Given - first trigger game over to enable match processing
            String gameOverLogContent = "L 04/20/2024 - 18:30:45: Game Over: competitive mg_active de_dust2 " +
                                      "score 16:10 after 45 min";
            String gameOverJsonLine = createJsonLogLine(gameOverLogContent, "2024-04-20T18:30:45Z");
            mockLines.add(gameOverJsonLine);
            
            // Setup mock lines without proper JSON structure
            mockLines.add("{\"time\":\"2024-04-20T17:54:00Z\",\"log\":\"L 04/20/2024 - 17:54:00: World triggered \\\"Round_End\\\"\"}");
            mockLines.add("{\"time\":\"2024-04-20T17:54:01Z\",\"log\":\"ACCOLADE data\"}"); // Should trigger early return
            
            // Trigger game over first
            parser.parseLine(gameOverJsonLine, mockLines, 0);
            
            // When
            Optional<ParseLineResponse> result = parser.parseLine(mockLines.get(1), mockLines, 1);

            // Then
            assertTrue(result.isPresent());
            assertTrue(result.get().getGameEvent() instanceof RoundEndEvent);
            RoundEndEvent roundEndEvent = (RoundEndEvent) result.get().getGameEvent();
            assertTrue(roundEndEvent.getPlayers().isEmpty()); // No players parsed
        }
    }
}