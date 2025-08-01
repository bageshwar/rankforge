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
import com.rankforge.core.interfaces.RankingService;
import com.rankforge.core.models.PlayerRank;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of ranking service that coordinates between storage and ranking algorithm
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class RankingServiceImpl implements RankingService {
    private static final Logger logger = LoggerFactory.getLogger(RankingServiceImpl.class);

    private final PlayerStatsStore statsRepo;
    private final RankingAlgorithm rankingAlgo;

    public RankingServiceImpl(PlayerStatsStore statsRepo, RankingAlgorithm rankingAlgo) {
        this.statsRepo = statsRepo;
        this.rankingAlgo = rankingAlgo;
    }

    @Override
    public double getPlayerRanking(String playerId) {
        // TODO Implement me
        return 0;
    }

    @Override
    public List<PlayerRank> getTopPlayers(int limit) {
        // TODO Implement me
        return List.of();
    }

    @Override
    public void updateRankings(List<PlayerStats> players) {
        for (PlayerStats playerStats : players) {
            playerStats.rank = rankingAlgo.calculateRank(playerStats);
        }
    }

    // Implementation of service methods...
}
