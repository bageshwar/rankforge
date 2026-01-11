# Coordinate Parsing Implementation - Complete

## Summary

Successfully implemented coordinate parsing for KILL and ATTACK events using a JSON column approach for database storage.

## What Was Implemented

### 1. Domain Model (GameActionEvent)
- Added 6 coordinate fields: `player1X`, `player1Y`, `player1Z`, `player2X`, `player2Y`, `player2Z`
- All fields are `Integer` (nullable) to handle missing coordinates
- Available on all `GameActionEvent` subclasses (KillEvent, AttackEvent, AssistEvent)

### 2. Log Parsing (CS2LogParser)
- Updated `KILL_PATTERN` to capture coordinates for both killer and victim
- Updated `parseKillEvent()` to extract and store coordinates
- Updated `parseAttackEvent()` to extract and store coordinates (pattern already captured them)
- Added `parseCoordinate()` helper method for safe integer parsing

### 3. Database Storage (JSON Column)
- Added `coordinates` column to `GameEventEntity` as `NVARCHAR(MAX)`
- Column stores JSON string: `{"player1": {"x": 100, "y": 200, "z": 50}, "player2": {"x": 150, "y": 250, "z": 60}}`
- Only populated for KILL and ATTACK events (NULL for others)
- Implemented `serializeCoordinates()` method in `JpaEventStore` to convert domain coordinates to JSON

### 4. Unprocessed Events Logger
- Created `UnprocessedEventsLoggerTest` to identify unprocessed log events
- Helps discover which events we're not currently parsing

## Files Modified

1. `rank-forge-core/src/main/java/com/rankforge/core/events/GameActionEvent.java`
   - Added coordinate fields and getters/setters

2. `rank-forge-pipeline/src/main/java/com/rankforge/pipeline/CS2LogParser.java`
   - Updated KILL_PATTERN regex
   - Updated parseKillEvent() and parseAttackEvent() methods

3. `rank-forge-pipeline/src/main/java/com/rankforge/pipeline/persistence/entity/GameEventEntity.java`
   - Added `coordinates` JSON column

4. `rank-forge-pipeline/src/main/java/com/rankforge/pipeline/persistence/JpaEventStore.java`
   - Added `serializeCoordinates()` method
   - Updated `convertToEntity()` to serialize coordinates for KILL and ATTACK events

5. `rank-forge-pipeline/src/test/java/com/rankforge/pipeline/UnprocessedEventsLoggerTest.java`
   - New test class for identifying unprocessed events

## Database Migration

If using `ddl-auto=validate` (production), you'll need to add the column manually:

```sql
ALTER TABLE GameEvent ADD coordinates NVARCHAR(MAX) NULL;
```

If using `ddl-auto=update` (development), Hibernate will add it automatically.

## Usage Examples

### Accessing Coordinates in Java
```java
KillEvent killEvent = ...;
Integer killerX = killEvent.getPlayer1X();
Integer killerY = killEvent.getPlayer1Y();
Integer killerZ = killEvent.getPlayer1Z();
Integer victimX = killEvent.getPlayer2X();
Integer victimY = killEvent.getPlayer2Y();
Integer victimZ = killEvent.getPlayer2Z();
```

### Querying Coordinates in SQL
```sql
-- Get all kills with coordinates
SELECT 
    id,
    at,
    player1,
    player2,
    JSON_VALUE(coordinates, '$.player1.x') as player1X,
    JSON_VALUE(coordinates, '$.player1.y') as player1Y,
    JSON_VALUE(coordinates, '$.player1.z') as player1Z,
    JSON_VALUE(coordinates, '$.player2.x') as player2X,
    JSON_VALUE(coordinates, '$.player2.y') as player2Y,
    JSON_VALUE(coordinates, '$.player2.z') as player2Z
FROM GameEvent
WHERE gameEventType = 'KILL'
  AND coordinates IS NOT NULL;

-- Find kills where killer was at specific X coordinate
SELECT *
FROM GameEvent
WHERE gameEventType = 'KILL'
  AND JSON_VALUE(coordinates, '$.player1.x') > 1000;
```

## Benefits of JSON Column Approach

1. **Single Column**: Only 1 column instead of 6, reducing table width
2. **Selective Population**: Only KILL and ATTACK events have coordinates (NULL for others)
3. **SQL Server Native Support**: Can query and index JSON fields directly
4. **Flexible**: Easy to extend with additional coordinate data (e.g., angle, velocity)
5. **Clean Schema**: No wasted columns for event types that don't need coordinates

## Testing

All code compiles successfully. To test:

1. **Run existing tests**:
   ```bash
   cd .cursor/coordinate-parsing/rank-forge
   mvn test
   ```

2. **Run unprocessed events logger**:
   ```bash
   mvn test -Dtest=UnprocessedEventsLoggerTest -Dlog.file.path=/path/to/log/file
   ```

## Next Steps

1. Run the unprocessed events logger to identify any missing event types
2. Test coordinate parsing with real production log files
3. Consider adding coordinate-based analytics/queries if needed
4. Add database migration script if not using auto-update
