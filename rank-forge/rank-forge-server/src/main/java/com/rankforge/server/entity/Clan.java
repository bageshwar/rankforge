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

package com.rankforge.server.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity for Clans
 * Represents a clan that has claimed an appServerId
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "Clan", indexes = {
    @Index(name = "idx_clan_appserverid", columnList = "appServerId", unique = true),
    @Index(name = "idx_clan_adminuserid", columnList = "adminUserId")
})
public class Clan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clan_seq")
    @SequenceGenerator(name = "clan_seq", sequenceName = "clan_seq", allocationSize = 50)
    private Long id;
    
    @Column(name = "appServerId", nullable = false, unique = true)
    private Long appServerId; // App Server ID from GameEntity.appServerId, claimed by clan admin
    
    @Column(name = "name", length = 255)
    private String name; // Optional display name for the clan
    
    @Column(name = "telegramChannelId", length = 255)
    private String telegramChannelId; // Optional Telegram channel ID for notifications
    
    @Column(name = "adminUserId", nullable = false)
    private Long adminUserId; // User who created/owns the clan (FK to Users)
    
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;
    
    // Default constructor
    public Clan() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public Clan(Long appServerId, String name, String telegramChannelId, Long adminUserId) {
        this.appServerId = appServerId;
        this.name = name;
        this.telegramChannelId = telegramChannelId;
        this.adminUserId = adminUserId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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
        this.updatedAt = Instant.now();
    }
    
    public String getTelegramChannelId() {
        return telegramChannelId;
    }
    
    public void setTelegramChannelId(String telegramChannelId) {
        this.telegramChannelId = telegramChannelId;
        this.updatedAt = Instant.now();
    }
    
    public Long getAdminUserId() {
        return adminUserId;
    }
    
    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
        this.updatedAt = Instant.now();
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
