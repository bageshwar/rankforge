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
import com.rankforge.core.interfaces.*;
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

            // Initialize services with batching support
            ObjectMapper objectMapper = new ObjectMapper();

            String jdbcUrl = System.getProperty("jdbcUrl");
            String username = System.getProperty("jdbcUsername");
            String password = System.getProperty("jdbcPassword");
            PersistenceLayer persistenceLayer = new JdbcBasedPersistenceLayer(jdbcUrl, username, password);

            EventStore eventStore = new DBBasedEventStore(persistenceLayer, objectMapper);
            PlayerStatsStore statsRepo = new DBBasedPlayerStatsStore(persistenceLayer, objectMapper);
            RankingAlgorithm rankingAlgo = new EloBasedRankingAlgorithm();
            RankingService rankingService = new RankingServiceImpl(statsRepo, rankingAlgo);

            // Initialize processors
            EventProcessor eventProcessor = new EventProcessorImpl(statsRepo, rankingService);

            // TODO see if there is a better way to latch the listeners other than typecasting
            eventProcessor.addGameEventListener((GameEventListener) eventStore);
            eventProcessor.addGameEventListener((GameEventListener) statsRepo);
            LogParser logParser = new CS2LogParser(objectMapper, eventStore);

            GameRankingSystem rankingSystem = new GameRankingSystem(
                    logParser, eventProcessor, eventStore,
                    Executors.newScheduledThreadPool(2));

            // Start monitoring log file
            String logFile = args.length > 1 ? args[1] : "./data/log.json";
            //rankingSystem.startProcessing(logFile);
            rankingSystem.processNewLogLines(logFile);


        } catch (Exception e) {
            logger.error("Failed to start ranking system", e);
            System.exit(1);
        }
    }
}
