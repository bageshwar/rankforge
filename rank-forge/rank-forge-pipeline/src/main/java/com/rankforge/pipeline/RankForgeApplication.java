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

package com.rankforge.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankforge.core.interfaces.EventProcessor;
import com.rankforge.core.interfaces.LogParser;
import com.rankforge.core.interfaces.RankingAlgorithm;
import com.rankforge.core.interfaces.RankingService;
import com.rankforge.core.stores.EventStore;
import com.rankforge.core.stores.PlayerStatsStore;
import com.rankforge.pipeline.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * The Main Application
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class RankForgeApplication {
    private static final Logger logger = LoggerFactory.getLogger(RankForgeApplication.class);

    public static void main(String[] args) {
        try {
            // Initialize components
            String dataDir = args.length > 0 ? args[0] : "./data";
            Files.createDirectories(Paths.get(dataDir));

            // Initialize services
            //EventStore eventStore = new FileBasedEventStore(dataDir);
            ObjectMapper objectMapper = new ObjectMapper();
            //SQLiteBasedPersistenceLayer persistenceLayer = new SQLiteBasedPersistenceLayer(dataDir);
            PersistenceLayer persistenceLayer = new FirestoreBasedPersistenceLayer("rankforge", "firestore-db");
            EventStore eventStore = new DBBasedEventStore(persistenceLayer, objectMapper);
            PlayerStatsStore statsRepo = new DBBasedPlayerStatsStore(persistenceLayer, objectMapper);
            RankingAlgorithm rankingAlgo = new EloBasedRankingAlgorithm();
            RankingService rankingService = new RankingServiceImpl(statsRepo, rankingAlgo);

            // Initialize processors
            EventProcessor eventProcessor = new EventProcessorImpl(statsRepo, rankingService);
            LogParser logParser = new CS2LogParser(objectMapper);

            // Start the system
            GameRankingSystem rankingSystem = new GameRankingSystem(
                    logParser, eventProcessor, eventStore, rankingService, Executors.newScheduledThreadPool(1));

            // Start monitoring log file
            String logFile = args.length > 1 ? args[1] : "./data/log.json";
            //rankingSystem.startProcessing(logFile);
            rankingSystem.processNewLogLines(logFile);

            // Start HTTP server for API
            //new RankingApiServer(rankingService).start(8080);

        } catch (Exception e) {
            logger.error("Failed to start ranking system", e);
            System.exit(1);
        }
    }
}
