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

import com.rankforge.server.entity.ClanMembership;

/**
 * DTO for ClanMembership entity
 * Author bageshwar.pn
 * Date 2026
 */
public class ClanMembershipDTO {
    
    private Long id;
    private Long clanId;
    private Long userId;
    private Long joinedAt; // Unix timestamp
    
    public ClanMembershipDTO() {}
    
    public ClanMembershipDTO(ClanMembership membership) {
        this.id = membership.getId();
        this.clanId = membership.getClanId();
        this.userId = membership.getUserId();
        this.joinedAt = membership.getJoinedAt() != null ? 
            membership.getJoinedAt().getEpochSecond() : null;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getClanId() {
        return clanId;
    }
    
    public void setClanId(Long clanId) {
        this.clanId = clanId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(Long joinedAt) {
        this.joinedAt = joinedAt;
    }
}
