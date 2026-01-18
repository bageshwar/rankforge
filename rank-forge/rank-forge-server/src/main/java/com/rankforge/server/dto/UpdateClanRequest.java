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

import jakarta.validation.constraints.Size;

/**
 * DTO for updating clan information
 * Author bageshwar.pn
 * Date 2026
 */
public class UpdateClanRequest {
    
    @Size(max = 255, message = "Clan name must not exceed 255 characters")
    private String name;
    
    @Size(max = 255, message = "Telegram channel ID must not exceed 255 characters")
    private String telegramChannelId;
    
    public UpdateClanRequest() {
    }
    
    public UpdateClanRequest(String name, String telegramChannelId) {
        this.name = name;
        this.telegramChannelId = telegramChannelId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTelegramChannelId() {
        return telegramChannelId;
    }
    
    public void setTelegramChannelId(String telegramChannelId) {
        this.telegramChannelId = telegramChannelId;
    }
}
