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

package com.rankforge.server.repository;

import com.rankforge.server.entity.ClanMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ClanMembership entity
 * Author bageshwar.pn
 * Date 2026
 */
@Repository
public interface ClanMembershipRepository extends JpaRepository<ClanMembership, Long> {
    
    /**
     * Find all clan memberships for a user
     */
    List<ClanMembership> findByUserId(Long userId);
    
    /**
     * Find all members of a clan
     */
    List<ClanMembership> findByClanId(Long clanId);
    
    /**
     * Check if a user is already a member of a clan
     */
    boolean existsByClanIdAndUserId(Long clanId, Long userId);
    
    /**
     * Find a specific membership
     */
    Optional<ClanMembership> findByClanIdAndUserId(Long clanId, Long userId);
}
