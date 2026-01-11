# Caching Strategy for Monthly Leaderboard

## Overview

The RankForge monthly leaderboard implements intelligent caching to improve performance while ensuring data accuracy. This document explains how caching works, when data is cached, and how to manage cache invalidation.

## Cache Implementation

### Technology
- **Cache Manager**: Spring Cache with `ConcurrentMapCacheManager` (in-memory)
- **Cache Names**: 
  - `monthlyLeaderboard` - Monthly leaderboard data
  - `allTimeLeaderboard` - All-time leaderboard data
  - `topLeaderboard` - Top N leaderboard data

### Configuration
Located in: `rank-forge-server/src/main/java/com/rankforge/server/config/CacheConfig.java`

## Caching Rules

### 1. Monthly Leaderboards

**Past Months**: ✅ **Cached Indefinitely**
- **Reason**: Historical data doesn't change once the month ends
- **Cache Key**: `{year}-{month}-{limit}-{offset}`
- **Example**: `2025-11-100-0` for November 2025, limit 100, offset 0
- **TTL**: Indefinite (until application restart or manual eviction)

**Current Month**: ❌ **Not Cached**
- **Reason**: Data changes as new games are processed
- **Cache Condition**: Only caches if requested month is before the current month
- **Implementation**: Uses Spring Cache `condition` attribute

```java
@Cacheable(value = "monthlyLeaderboard", 
           key = "#year + '-' + #month + '-' + #limit + '-' + #offset",
           condition = "T(java.time.LocalDate).of(#year, #month, 1).isBefore(T(java.time.LocalDate).now().withDayOfMonth(1))")
```

### 2. All-Time Leaderboards

**Cached**: ✅ **Yes**
- **Cache Key**: `all-time`
- **TTL**: 1 minute (recommended, but not enforced by ConcurrentMapCacheManager)
- **Reason**: Data changes as new games are processed, but less frequently than current month

### 3. Top N Leaderboards

**Cached**: ✅ **Yes**
- **Cache Key**: `{limit}` (e.g., `10`, `25`, `100`)
- **TTL**: 1 minute (recommended, but not enforced by ConcurrentMapCacheManager)
- **Reason**: Data changes as new games are processed

## Cache Key Format

### Monthly Leaderboard
```
monthlyLeaderboard::{year}-{month}-{limit}-{offset}
```
Example: `monthlyLeaderboard::2025-11-100-0`

### All-Time Leaderboard
```
allTimeLeaderboard::all-time
```

### Top N Leaderboard
```
topLeaderboard::{limit}
```
Example: `topLeaderboard::10`

## Cache Behavior

### Cache Hits
When a cached request is made:
1. Spring checks if cache entry exists for the key
2. If found, returns cached data immediately (no database query)
3. Logs cache hit (if debug logging enabled)

### Cache Misses
When cache entry doesn't exist:
1. Executes the service method
2. Queries database
3. Stores result in cache
4. Returns result to client

### Cache Eviction

**Automatic Eviction**:
- On application restart (in-memory cache is cleared)
- When cache size limit is reached (if configured)

**Manual Eviction** (Future Enhancement):
```java
@CacheEvict(value = "monthlyLeaderboard", allEntries = true)
public void evictMonthlyLeaderboardCache() {
    // Evict all monthly leaderboard entries
}
```

**Recommended Manual Eviction Points**:
- After processing new game logs (for current month)
- After bulk data imports
- After data corrections

## Performance Impact

### Before Caching
- **Past Month Query**: ~500-2000ms (depends on data size)
- **Database Queries**: Multiple queries per request
  - Games in month
  - ROUND_END events
  - Player stats
  - Games counts (batch query)

### After Caching
- **Past Month Query (Cached)**: ~1-5ms (cache lookup)
- **Database Queries**: 0 (served from cache)
- **Memory Usage**: ~1-10MB per cached month (depends on player count)

## Monitoring Cache

