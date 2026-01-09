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
import com.rankforge.server.service.LogProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PipelineApiController
 * Tests REST endpoint with security and async processing
 */
@WebMvcTest(PipelineApiController.class)
@TestPropertySource(properties = {"rankforge.api.key=test-api-key-12345"})
class PipelineApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogProcessingService logProcessingService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_API_KEY = "test-api-key-12345";

    @Test
    void testProcessLogFile_WithValidRequest_ReturnsAccepted() throws Exception {
        String s3Path = "s3://test-bucket/path/to/log.json";
        String jobId = UUID.randomUUID().toString();
        ProcessLogRequest request = new ProcessLogRequest(s3Path);

        when(logProcessingService.processLogFileAsync(s3Path)).thenReturn(jobId);

        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.message").exists());

        verify(logProcessingService, times(1)).processLogFileAsync(s3Path);
    }

    @Test
    void testProcessLogFile_WithoutApiKey_ReturnsUnauthorized() throws Exception {
        String s3Path = "s3://test-bucket/path/to/log.json";
        ProcessLogRequest request = new ProcessLogRequest(s3Path);

        mockMvc.perform(post("/api/pipeline/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(logProcessingService, never()).processLogFileAsync(anyString());
    }

    @Test
    void testProcessLogFile_WithInvalidApiKey_ReturnsUnauthorized() throws Exception {
        String s3Path = "s3://test-bucket/path/to/log.json";
        ProcessLogRequest request = new ProcessLogRequest(s3Path);

        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(logProcessingService, never()).processLogFileAsync(anyString());
    }

    @Test
    void testProcessLogFile_WithEmptyS3Path_ReturnsBadRequest() throws Exception {
        ProcessLogRequest request = new ProcessLogRequest("");

        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(logProcessingService, never()).processLogFileAsync(anyString());
    }

    @Test
    void testProcessLogFile_WithNullS3Path_ReturnsBadRequest() throws Exception {
        ProcessLogRequest request = new ProcessLogRequest(null);

        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(logProcessingService, never()).processLogFileAsync(anyString());
    }

    @Test
    void testProcessLogFile_WithInvalidS3PathFormat_ReturnsBadRequest() throws Exception {
        ProcessLogRequest request = new ProcessLogRequest("invalid-path");

        when(logProcessingService.processLogFileAsync("invalid-path"))
                .thenThrow(new IllegalArgumentException("Invalid S3 path format"));

        mockMvc.perform(post("/api/pipeline/process")
                        .header("X-API-Key", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testHealthEndpoint_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/pipeline/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pipeline API is healthy"));
    }

    @Test
    void testHealthEndpoint_DoesNotRequireApiKey() throws Exception {
        // Health endpoint should be accessible without API key
        mockMvc.perform(get("/api/pipeline/health"))
                .andExpect(status().isOk());
    }
}
