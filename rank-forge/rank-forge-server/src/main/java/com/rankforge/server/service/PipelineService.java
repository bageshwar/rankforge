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
import com.rankforge.core.interfaces.*;
import com.rankforge.core.stores.EventStore;
import com.rankforge.core.stores.PlayerStatsStore;
import com.rankforge.pipeline.*;
import com.rankforge.pipeline.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Service factory for creating pipeline components
 * Wires pipeline components using server's JDBC configuration
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class PipelineService {
    
    private static final Logger logger = LoggerFactory.getLogger(PipelineService.class);
    
    private final PersistenceLayer persistenceLayer;
    private final ObjectMapper objectMapper;

    @Autowired
    public PipelineService(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        this.persistenceLayer = persistenceLayer;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new GameRankingSystem instance with all required components
     * Components are wired together using the server's persistence layer
     * 
     * @return Configured GameRankingSystem instance
     */
    public GameRankingSystem createGameRankingSystem() {
        logger.debug("Creating pipeline components with server JDBC configuration");
        
        // Create stores using server's persistence layer
        EventStore eventStore = new DBBasedEventStore(persistenceLayer, objectMapper);
        PlayerStatsStore statsRepo = new DBBasedPlayerStatsStore(persistenceLayer, objectMapper);
        
        // Create ranking algorithm and service
        RankingAlgorithm rankingAlgo = new EloBasedRankingAlgorithm();
        RankingService rankingService = new RankingServiceImpl(statsRepo, rankingAlgo);
        
        // Create event processor
        EventProcessor eventProcessor = new EventProcessorImpl(statsRepo, rankingService);
        
        // Wire event listeners
        eventProcessor.addGameEventListener((GameEventListener) eventStore);
        eventProcessor.addGameEventListener((GameEventListener) statsRepo);
        
        // Create log parser
        LogParser logParser = new CS2LogParser(objectMapper, eventStore);
        
        // Create scheduler for async processing
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        // Create and return game ranking system
        GameRankingSystem rankingSystem = new GameRankingSystem(
                logParser, eventProcessor, eventStore, scheduler);
        
        logger.debug("Successfully created GameRankingSystem with all components");
        return rankingSystem;
    }
}
