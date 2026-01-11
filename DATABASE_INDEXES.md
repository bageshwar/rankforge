# Database Indexes for Monthly Leaderboard

## Overview

This document describes the database indexes required for optimal performance of the monthly leaderboard feature. These indexes ensure efficient queries for date range filtering, player lookups, and game event aggregation.

## Required Indexes

### 1. PlayerStatsEntity Indexes

#### Index: `idx_playerstats_gametimestamp`
**Purpose**: Fast lookup of player stats within a month range

**Columns**: `gameTimestamp`

**Query Used In**:
- `findStatsByMonthRange()` - Get all player stats in a month
- `findLatestStatsBeforeDate()` - Get baseline stats before month

**SQL**:
```sql
CREATE INDEX idx_playerstats_gametimestamp 
ON PlayerStats (gameTimestamp);
```

**Verification**:
```sql
-- Check if index exists
SELECT name FROM sys.indexes 
WHERE object_id = OBJECT_ID('PlayerStats') 
AND name = 'idx_playerstats_gametimestamp';
```

#### Index: `idx_playerstats_playerid_gametimestamp`
**Purpose**: Fast lookup of player stats by player ID and timestamp

**Columns**: `playerId`, `gameTimestamp`

**Query Used In**:
- `findLatestStatsBeforeDate()` - Get latest stats for a player before a date
- `countDistinctGamesByPlayerIdInMonth()` - Count games for a player in a month

**SQL**:
```sql
CREATE INDEX idx_playerstats_playerid_gametimestamp 
ON PlayerStats (playerId, gameTimestamp);
```

**Verification**:
```sql
SELECT name FROM sys.indexes 
WHERE object_id = OBJECT_ID('PlayerStats') 
AND name = 'idx_playerstats_playerid_gametimestamp';
```

#### Index: `idx_playerstats_playerid_gametimestamp_distinct`
**Purpose**: Optimize COUNT(DISTINCT gameTimestamp) queries

**Columns**: `playerId`, `gameTimestamp`

**Query Used In**:
- `countDistinctGamesByPlayerIdsInMonth()` - Batch count games for multiple players

**Note**: The composite index above should cover this, but ensure it's optimized for DISTINCT operations.

### 2. GameEntity Indexes

#### Index: `idx_game_gameovertimestamp`
**Purpose**: Fast lookup of games within a month range

**Columns**: `gameOverTimestamp`

**Query Used In**:
- `findGamesByMonthRange()` - Get all games in a month
- `calculateTotalRoundsInMonth()` - Calculate total rounds in a month

**SQL**:
```sql
CREATE INDEX idx_game_gameovertimestamp 
ON Game (gameOverTimestamp);
```

**Verification**:
```sql
SELECT name FROM sys.indexes 
WHERE object_id = OBJECT_ID('Game') 
AND name = 'idx_game_gameovertimestamp';
```

### 3. GameEventEntity Indexes

#### Index: `idx_gameevent_gameid_eventtype`
**Purpose**: Fast lookup of ROUND_END events for specific games

**Columns**: `gameId`, `gameEventType`

**Query Used In**:
- `findRoundEndEventsByGameIds()` - Get ROUND_END events for multiple games

**SQL**:
```sql
CREATE INDEX idx_gameevent_gameid_eventtype 
ON GameEvent (gameId, gameEventType);
```

**Note**: This is a composite index that covers both the WHERE clause (gameId IN :gameIds) and the discriminator (gameEventType = 'ROUND_END').

**Verification**:
```sql
SELECT name FROM sys.indexes 
WHERE object_id = OBJECT_ID('GameEvent') 
AND name = 'idx_gameevent_gameid_eventtype';
```

#### Index: `idx_gameevent_timestamp`
**Purpose**: Fast ordering and filtering by timestamp

**Columns**: `timestamp`

**Query Used In**:
- Event ordering in various queries
- Potential future timestamp-based queries

**SQL**:
```sql
CREATE INDEX idx_gameevent_timestamp 
ON GameEvent (timestamp);
```

## Index Creation Script

### Complete Index Creation Script

```sql
-- PlayerStats indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_playerstats_gametimestamp' AND object_id = OBJECT_ID('PlayerStats'))
BEGIN
    CREATE INDEX idx_playerstats_gametimestamp 
    ON PlayerStats (gameTimestamp);
    PRINT 'Created index: idx_playerstats_gametimestamp';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_playerstats_playerid_gametimestamp' AND object_id = OBJECT_ID('PlayerStats'))
BEGIN
    CREATE INDEX idx_playerstats_playerid_gametimestamp 
    ON PlayerStats (playerId, gameTimestamp);
    PRINT 'Created index: idx_playerstats_playerid_gametimestamp';
END
GO

-- Game indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_game_gameovertimestamp' AND object_id = OBJECT_ID('Game'))
BEGIN
    CREATE INDEX idx_game_gameovertimestamp 
    ON Game (gameOverTimestamp);
    PRINT 'Created index: idx_game_gameovertimestamp';
END
GO

-- GameEvent indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_gameevent_gameid_eventtype' AND object_id = OBJECT_ID('GameEvent'))
BEGIN
    CREATE INDEX idx_gameevent_gameid_eventtype 
    ON GameEvent (gameId, gameEventType);
    PRINT 'Created index: idx_gameevent_gameid_eventtype';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_gameevent_timestamp' AND object_id = OBJECT_ID('GameEvent'))
BEGIN
    CREATE INDEX idx_gameevent_timestamp 
    ON GameEvent (timestamp);
    PRINT 'Created index: idx_gameevent_timestamp';
END
GO
```

