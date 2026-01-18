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
 * Entity for Clan Memberships
 * Represents a user's membership in a clan
 * Author bageshwar.pn
 * Date 2026
 */
@Entity
@Table(name = "ClanMembership", indexes = {
    @Index(name = "idx_clanmembership_clanid_userid", columnList = "clanId,userId", unique = true),
    @Index(name = "idx_clanmembership_userid", columnList = "userId"),
    @Index(name = "idx_clanmembership_clanid", columnList = "clanId")
})
public class ClanMembership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clan_membership_seq")
    @SequenceGenerator(name = "clan_membership_seq", sequenceName = "clan_membership_seq", allocationSize = 50)
    private Long id;
    
    @Column(name = "clanId", nullable = false)
    private Long clanId; // FK to Clan
    
    @Column(name = "userId", nullable = false)
    private Long userId; // FK to Users
    
    @Column(name = "joinedAt", nullable = false)
    private Instant joinedAt; // When player first played on this server (or when clan was created if retroactive)
    
    // Default constructor
    public ClanMembership() {
        this.joinedAt = Instant.now();
    }
    
    public ClanMembership(Long clanId, Long userId, Instant joinedAt) {
        this.clanId = clanId;
        this.userId = userId;
        this.joinedAt = joinedAt != null ? joinedAt : Instant.now();
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
    
    public Instant getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
