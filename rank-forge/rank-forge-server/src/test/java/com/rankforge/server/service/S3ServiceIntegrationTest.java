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

package com.rankforge.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for S3Service to verify S3 connectivity
 * Requires actual AWS credentials configured in application-local.properties
 * 
 * To run: mvn test -Dtest=S3ServiceIntegrationTest -Dspring.profiles.active=local
 * 
 * Note: This test uses TestPropertySource to override database config with empty values
 * to avoid database connection requirements for S3-only testing
 */
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
    "rankforge.persistence.jdbc.url=",
    "rankforge.persistence.jdbc.username=",
    "rankforge.persistence.jdbc.password=",
    "rankforge.persistence.type="
})
class S3ServiceIntegrationTest {

    @Autowired(required = false)
    private S3Service s3Service;

    @Test
    void testS3ServiceIsConfigured() {
        assumeTrue(s3Service != null, "S3Service must be available - check Spring context loads correctly");
        assertNotNull(s3Service, "S3Service should be autowired");
    }

    @Test
    void testS3Connection_WithValidPath() {
        assumeTrue(s3Service != null, "S3Service must be available");
        
        // Actual S3 path in your bucket for testing
        String testS3Path = "s3://cs2serverdata/cs2_log_2026-01-07.json";
        
        assertDoesNotThrow(() -> {
            List<String> lines = s3Service.downloadFileAsLines(testS3Path);
            assertNotNull(lines, "Should return list of lines");
            assertFalse(lines.isEmpty(), "File should not be empty");
            System.out.println("✅ Successfully downloaded " + lines.size() + " lines from S3");
        }, "Should successfully download file from S3");
    }

    @Test
    void testS3Connection_WithInvalidPath() {
        assumeTrue(s3Service != null, "S3Service must be available");
        
        String invalidPath = "s3://non-existent-bucket/invalid-file.json";
        
        assertThrows(Exception.class, () -> {
            s3Service.downloadFileAsLines(invalidPath);
        }, "Should throw exception for invalid S3 path");
    }
}
