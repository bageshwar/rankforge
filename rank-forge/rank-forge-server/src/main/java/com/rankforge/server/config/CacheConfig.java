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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;

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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    
    /**
     * Configure cache manager with logging
     * Using ConcurrentMapCacheManager for simple in-memory caching
     * For production with high traffic, consider Redis or EhCache
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        return new LoggingCacheManager();
    }
    
    /**
     * Custom CacheManager that logs cache hits and misses
     */
    private static class LoggingCacheManager implements CacheManager {
        private final ConcurrentMapCacheManager delegate;
        
        public LoggingCacheManager() {
            this.delegate = new ConcurrentMapCacheManager();
            this.delegate.setCacheNames(java.util.Arrays.asList(
                "monthlyLeaderboard",
                "allTimeLeaderboard",
                "topLeaderboard"
            ));
            this.delegate.setAllowNullValues(false);
        }
        
        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            if (cache != null) {
                return new LoggingCache(cache);
            }
            return null;
        }
        
        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }
    
    /**
     * Cache wrapper that logs cache operations
     */
    private static class LoggingCache implements Cache {
        private final Cache delegate;
        private static final Logger CACHE_LOGGER = LoggerFactory.getLogger("Cache");
        
        public LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String getName() {
            return delegate.getName();
        }
        
        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }
        
        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            if (value != null) {
                CACHE_LOGGER.info("Cache HIT: cache='{}', key='{}'", getName(), key);
            } else {
                CACHE_LOGGER.debug("Cache MISS: cache='{}', key='{}'", getName(), key);
            }
            return value;
        }
        
        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = delegate.get(key, type);
            if (value != null) {
                CACHE_LOGGER.info("Cache HIT: cache='{}', key='{}'", getName(), key);
            } else {
                CACHE_LOGGER.debug("Cache MISS: cache='{}', key='{}'", getName(), key);
            }
            return value;
        }
        
        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            // This method is called when cache miss occurs and value needs to be loaded
            CACHE_LOGGER.debug("Cache MISS - loading value: cache='{}', key='{}'", getName(), key);
            T value = delegate.get(key, valueLoader);
            if (value != null) {
                CACHE_LOGGER.info("Cache PUT: cache='{}', key='{}'", getName(), key);
            }
            return value;
        }
        
        @Override
        public void put(Object key, Object value) {
            delegate.put(key, value);
            CACHE_LOGGER.info("Cache PUT: cache='{}', key='{}'", getName(), key);
        }
        
        @Override
        public void evict(Object key) {
            delegate.evict(key);
            CACHE_LOGGER.info("Cache EVICT: cache='{}', key='{}'", getName(), key);
        }
        
        @Override
        public void clear() {
            delegate.clear();
            CACHE_LOGGER.info("Cache CLEAR: cache='{}'", getName());
        }
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
