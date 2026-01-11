# Coordinate Storage Design Decision

## Problem
Adding 6 separate columns (player1X, player1Y, player1Z, player2X, player2Y, player2Z) to `GameEventEntity` would:
- Add columns to ALL event types (due to Single Table Inheritance)
- Result in mostly NULL values (only KILL and ATTACK events need coordinates)
- Increase table width unnecessarily

## Recommended Solution: JSON Column

### Option 1: Single JSON Column (Recommended)
Store coordinates as a JSON string in a single nullable column.

**Pros:**
- Only 1 column instead of 6
- Flexible - easy to extend with additional coordinate data
- SQL Server 2016+ has native JSON support with indexing
- Only populated for events that need it (KILL, ATTACK)
- Can query JSON fields directly in SQL

**Cons:**
- Slightly more complex to query (but SQL Server JSON functions are good)
- Need to serialize/deserialize in Java code

### Option 2: Add Columns Only to Subclasses
Add coordinate columns to `KillEventEntity` and `AttackEventEntity` only.

**Pros:**
- Semantically cleaner (columns only where needed)
- Type-safe in Java code

**Cons:**
- With Single Table Inheritance, columns still exist in base table
- Still results in 6 columns (3 per entity type)
- NULL for all other event types

### Option 3: Keep 6 Separate Columns
Add all 6 columns to base `GameEventEntity`.

**Pros:**
- Simple queries
- Easy indexing on individual coordinates
- Type-safe

**Cons:**
- 6 columns that are NULL for most event types
- Table bloat
- Not normalized

## Recommendation: JSON Column

Add a single `coordinates` JSON column to `GameEventEntity`:

```java
@Column(name = "coordinates", columnDefinition = "NVARCHAR(MAX)")
private String coordinates; // JSON: {"player1": {"x": 100, "y": 200, "z": 50}, "player2": {"x": 150, "y": 250, "z": 60}}
```

**Benefits:**
1. Only 1 column instead of 6
2. SQL Server can index and query JSON natively
3. Easy to extend (e.g., add angle, velocity later)
4. Cleaner table structure

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

**Java Code:**
```java
// Serialize coordinates to JSON
ObjectMapper mapper = new ObjectMapper();
Map<String, Map<String, Integer>> coords = Map.of(
    "player1", Map.of("x", 100, "y", 200, "z", 50),
    "player2", Map.of("x", 150, "y", 250, "z", 60)
);
entity.setCoordinates(mapper.writeValueAsString(coords));

// Deserialize from JSON
Map<String, Map<String, Integer>> coords = mapper.readValue(
    entity.getCoordinates(), 
    new TypeReference<Map<String, Map<String, Integer>>>() {}
);
```

## Alternative: If You Need Individual Column Queries

If you frequently need to query/filter by individual coordinates (e.g., "find all kills where player1X > 1000"), then separate columns with indexes might be better for performance. But for most use cases, JSON is sufficient and more flexible.
