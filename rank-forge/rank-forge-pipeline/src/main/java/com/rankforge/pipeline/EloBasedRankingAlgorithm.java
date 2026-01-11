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

import com.rankforge.core.interfaces.RankingAlgorithm;
import com.rankforge.core.models.PlayerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ranking algorithm based on ELO rating system
 * Stateless class that can be safely shared across requests
 * Note: Bean is configured in rank-forge-server module's CacheConfig
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class EloBasedRankingAlgorithm implements RankingAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(EloBasedRankingAlgorithm.class);
    private static final double K_FACTOR = 32.0;
    private static final double HEADSHOT_BONUS = 0.1;

    @Override
    public int calculateRank(PlayerStats stats) {
        double kdr = calculateKDR(stats);
        double headshotRatio = calculateHeadshotRatio(stats);
        double clutchFactor = calculateClutchFactor(stats);

        // Calculate base rating
        double rating = 1000 +
                (kdr * 200) +
                (headshotRatio * 100) +
                (clutchFactor * 150);

        // Alt formula
        //rating = (kdr * K_FACTOR) + (headshotRatio * HEADSHOT_BONUS) * 1000;

        int intRating = (int) rating;
        logger.debug("Calculated rank for player: {} = {}", stats.getPlayerId(), intRating);
        return intRating;
    }

    private double calculateClutchFactor(PlayerStats stats) {
        // TODO implement calculateClutchFactor
        return 0;
    }

    private double calculateHeadshotRatio(PlayerStats stats) {
        return (double) stats.getHeadshotKills() / (stats.getKills() + 1);
    }

    private double calculateKDR(PlayerStats stats) {
        return (double) stats.getKills() / (stats.getDeaths() + 1);
    }

    // Helper methods...
}
