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

import com.rankforge.server.entity.Clan;

/**
 * DTO for Clan entity
 * Author bageshwar.pn
 * Date 2026
 */
public class ClanDTO {
    
    private Long id;
    private Long appServerId;
    private String name;
    private String telegramChannelId;
    private Long adminUserId;
    private Long createdAt; // Unix timestamp
    private Long updatedAt; // Unix timestamp
    
    public ClanDTO() {}
    
    public ClanDTO(Clan clan) {
        this.id = clan.getId();
        this.appServerId = clan.getAppServerId();
        this.name = clan.getName();
        this.telegramChannelId = clan.getTelegramChannelId();
        this.adminUserId = clan.getAdminUserId();
        this.createdAt = clan.getCreatedAt() != null ? 
            clan.getCreatedAt().getEpochSecond() : null;
        this.updatedAt = clan.getUpdatedAt() != null ? 
            clan.getUpdatedAt().getEpochSecond() : null;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAppServerId() {
        return appServerId;
    }
    
    public void setAppServerId(Long appServerId) {
        this.appServerId = appServerId;
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
    
    public Long getAdminUserId() {
        return adminUserId;
    }
    
    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
