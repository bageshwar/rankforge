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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Standalone integration test for S3Service to verify S3 connectivity
 * This test manually loads credentials from application-local.properties
 * and creates S3Service directly, avoiding Spring Boot context requirements
 * 
 * To run: mvn test -Dtest=S3ServiceStandaloneTest
 */
class S3ServiceStandaloneTest {

    private S3Service createS3ServiceFromProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application-local.properties")) {
            if (is == null) {
                throw new IOException("application-local.properties not found");
            }
            props.load(is);
        }

        String accessKey = props.getProperty("rankforge.s3.accessKey", "");
        String secretKey = props.getProperty("rankforge.s3.secretKey", "");
        String region = props.getProperty("rankforge.s3.region", "us-east-1");

        // Skip if credentials are not configured
        assumeTrue(!accessKey.isEmpty() && !accessKey.equals("your_aws_access_key_here") 
                && !accessKey.equals("AKIAIOSFODNN7EXAMPLE"),
                "S3 credentials must be configured in application-local.properties");

        return new S3Service(accessKey, secretKey, region);
    }

    @Test
    void testS3ServiceCreation() throws IOException {
        S3Service s3Service = createS3ServiceFromProperties();
        assertNotNull(s3Service, "S3Service should be created");
    }

    @Test
    void testS3Connection_WithValidPath() throws IOException {
        S3Service s3Service = createS3ServiceFromProperties();
        
        // Actual S3 path in your bucket for testing
        String testS3Path = "s3://cs2serverdata/cs2_log_2026-01-07.json";
        
        assertDoesNotThrow(() -> {
            var lines = s3Service.downloadFileAsLines(testS3Path);
            assertNotNull(lines, "Should return list of lines");
            assertFalse(lines.isEmpty(), "File should not be empty");
            System.out.println("✅ Successfully downloaded " + lines.size() + " lines from S3");
            System.out.println("✅ First line preview: " + 
                (lines.get(0).length() > 100 ? lines.get(0).substring(0, 100) + "..." : lines.get(0)));
        }, "Should successfully download file from S3");
    }

    @Test
    void testS3Connection_WithInvalidPath() throws IOException {
        S3Service s3Service = createS3ServiceFromProperties();
        
        String invalidPath = "s3://non-existent-bucket-12345/invalid-file.json";
        
        assertThrows(Exception.class, () -> {
            s3Service.downloadFileAsLines(invalidPath);
        }, "Should throw exception for invalid S3 path");
    }
}
