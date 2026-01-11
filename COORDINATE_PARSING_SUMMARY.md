# Coordinate Parsing Implementation Summary

## Overview
This worktree implements coordinate parsing for player positions in game events and adds a test utility to identify unprocessed events.

## Changes Made

### 1. Coordinate Fields Added to GameActionEvent
- Added 6 new fields to `GameActionEvent` base class:
  - `player1X`, `player1Y`, `player1Z` (coordinates for player1)
  - `player2X`, `player2Y`, `player2Z` (coordinates for player2)
- Added getters and setters for all coordinate fields
- Added a new constructor that accepts coordinates

**File**: `rank-forge-core/src/main/java/com/rankforge/core/events/GameActionEvent.java`

### 2. Updated KILL_PATTERN to Capture Coordinates
- Modified the regex pattern to capture x, y, z coordinates for both killer and victim
- Pattern now captures:
  - `killerX`, `killerY`, `killerZ`
  - `victimX`, `victimY`, `victimZ`

**File**: `rank-forge-pipeline/src/main/java/com/rankforge/pipeline/CS2LogParser.java`

### 3. Updated parseKillEvent to Extract and Store Coordinates
- Extracts coordinates from the regex matcher
- Sets coordinates on the KillEvent object using setters
- Added `parseCoordinate()` helper method for safe integer parsing

**File**: `rank-forge-pipeline/src/main/java/com/rankforge/pipeline/CS2LogParser.java`

### 4. Updated parseAttackEvent to Extract and Store Coordinates
- ATTACK_PATTERN already captured coordinates (attackerX/Y/Z, victimX/Y/Z)
- Now extracts and stores them in the AttackEvent object
- Uses the same `parseCoordinate()` helper method

**File**: `rank-forge-pipeline/src/main/java/com/rankforge/pipeline/CS2LogParser.java`

### 5. Created UnprocessedEventsLoggerTest
- New test class that processes a production log file
- Identifies all unique log lines that don't match any known event pattern
- Logs unprocessed events with their frequency counts
- Helps identify which events we're not currently processing

**File**: `rank-forge-pipeline/src/test/java/com/rankforge/pipeline/UnprocessedEventsLoggerTest.java`

## Usage

### Running the Unprocessed Events Logger Test

1. **Set the log file path** (one of these methods):
   - Update `LOG_FILE_PATH` constant in the test file
   - Pass as system property: `-Dlog.file.path=/path/to/log/file`

2. **Run the test**:
   ```bash
   cd .cursor/coordinate-parsing/rank-forge
   mvn test -Dtest=UnprocessedEventsLoggerTest
   ```

3. **Review the output**:
   - The test will log a summary of processed vs unprocessed events
   - Lists all unique unprocessed event lines sorted by frequency
   - Helps identify which new events should be processed

### Accessing Coordinates in Events

Coordinates are now available on all `GameActionEvent` subclasses (KillEvent, AttackEvent):

```java
KillEvent killEvent = ...;
Integer killerX = killEvent.getPlayer1X();
Integer killerY = killEvent.getPlayer1Y();
Integer killerZ = killEvent.getPlayer1Z();
Integer victimX = killEvent.getPlayer2X();
Integer victimY = killEvent.getPlayer2Y();
Integer victimZ = killEvent.getPlayer2Z();
```

## Database Storage

### JSON Column Approach
Coordinates are stored in a single JSON column (`coordinates`) in the `GameEvent` table instead of 6 separate columns. This approach:
- Reduces table width (1 column instead of 6)
- Only populated for KILL and ATTACK events (NULL for others)
- Leverages SQL Server's native JSON support
- Easy to extend with additional coordinate data

**JSON Format:**
```json
{
  "player1": {"x": 100, "y": 200, "z": 50},
  "player2": {"x": 150, "y": 250, "z": 60}
}
```

**SQL Query Example:**
```sql
-- Query coordinates from JSON
SELECT 
    id,
    JSON_VALUE(coordinates, '$.player1.x') as player1X,
    JSON_VALUE(coordinates, '$.player1.y') as player1Y,
    JSON_VALUE(coordinates, '$.player1.z') as player1Z
FROM GameEvent
WHERE gameEventType = 'KILL'
  AND coordinates IS NOT NULL
```

## Notes

- **Assist Events**: Assist events don't have coordinates in the log format, so they were not updated
- **Coordinate Parsing**: Coordinates are parsed as integers and stored as `Integer` (nullable) to handle cases where coordinates might be missing
- **Backward Compatibility**: Existing code continues to work - coordinates are optional fields
- **Database Migration**: The `coordinates` column will be added automatically if using `ddl-auto=update`, or you'll need to add it manually:
  ```sql
  ALTER TABLE GameEvent ADD coordinates NVARCHAR(MAX) NULL;
  ```

## Testing

All existing tests should continue to pass. The coordinate fields are optional and don't break existing functionality.
