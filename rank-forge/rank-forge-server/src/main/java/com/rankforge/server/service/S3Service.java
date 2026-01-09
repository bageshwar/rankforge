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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for downloading files from AWS S3
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class S3Service {
    
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    private final String region;

    public S3Service(
            @Value("${rankforge.s3.accessKey:}") String accessKey,
            @Value("${rankforge.s3.secretKey:}") String secretKey,
            @Value("${rankforge.s3.region:us-east-1}") String region) {
        
        this.region = region;
        
        // Initialize S3 client with credentials if provided
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            logger.info("Initialized S3 client with provided credentials for region: {}", region);
        } else {
            // Use default credential chain (IAM role, environment variables, etc.)
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .build();
            logger.info("Initialized S3 client with default credential chain for region: {}", region);
        }
    }

    /**
     * Downloads a file from S3 and returns its contents as a list of lines
     * 
     * @param s3Path S3 path in format "s3://bucket/key" or "bucket/key"
     * @return List of lines from the file
     * @throws IOException if file cannot be downloaded or read
     * @throws IllegalArgumentException if S3 path is invalid
     */
    public List<String> downloadFileAsLines(String s3Path) throws IOException {
        logger.info("Downloading file from S3: {}", s3Path);
        
        S3Path parsed = parseS3Path(s3Path);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(parsed.bucket)
                    .key(parsed.key)
                    .build();
            
            // Download entire file into memory
            ResponseInputStream<GetObjectResponse> responseInputStream = 
                    s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
            
            // Read all lines into memory
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseInputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            
            logger.info("Successfully downloaded {} lines from S3 path: {}", lines.size(), s3Path);
            return lines;
            
        } catch (NoSuchKeyException e) {
            logger.error("File not found in S3: {}", s3Path, e);
            throw new IOException("File not found in S3: " + s3Path, e);
        } catch (S3Exception e) {
            // Handle different S3 error types with more specific messages
            String errorMessage = getS3ErrorMessage(e, parsed.bucket, s3Path);
            logger.error("S3 error while downloading file: {} - {}", s3Path, errorMessage, e);
            throw new IOException(errorMessage, e);
        }
    }

    /**
     * Gets a user-friendly error message for S3 exceptions
     */
    private String getS3ErrorMessage(S3Exception e, String bucket, String s3Path) {
        int statusCode = e.statusCode();
        String errorCode = e.awsErrorDetails().errorCode();
        
        if (statusCode == 301) {
            // Bucket region mismatch or bucket doesn't exist
            return String.format(
                "Bucket '%s' not found or is in a different region. " +
                "Current region: %s. Please verify the bucket name and region. Path: %s",
                bucket, region, s3Path
            );
        } else if (statusCode == 403) {
            return String.format(
                "Access denied to bucket '%s'. Check your AWS credentials and IAM permissions. Path: %s",
                bucket, s3Path
            );
        } else if (statusCode == 404) {
            return String.format(
                "Bucket '%s' not found. Verify the bucket name exists. Path: %s",
                bucket, s3Path
            );
        } else {
            return String.format(
                "S3 error (%s - %s): %s. Path: %s",
                statusCode, errorCode, e.getMessage(), s3Path
            );
        }
    }

    /**
     * Parses S3 path into bucket and key components
     * Supports formats: "s3://bucket/key" or "bucket/key"
     */
    private S3Path parseS3Path(String s3Path) {
        if (s3Path == null || s3Path.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 path cannot be null or empty");
        }
        
        String path = s3Path.trim();
        
        // Remove s3:// prefix if present
        if (path.startsWith("s3://")) {
            path = path.substring(5);
        }
        
        // Split into bucket and key
        int firstSlash = path.indexOf('/');
        if (firstSlash == -1) {
            throw new IllegalArgumentException("Invalid S3 path format. Expected: s3://bucket/key or bucket/key");
        }
        
        String bucket = path.substring(0, firstSlash);
        String key = path.substring(firstSlash + 1);
        
        if (bucket.isEmpty() || key.isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 path format. Bucket and key cannot be empty");
        }
        
        return new S3Path(bucket, key);
    }

    /**
     * Inner class to hold parsed S3 path components
     */
    private static class S3Path {
        final String bucket;
        final String key;
        
        S3Path(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
        }
    }
}
