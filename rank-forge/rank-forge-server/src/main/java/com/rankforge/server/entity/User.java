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
 * Entity for authenticated users from Steam
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "Users", indexes = {
    @Index(name = "idx_users_steamid64", columnList = "steamId64", unique = true),
    @Index(name = "idx_users_steamid3", columnList = "steamId3")
})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 50)
    private Long id;
    
    @Column(name = "steamId64", nullable = false, unique = true, length = 20)
    private String steamId64; // 64-bit Steam ID (e.g., "76561198012345678")
    
    @Column(name = "steamId3", nullable = false, length = 255)
    private String steamId3; // Steam ID3 format [U:1:xxx] to link with PlayerStats
    
    @Column(name = "personaName", nullable = false, length = 255)
    private String personaName; // Steam display name
    
    @Column(name = "avatarUrl", length = 500)
    private String avatarUrl; // Full avatar URL
    
    @Column(name = "avatarMediumUrl", length = 500)
    private String avatarMediumUrl; // Medium avatar URL
    
    @Column(name = "avatarSmallUrl", length = 500)
    private String avatarSmallUrl; // Small avatar URL
    
    @Column(name = "profileUrl", length = 500)
    private String profileUrl; // Steam profile URL
    
    @Column(name = "accountCreated")
    private Instant accountCreated; // Steam account creation date
    
    @Column(name = "vacBanned", nullable = false)
    private Boolean vacBanned = false; // VAC ban status
    
    @Column(name = "country", length = 10)
    private String country; // Country code (e.g., "US", "IN")
    
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;
    
    @Column(name = "lastLogin", nullable = false)
    private Instant lastLogin;
    
    @Column(name = "defaultClanId", nullable = true)
    private Long defaultClanId; // Default clan for filtering rankings/games
    
    // Default constructor
    public User() {
        this.createdAt = Instant.now();
        this.lastLogin = Instant.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSteamId64() {
        return steamId64;
    }
    
    public void setSteamId64(String steamId64) {
        this.steamId64 = steamId64;
    }
    
    public String getSteamId3() {
        return steamId3;
    }
    
    public void setSteamId3(String steamId3) {
        this.steamId3 = steamId3;
    }
    
    public String getPersonaName() {
        return personaName;
    }
    
    public void setPersonaName(String personaName) {
        this.personaName = personaName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public String getAvatarMediumUrl() {
        return avatarMediumUrl;
    }
    
    public void setAvatarMediumUrl(String avatarMediumUrl) {
        this.avatarMediumUrl = avatarMediumUrl;
    }
    
    public String getAvatarSmallUrl() {
        return avatarSmallUrl;
    }
    
    public void setAvatarSmallUrl(String avatarSmallUrl) {
        this.avatarSmallUrl = avatarSmallUrl;
    }
    
    public String getProfileUrl() {
        return profileUrl;
    }
    
    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }
    
    public Instant getAccountCreated() {
        return accountCreated;
    }
    
    public void setAccountCreated(Instant accountCreated) {
        this.accountCreated = accountCreated;
    }
    
    public Boolean getVacBanned() {
        return vacBanned;
    }
    
    public void setVacBanned(Boolean vacBanned) {
        this.vacBanned = vacBanned;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Long getDefaultClanId() {
        return defaultClanId;
    }
    
    public void setDefaultClanId(Long defaultClanId) {
        this.defaultClanId = defaultClanId;
    }
}
