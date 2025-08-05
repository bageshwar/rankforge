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

import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service to seed the database with initial player data for testing purposes.
 * Only runs if enabled via configuration and if database is empty.
 * 
 * Author bageshwar.pn
 * Date [Current Date]
 */
@Service
public class DataSeedingService implements CommandLineRunner {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeedingService.class);
    
    private final PlayerStatsStore playerStatsStore;
    
    @Value("${rankforge.data.seed.enabled:false}")
    private boolean seedingEnabled;
    
    @Autowired
    public DataSeedingService(PlayerStatsStore playerStatsStore) {
        this.playerStatsStore = playerStatsStore;
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (!seedingEnabled) {
            LOGGER.info("Data seeding is disabled");
            return;
        }
        
        LOGGER.info("Checking if database needs seeding...");
        
        // Check if data already exists
        if (hasExistingData()) {
            LOGGER.info("Database already contains data, skipping seeding");
            return;
        }
        
        LOGGER.info("Database is empty, seeding with sample data...");
        seedSampleData();
        LOGGER.info("Database seeding completed");
    }
    
    /**
     * Checks if the database already contains player data
     */
    private boolean hasExistingData() {
        // Try to find any existing player - if found, assume database is populated
        Optional<PlayerStats> anyPlayer = playerStatsStore.getPlayerStats("STEAM_1000001");
        return anyPlayer.isPresent();
    }
    
    /**
     * Seeds the database with sample player data
     */
    private void seedSampleData() {
        String[] playerData = {
            "STEAM_1000001,ProGamer_2024,185,75,52,47,138,8,15420.5,1",
            "STEAM_1000002,ShadowSniper,172,82,45,43,125,6,14250.8,2", 
            "STEAM_1000003,FragMaster,165,85,48,41,122,7,13890.2,3",
            "STEAM_1000004,HeadshotKing,198,70,38,59,145,12,16780.3,4",
            "STEAM_1000005,ClutchLord,156,88,55,35,119,15,13560.7,5",
            "STEAM_1000006,AWPer_Elite,189,78,32,51,140,9,15920.1,6",
            "STEAM_1000007,SprayControl,149,92,48,33,115,5,12840.9,7",
            "STEAM_1000008,TacticalNinja,167,85,62,42,127,11,14125.6,8",
            "STEAM_1000009,BombDefuser,134,95,58,29,108,4,11950.3,9",
            "STEAM_1000010,TeamLeader,178,80,67,45,135,8,14680.2,10",
            "STEAM_1000011,QuickScope,142,98,35,31,112,6,12350.8,11",
            "STEAM_1000012,FlashMaster,161,87,52,38,123,7,13420.5,12",
            "STEAM_1000013,SmokeGuru,125,101,64,28,105,3,11200.4,13",
            "STEAM_1000014,EntryFragger,195,72,41,56,142,10,16120.7,14",
            "STEAM_1000015,SupportPlayer,118,89,71,26,102,4,10890.1,15"
        };
        
        for (String data : playerData) {
            try {
                String[] parts = data.split(",");
                if (parts.length != 10) {
                    LOGGER.warn("Invalid data format: {}", data);
                    continue;
                }
                
                PlayerStats stats = new PlayerStats();
                stats.setPlayerId(parts[0]);
                stats.setLastSeenNickname(parts[1]);
                stats.setKills(Integer.parseInt(parts[2]));
                stats.setDeaths(Integer.parseInt(parts[3]));
                stats.setAssists(Integer.parseInt(parts[4]));
                stats.setHeadshotKills(Integer.parseInt(parts[5]));
                stats.setRoundsPlayed(Integer.parseInt(parts[6]));
                stats.setClutchesWon(Integer.parseInt(parts[7]));
                stats.setDamageDealt(Double.parseDouble(parts[8]));
                stats.setRank(Integer.parseInt(parts[9])); // Set the rank field
                stats.setLastUpdated(Instant.now());
                
                playerStatsStore.store(stats, false);
                LOGGER.debug("Seeded player: {} (Rank: {})", stats.getLastSeenNickname(), stats.getRank());
                
            } catch (Exception e) {
                LOGGER.error("Failed to seed player data: {}", data, e);
            }
        }
    }
}