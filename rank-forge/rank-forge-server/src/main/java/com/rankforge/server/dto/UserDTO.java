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

import com.rankforge.server.entity.User;

/**
 * DTO for User entity
 * Author bageshwar.pn
 * Date 2026
 */
public class UserDTO {
    
    private Long id;
    private String steamId64;
    private String steamId3;
    private String personaName;
    private String avatarUrl;
    private String avatarMediumUrl;
    private String avatarSmallUrl;
    private String profileUrl;
    private Long accountCreated; // Unix timestamp
    private Boolean vacBanned;
    private String country;
    private Long createdAt; // Unix timestamp
    private Long lastLogin; // Unix timestamp
    private Long defaultClanId; // Default clan ID for filtering
    
    public UserDTO() {}
    
    public UserDTO(User user) {
        this.id = user.getId();
        this.steamId64 = user.getSteamId64();
        this.steamId3 = user.getSteamId3();
        this.personaName = user.getPersonaName();
        this.avatarUrl = user.getAvatarUrl();
        this.avatarMediumUrl = user.getAvatarMediumUrl();
        this.avatarSmallUrl = user.getAvatarSmallUrl();
        this.profileUrl = user.getProfileUrl();
        this.accountCreated = user.getAccountCreated() != null ? 
            user.getAccountCreated().getEpochSecond() : null;
        this.vacBanned = user.getVacBanned();
        this.country = user.getCountry();
        this.createdAt = user.getCreatedAt() != null ? 
            user.getCreatedAt().getEpochSecond() : null;
        this.lastLogin = user.getLastLogin() != null ? 
            user.getLastLogin().getEpochSecond() : null;
        this.defaultClanId = user.getDefaultClanId();
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
    
    public Long getAccountCreated() {
        return accountCreated;
    }
    
    public void setAccountCreated(Long accountCreated) {
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
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(Long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Long getDefaultClanId() {
        return defaultClanId;
    }
    
    public void setDefaultClanId(Long defaultClanId) {
        this.defaultClanId = defaultClanId;
    }
}
