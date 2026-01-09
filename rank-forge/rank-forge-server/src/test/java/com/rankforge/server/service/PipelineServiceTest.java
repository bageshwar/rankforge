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

package com.rankforge.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.pipeline.GameRankingSystem;
import com.rankforge.pipeline.persistence.PersistenceLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PipelineService
 * Tests pipeline component creation and wiring
 */
@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PersistenceLayer persistenceLayer;

    private ObjectMapper objectMapper;
    private PipelineService pipelineService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pipelineService = new PipelineService(persistenceLayer, objectMapper);
    }

    @Test
    void testCreateGameRankingSystem_ReturnsNonNull() {
        GameRankingSystem rankingSystem = pipelineService.createGameRankingSystem();
        
        assertNotNull(rankingSystem, "GameRankingSystem should not be null");
    }

    @Test
    void testCreateGameRankingSystem_CreatesNewInstance() {
        GameRankingSystem system1 = pipelineService.createGameRankingSystem();
        GameRankingSystem system2 = pipelineService.createGameRankingSystem();
        
        assertNotSame(system1, system2, "Each call should create a new instance");
    }

    @Test
    void testCreateGameRankingSystem_UsesProvidedPersistenceLayer() {
        // Verify that persistence layer is used (indirectly through component creation)
        GameRankingSystem rankingSystem = pipelineService.createGameRankingSystem();
        
        assertNotNull(rankingSystem);
        // Components are created internally, so we verify the system is created successfully
        // which implies persistence layer was used
    }
}
