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
import com.rankforge.server.dto.ProcessLogRequest;
import com.rankforge.server.dto.ProcessLogResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
class PipelineApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rankforge.api.key:}")
    private String configuredApiKey;

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
}
