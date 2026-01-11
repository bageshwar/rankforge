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
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Service factory for creating pipeline components
 * Wires pipeline components using server's JPA configuration
 * Author bageshwar.pn
 * Date 2024
 */
@Service
public class PipelineService {
    
    private static final Logger logger = LoggerFactory.getLogger(PipelineService.class);
    
    private final GameEventRepository gameEventRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final AccoladeRepository accoladeRepository;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper;
    private final EventProcessingContext eventProcessingContext;
    
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;
    
    @Value("${rankforge.persistence.type:jpa}")
    private String persistenceType;

    @Autowired
    public PipelineService(GameEventRepository gameEventRepository,
                          PlayerStatsRepository playerStatsRepository,
                          AccoladeRepository accoladeRepository,
                          GameRepository gameRepository,
                          ObjectMapper objectMapper,
                          EventProcessingContext eventProcessingContext) {
        this.gameEventRepository = gameEventRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.accoladeRepository = accoladeRepository;
        this.gameRepository = gameRepository;
        this.objectMapper = objectMapper;
        this.eventProcessingContext = eventProcessingContext;
    }

    /**
     * Creates a new GameRankingSystem instance with all required components
     * Components are wired together using the server's JPA repositories
     * 
     * @return Configured GameRankingSystem instance
     */
    public GameRankingSystem createGameRankingSystem() {
        logger.debug("Creating pipeline components with server JPA configuration");
        
        // Create a new EntityManager for this processing job
        // This is needed because @PersistenceContext provides a thread-bound proxy
        // that doesn't work across different thread pools (like ForkJoinPool)
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        logger.debug("Created EntityManager for processing job: {}", entityManager != null);
        
        // Create stores using JPA repositories and shared context
        JpaEventStore jpaEventStore = new JpaEventStore(gameEventRepository, accoladeRepository,
                gameRepository, objectMapper, eventProcessingContext);
        // Inject EntityManager for direct persistence operations
        jpaEventStore.setEntityManager(entityManager);
        EventStore eventStore = jpaEventStore;
        
        PlayerStatsStore statsRepo = new JpaPlayerStatsStore(playerStatsRepository);
        AccoladeStore accoladeStore = new AccoladeStore(accoladeRepository, eventProcessingContext);
        
        // Create ranking algorithm and service
        RankingAlgorithm rankingAlgo = new EloBasedRankingAlgorithm();
        RankingService rankingService = new RankingServiceImpl(statsRepo, rankingAlgo);
        
        // Create event processor with shared context for direct entity reference linking
        EventProcessor eventProcessor = new EventProcessorImpl(statsRepo, rankingService, 
                eventProcessingContext, gameRepository);
        
        // Wire event listeners
        eventProcessor.addGameEventListener((GameEventListener) eventStore);
        eventProcessor.addGameEventListener((GameEventListener) statsRepo);
        
        // Create log parser with accolade store
        LogParser logParser = new CS2LogParser(objectMapper, eventStore, accoladeStore);
        
        // Create scheduler for async processing
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        // Create and return game ranking system
        GameRankingSystem rankingSystem = new GameRankingSystem(
                logParser, eventProcessor, eventStore, scheduler);
        
        logger.debug("Successfully created GameRankingSystem with all components");
        return rankingSystem;
    }
}
