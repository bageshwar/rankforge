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

import com.rankforge.pipeline.GameRankingSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;

/**
 * Unit tests for LogProcessingService
 * Tests async log processing with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class LogProcessingServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private PipelineService pipelineService;

    @Mock
    private GameRankingSystem gameRankingSystem;

    private LogProcessingService logProcessingService;

    @BeforeEach
    void setUp() {
        logProcessingService = new LogProcessingService(s3Service, pipelineService);
    }

    @Test
    void testProcessLogFileAsync_WithValidS3Path_ReturnsJobId() throws IOException {
        String s3Path = "s3://test-bucket/path/to/log.json";
        List<String> mockLines = Arrays.asList(
                "{\"time\":\"2024-01-01T00:00:00Z\",\"log\":\"test log line 1\"}",
                "{\"time\":\"2024-01-01T00:00:01Z\",\"log\":\"test log line 2\"}"
        );

        when(s3Service.downloadFileAsLines(s3Path)).thenReturn(mockLines);
        when(pipelineService.createGameRankingSystem()).thenReturn(gameRankingSystem);

        // Note: Since processLogFileAsync is @Async, the actual execution happens in a separate thread
        // We can verify the method returns a CompletableFuture with job ID immediately
        CompletableFuture<String> futureJobId = logProcessingService.processLogFileAsync(s3Path);
        
        assertNotNull(futureJobId, "Future should not be null");
        
        // Get the job ID (it's already completed, so this returns immediately)
        String jobId = futureJobId.join();

        assertNotNull(jobId, "Job ID should not be null");
        assertFalse(jobId.isEmpty(), "Job ID should not be empty");
        
        // Verify UUID format
        assertDoesNotThrow(() -> UUID.fromString(jobId), "Job ID should be a valid UUID");
    }

    @Test
    void testProcessLogFileAsync_WithS3Error_HandlesGracefully() throws IOException {
        String s3Path = "s3://test-bucket/invalid/path.json";

        lenient().when(s3Service.downloadFileAsLines(s3Path))
                .thenThrow(new IOException("File not found in S3"));

        // Should not throw exception, but log error (async processing)
        assertDoesNotThrow(() -> {
            CompletableFuture<String> futureJobId = logProcessingService.processLogFileAsync(s3Path);
            assertNotNull(futureJobId);
            String jobId = futureJobId.join();
            assertNotNull(jobId);
        });
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify the stubbing was used (in async context)
        verify(s3Service, timeout(1000)).downloadFileAsLines(s3Path);
    }

    @Test
    void testProcessLogFileAsync_WithProcessingError_HandlesGracefully() throws IOException {
        String s3Path = "s3://test-bucket/path/to/log.json";
        List<String> mockLines = Arrays.asList("invalid json line");

        lenient().when(s3Service.downloadFileAsLines(s3Path)).thenReturn(mockLines);
        lenient().when(pipelineService.createGameRankingSystem()).thenReturn(gameRankingSystem);

        // Processing errors should be caught and logged, not thrown
        assertDoesNotThrow(() -> {
            CompletableFuture<String> futureJobId = logProcessingService.processLogFileAsync(s3Path);
            assertNotNull(futureJobId);
            String jobId = futureJobId.join();
            assertNotNull(jobId);
        });
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify the stubbings were used (in async context)
        verify(s3Service, timeout(1000)).downloadFileAsLines(s3Path);
        verify(pipelineService, timeout(1000)).createGameRankingSystem();
    }
}
