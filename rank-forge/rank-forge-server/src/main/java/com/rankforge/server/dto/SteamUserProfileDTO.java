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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * DTO for Steam user profile data from Steam Web API
 * Author bageshwar.pn
 * Date 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamUserProfileDTO {
    
    @JsonProperty("steamid")
    private String steamId64;
    
    @JsonProperty("personaname")
    private String personaName;
    
    @JsonProperty("profileurl")
    private String profileUrl;
    
    @JsonProperty("avatar")
    private String avatarUrl;
    
    @JsonProperty("avatarmedium")
    private String avatarMediumUrl;
    
    @JsonProperty("avatarfull")
    private String avatarFullUrl;
    
    @JsonProperty("personastate")
    private Integer personaState;
    
    @JsonProperty("communityvisibilitystate")
    private Integer communityVisibilityState;
    
    @JsonProperty("profilestate")
    private Integer profileState;
    
    @JsonProperty("lastlogoff")
    private Long lastLogoff;
    
    @JsonProperty("commentpermission")
    private Integer commentPermission;
    
    @JsonProperty("realname")
    private String realName;
    
    @JsonProperty("primaryclanid")
    private String primaryClanId;
    
    @JsonProperty("timecreated")
    private Long timeCreated;
    
    @JsonProperty("gameid")
    private String gameId;
    
    @JsonProperty("gameserverip")
    private String gameServerIp;
    
    @JsonProperty("gameextrainfo")
    private String gameExtraInfo;
    
    @JsonProperty("loccountrycode")
    private String countryCode;
    
    @JsonProperty("locstatecode")
    private String stateCode;
    
    @JsonProperty("loccityid")
    private Integer cityId;
    
    // Getters and setters
    public String getSteamId64() {
        return steamId64;
    }
    
    public void setSteamId64(String steamId64) {
        this.steamId64 = steamId64;
    }
    
    public String getPersonaName() {
        return personaName;
    }
    
    public void setPersonaName(String personaName) {
        this.personaName = personaName;
    }
    
    public String getProfileUrl() {
        return profileUrl;
    }
    
    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
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
    
    public String getAvatarFullUrl() {
        return avatarFullUrl;
    }
    
    public void setAvatarFullUrl(String avatarFullUrl) {
        this.avatarFullUrl = avatarFullUrl;
    }
    
    public Integer getPersonaState() {
        return personaState;
    }
    
    public void setPersonaState(Integer personaState) {
        this.personaState = personaState;
    }
    
    public Integer getCommunityVisibilityState() {
        return communityVisibilityState;
    }
    
    public void setCommunityVisibilityState(Integer communityVisibilityState) {
        this.communityVisibilityState = communityVisibilityState;
    }
    
    public Integer getProfileState() {
        return profileState;
    }
    
    public void setProfileState(Integer profileState) {
        this.profileState = profileState;
    }
    
    public Long getLastLogoff() {
        return lastLogoff;
    }
    
    public void setLastLogoff(Long lastLogoff) {
        this.lastLogoff = lastLogoff;
    }
    
    public Integer getCommentPermission() {
        return commentPermission;
    }
    
    public void setCommentPermission(Integer commentPermission) {
        this.commentPermission = commentPermission;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public String getPrimaryClanId() {
        return primaryClanId;
    }
    
    public void setPrimaryClanId(String primaryClanId) {
        this.primaryClanId = primaryClanId;
    }
    
    public Long getTimeCreated() {
        return timeCreated;
    }
    
    public void setTimeCreated(Long timeCreated) {
        this.timeCreated = timeCreated;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    public String getGameServerIp() {
        return gameServerIp;
    }
    
    public void setGameServerIp(String gameServerIp) {
        this.gameServerIp = gameServerIp;
    }
    
    public String getGameExtraInfo() {
        return gameExtraInfo;
    }
    
    public void setGameExtraInfo(String gameExtraInfo) {
        this.gameExtraInfo = gameExtraInfo;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public String getStateCode() {
        return stateCode;
    }
    
    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }
    
    public Integer getCityId() {
        return cityId;
    }
    
    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }
    
    /**
     * Convert timeCreated (Unix timestamp) to Instant
     */
    public Instant getAccountCreatedInstant() {
        if (timeCreated != null && timeCreated > 0) {
            return Instant.ofEpochSecond(timeCreated);
        }
        return null;
    }
}
