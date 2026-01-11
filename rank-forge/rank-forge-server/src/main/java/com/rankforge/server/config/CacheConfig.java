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

package com.rankforge.server.config;

import com.rankforge.core.interfaces.RankingAlgorithm;
import com.rankforge.pipeline.EloBasedRankingAlgorithm;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for RankForge Server
 * Uses in-memory caching (ConcurrentMapCacheManager) for leaderboard data
 * 
 * Caching Strategy:
 * - Monthly leaderboards for past months: Cached indefinitely (data doesn't change)
 * - Monthly leaderboard for current month: Cached for 5 minutes (data may change as new games are processed)
 * - All-time leaderboards: Cached for 1 minute (data changes as new games are processed)
 * 
 * Cache Keys:
 * - Monthly: "monthly-leaderboard-{year}-{month}-{limit}-{offset}"
 * - All-time: "all-time-leaderboard-{limit}"
 * - Top N: "top-leaderboard-{limit}"
 * 
 * Cache Eviction:
 * - Manual eviction via @CacheEvict when new games are processed (future enhancement)
 * - Automatic eviction based on TTL (if using Redis/EhCache in future)
 * 
 * Author bageshwar.pn
 * Date 2026
 */
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {
    
    /**
     * Configure cache manager
     * Using ConcurrentMapCacheManager for simple in-memory caching
     * For production with high traffic, consider Redis or EhCache
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        // Define cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "monthlyLeaderboard",
            "allTimeLeaderboard",
            "topLeaderboard"
        ));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
    
    /**
     * Provide RankingAlgorithm bean
     * Creates a singleton instance of EloBasedRankingAlgorithm
     */
    @Bean
    public RankingAlgorithm rankingAlgorithm() {
        return new EloBasedRankingAlgorithm();
    }
}
