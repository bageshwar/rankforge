/*
 *
 *  *Copyright [2026] [Bageshwar Pratap Narain]
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

package com.rankforge.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ObjectMapperFactory.
 * Verifies that date/time serialization works correctly for frontend consumption.
 * 
 * Author bageshwar.pn
 * Date 2026
 */
class ObjectMapperFactoryTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.createObjectMapper();
    }

    @Test
    @DisplayName("Instant should be serialized as ISO-8601 string, not numeric timestamp")
    void instantShouldBeSerializedAsIsoString() throws JsonProcessingException {
        // Given: A specific timestamp
        Instant timestamp = Instant.parse("2026-01-07T17:18:27Z");

        // When: Serializing the Instant directly
        String json = objectMapper.writeValueAsString(timestamp);

        // Then: It should be a quoted ISO-8601 string, not a numeric timestamp
        assertEquals("\"2026-01-07T17:18:27Z\"", json,
                "Instant should be serialized as ISO-8601 string for JavaScript compatibility");
        
        // Verify it's NOT a numeric value (which would cause JavaScript Date parsing issues)
        assertFalse(json.matches("^\\d+(\\.\\d+)?$"),
                "Instant should NOT be serialized as a numeric timestamp");
    }

    @Test
    @DisplayName("Object with Instant field should serialize date as ISO-8601 string")
    void objectWithInstantFieldShouldSerializeDateAsIsoString() throws JsonProcessingException {
        // Given: A DTO-like object with an Instant field
        TestDto dto = new TestDto();
        dto.gameDate = Instant.parse("2026-01-07T17:18:27Z");
        dto.name = "Test Game";

        // When: Serializing the object
        String json = objectMapper.writeValueAsString(dto);

        // Then: The gameDate should be an ISO-8601 string in the JSON
        assertTrue(json.contains("\"gameDate\":\"2026-01-07T17:18:27Z\""),
                "Instant field should be serialized as ISO-8601 string. Got: " + json);
        
        // Verify it doesn't contain a numeric timestamp for gameDate
        assertFalse(json.matches(".*\"gameDate\":\\d+.*"),
                "gameDate should NOT be a numeric timestamp. Got: " + json);
    }

    @Test
    @DisplayName("Serialized Instant should be parseable by JavaScript new Date()")
    void serializedInstantShouldBeParseableByJavaScript() throws JsonProcessingException {
        // Given: A timestamp that previously caused issues (Jan 21, 1970 bug)
        Instant timestamp = Instant.parse("2026-01-07T17:18:27Z");

        // When: Serializing the Instant
        String json = objectMapper.writeValueAsString(timestamp);
        
        // Then: Remove quotes and verify it's a valid ISO-8601 format
        String isoString = json.replace("\"", "");
        
        // Verify the format matches ISO-8601 (which JavaScript can parse)
        assertTrue(isoString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"),
                "Serialized format should be ISO-8601. Got: " + isoString);
        
        // Verify we can parse it back correctly
        Instant parsed = Instant.parse(isoString);
        assertEquals(timestamp, parsed, "Round-trip parsing should preserve the timestamp");
    }

    @Test
    @DisplayName("Instant with nanoseconds should serialize correctly")
    void instantWithNanosecondsShouldSerializeCorrectly() throws JsonProcessingException {
        // Given: A timestamp with nanosecond precision
        Instant timestamp = Instant.parse("2026-01-07T17:18:27.123456789Z");

        // When: Serializing the Instant
        String json = objectMapper.writeValueAsString(timestamp);

        // Then: It should be a quoted string containing the full precision
        assertTrue(json.startsWith("\""), "Should be a quoted string");
        assertTrue(json.endsWith("\""), "Should be a quoted string");
        assertTrue(json.contains("2026-01-07"), "Should contain the date");
        assertTrue(json.contains("17:18:27"), "Should contain the time");
    }

    /**
     * Simple test DTO to verify serialization of objects containing Instant fields.
     * Mimics the structure of GameDTO.
     */
    static class TestDto {
        public Instant gameDate;
        public String name;
    }
}
