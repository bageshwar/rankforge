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

package com.rankforge.pipeline.persistence.repository;

import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Accolade entities.
 * Note: Post-hoc linking methods have been removed - entity references are now set directly
 * via EventProcessingContext before persistence.
 * Author bageshwar.pn
 * Date 2026
 */
@Repository
public interface AccoladeRepository extends JpaRepository<AccoladeEntity, Long> {
    
    /**
     * Find accolades by game ID
     */
    @Query("SELECT a FROM AccoladeEntity a WHERE a.game.id = :gameId ORDER BY a.position ASC")
    List<AccoladeEntity> findByGameId(@Param("gameId") Long gameId);
}
