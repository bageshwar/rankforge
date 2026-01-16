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

package com.rankforge.server.dto;

import java.time.Instant;

/**
 * Response DTO for API key regeneration
 * Contains the new key (shown once) and rotation timestamp
 * Author bageshwar.pn
 * Date 2026
 */
public class RegenerateApiKeyResponse {
    
    private String apiKey; // New key shown once
    private Long rotatedAt; // Unix timestamp
    
    public RegenerateApiKeyResponse() {}
    
    public RegenerateApiKeyResponse(String apiKey, Instant rotatedAt) {
        this.apiKey = apiKey;
        this.rotatedAt = rotatedAt != null ? rotatedAt.getEpochSecond() : null;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public Long getRotatedAt() {
        return rotatedAt;
    }
    
    public void setRotatedAt(Long rotatedAt) {
        this.rotatedAt = rotatedAt;
    }
}