## Index Performance Analysis

### Query Performance Impact

#### Before Indexes
- `findStatsByMonthRange()`: Full table scan, ~5-30 seconds for large datasets
- `findRoundEndEventsByGameIds()`: Full table scan, ~10-60 seconds for large datasets
- `countDistinctGamesByPlayerIdsInMonth()`: Multiple full table scans

#### After Indexes
- `findStatsByMonthRange()`: Index seek, ~50-500ms
- `findRoundEndEventsByGameIds()`: Index seek, ~100-1000ms
- `countDistinctGamesByPlayerIdsInMonth()`: Index seek, ~200-2000ms

### Expected Performance Improvement
- **10-100x faster** for date range queries
- **5-50x faster** for player-specific queries
- **Significant reduction** in database CPU and I/O

## Index Maintenance

### Statistics Update
SQL Server automatically updates index statistics, but you can manually update:

```sql
UPDATE STATISTICS PlayerStats;
UPDATE STATISTICS Game;
UPDATE STATISTICS GameEvent;
```

### Index Fragmentation
Monitor and rebuild indexes periodically:

```sql
-- Check fragmentation
SELECT 
    OBJECT_NAME(object_id) AS TableName,
    name AS IndexName,
    avg_fragmentation_in_percent
FROM sys.dm_db_index_physical_stats(
    DB_ID(), 
    OBJECT_ID('PlayerStats'), 
    NULL, 
    NULL, 
    'DETAILED'
)
WHERE avg_fragmentation_in_percent > 30;

-- Rebuild if fragmentation > 30%
ALTER INDEX idx_playerstats_gametimestamp 
ON PlayerStats REBUILD;
```

## Verification Queries

### Check All Indexes Exist

```sql
SELECT 
    t.name AS TableName,
    i.name AS IndexName,
    i.type_desc AS IndexType,
    STRING_AGG(c.name, ', ') WITHIN GROUP (ORDER BY ic.key_ordinal) AS Columns
FROM sys.tables t
INNER JOIN sys.indexes i ON t.object_id = i.object_id
INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE t.name IN ('PlayerStats', 'Game', 'GameEvent')
    AND i.name LIKE 'idx_%'
GROUP BY t.name, i.name, i.type_desc
ORDER BY t.name, i.name;
```

### Check Index Usage

```sql
SELECT 
    OBJECT_NAME(s.object_id) AS TableName,
    i.name AS IndexName,
    s.user_seeks,
    s.user_scans,
    s.user_lookups,
    s.user_updates
FROM sys.dm_db_index_usage_stats s
INNER JOIN sys.indexes i ON s.object_id = i.object_id AND s.index_id = i.index_id
WHERE OBJECT_NAME(s.object_id) IN ('PlayerStats', 'Game', 'GameEvent')
    AND i.name LIKE 'idx_%'
ORDER BY s.user_seeks + s.user_scans DESC;
```

## Index Recommendations by Query Type

### Date Range Queries
- ✅ `idx_playerstats_gametimestamp` - Essential
- ✅ `idx_game_gameovertimestamp` - Essential

### Player-Specific Queries
- ✅ `idx_playerstats_playerid_gametimestamp` - Essential

### Game Event Queries
- ✅ `idx_gameevent_gameid_eventtype` - Essential
- ✅ `idx_gameevent_timestamp` - Recommended for ordering

## Notes

1. **Composite Indexes**: The order of columns matters. Put the most selective column first (usually `playerId` or `gameId`).

2. **Covering Indexes**: Consider adding frequently selected columns to indexes to create "covering indexes" that avoid table lookups.

3. **Index Size**: Monitor index size and ensure sufficient disk space. Indexes typically use 10-30% of table size.

4. **Write Performance**: More indexes = slower writes. Balance read performance with write performance.

5. **Maintenance Windows**: Rebuild indexes during low-traffic periods to avoid blocking queries.

## Migration

### Adding Indexes to Existing Database

1. **Test Environment First**: Always test index creation in a test environment
2. **Off-Peak Hours**: Create indexes during low-traffic periods
3. **Monitor Performance**: Watch for blocking queries during index creation
4. **Verify**: Run verification queries to ensure indexes are created

### Rollback Plan

If indexes cause issues:
```sql
DROP INDEX idx_playerstats_gametimestamp ON PlayerStats;
DROP INDEX idx_playerstats_playerid_gametimestamp ON PlayerStats;
-- etc.
```

## Summary

These indexes are **critical** for monthly leaderboard performance. Without them, queries will be slow and may timeout. Ensure all indexes are created before deploying the monthly leaderboard feature to production.