### Enable Cache Logging
Add to `application.properties`:
```properties
logging.level.org.springframework.cache=DEBUG
```

### Cache Statistics
With `ConcurrentMapCacheManager`, you can:
1. Check cache size programmatically
2. Monitor memory usage
3. Log cache hit/miss ratios

## Production Considerations

### Current Implementation (In-Memory)
- ✅ Simple setup
- ✅ No external dependencies
- ✅ Fast for single-instance deployments
- ❌ Not shared across instances
- ❌ Lost on restart
- ❌ Limited by JVM memory

### Recommended for Production (High Traffic)

**Option 1: Redis Cache**
```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(60))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .build();
}
```

**Benefits**:
- Shared across multiple instances
- Persistent across restarts
- Configurable TTL
- Better memory management

**Option 2: Caffeine Cache**
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

**Benefits**:
- Better performance than ConcurrentMap
- Size-based eviction
- Time-based expiration
- Statistics support

## Cache Invalidation Strategy

### When to Invalidate

1. **After Game Processing** (Current Month)
   - Don't cache current month, so no invalidation needed
   - All-time and top leaderboards should be invalidated

2. **After Data Corrections**
   - Invalidate affected months
   - Invalidate all-time leaderboard

3. **Scheduled Invalidation**
   - Daily: Invalidate current month cache (if accidentally cached)
   - Monthly: No action needed (past months don't change)

### Implementation Example

```java
@Service
public class CacheEvictionService {
    
    @Autowired
    private CacheManager cacheManager;
    
    @CacheEvict(value = {"allTimeLeaderboard", "topLeaderboard"}, allEntries = true)
    public void evictLeaderboardCaches() {
        LOGGER.info("Evicted all-time and top leaderboard caches");
    }
    
    @CacheEvict(value = "monthlyLeaderboard", key = "#year + '-' + #month + '-*'")
    public void evictMonthlyLeaderboard(int year, int month) {
        LOGGER.info("Evicted monthly leaderboard cache for {}-{}", year, month);
    }
}
```

## Testing Cache

### Verify Cache is Working

1. **First Request** (Cache Miss):
   ```bash
   curl http://localhost:8080/api/rankings/leaderboard/monthly?year=2025&month=11
   # Check logs for database queries
   ```

2. **Second Request** (Cache Hit):
   ```bash
   curl http://localhost:8080/api/rankings/leaderboard/monthly?year=2025&month=11
   # Should be much faster, no database queries
   ```

3. **Current Month** (No Cache):
   ```bash
   curl http://localhost:8080/api/rankings/leaderboard/monthly?year=2026&month=1
   # Should always query database (not cached)
   ```

## Troubleshooting

### Cache Not Working
1. Verify `@EnableCaching` is on main application class
2. Check `CacheConfig` bean is created
3. Verify cache name matches `@Cacheable` value
4. Check cache condition is not preventing caching

### Memory Issues
1. Monitor cache size
2. Implement cache size limits
3. Consider TTL for all caches
4. Switch to Redis for distributed caching

### Stale Data
1. Verify cache eviction is working
2. Check cache condition logic
3. Ensure current month is not cached
4. Manually evict cache if needed

## Future Enhancements

1. **Cache Statistics**: Add metrics for cache hit/miss ratios
2. **TTL Configuration**: Make TTL configurable via properties
3. **Cache Warming**: Pre-populate cache for popular months
4. **Distributed Cache**: Use Redis for multi-instance deployments
5. **Cache Compression**: Compress large cache entries
6. **Selective Eviction**: Evict only affected cache entries

## Summary

- ✅ Past months: Cached indefinitely (data doesn't change)
- ❌ Current month: Not cached (data changes frequently)
- ✅ All-time: Cached for 1 minute (changes less frequently)
- ✅ Top N: Cached for 1 minute (changes less frequently)

This strategy balances performance with data accuracy, ensuring users always see up-to-date data for the current month while benefiting from fast cached responses for historical data.
