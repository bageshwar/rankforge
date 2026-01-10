# Event Processing Flow

This document describes how CS2 game events flow through the RankForge pipeline and why entity references can be set directly (no post-hoc linking needed).

## Parser Behavior: The Rewind Pattern

The key insight is in [`CS2LogParser.parseLine()`](src/main/java/com/rankforge/pipeline/CS2LogParser.java):

1. **Log lines are scanned sequentially**, tracking `Round_Start` indices (line 198-202)
2. When `GAME_OVER` is detected (line 204-215):
   - Accolades are parsed and queued to `EventProcessingContext` (line 310)
   - `matchStarted = true` is set (line 293)
   - **Parser REWINDS** to first round (line 314-316): `return new ParseLineResponse(..., indexToStart)`
3. Events are then processed in chronological order until `GAME_PROCESSED`

## Actual Event Order Delivered to EventProcessor

```
GAME_OVER        ← Comes FIRST (parser detects end of match)
   ↓ (rewind)
ROUND_START      ← Round 1
KILL / ASSIST / ATTACK
ROUND_END
ROUND_START      ← Round 2
KILL / ASSIST / ATTACK
ROUND_END
   ...
GAME_PROCESSED   ← Comes LAST (signals end of processing)
```

## Why This Enables Direct Reference Setting

Since `GAME_OVER` is processed **before** any round events:

| When Processing... | GameEntity exists? | RoundStartEventEntity exists? |
|-------------------|-------------------|------------------------------|
| GAME_OVER         | Created now       | No                           |
| ROUND_START       | Yes ✓             | Created now                  |
| KILL/ASSIST/ATTACK| Yes ✓             | Yes ✓                        |
| ROUND_END         | Yes ✓             | Yes ✓                        |
| GAME_PROCESSED    | Yes ✓             | No (cleared after each round)|

**Result:** Every event can have its `game` and `roundStart` references set immediately at creation time. No pending list or retroactive updates needed.

## Code Flow

```
CS2LogParser.parseLine()
    │
    ├─► GAME_OVER detected
    │       └─► Accolades parsed and queued to context
    │       └─► Returns GameOverEvent, rewinds to first round
    │
    └─► EventProcessorImpl.processEvent()
            │
            ├─► visit(GameOverEvent)
            │       └─► Create GameEntity, store in context.currentGame
            │       └─► Link queued accolades to GameEntity
            │
            ├─► visit(RoundStartEvent)
            │       └─► Create RoundStartEventEntity
            │           Set game = context.currentGame
            │           Store in context.currentRoundStart
            │
            ├─► visit(KillEvent / AssistEvent / AttackEvent)
            │       └─► Create entity
            │           Set game = context.currentGame
            │           Set roundStart = context.currentRoundStart
            │
            ├─► visit(RoundEndEvent)
            │       └─► Create entity with game and roundStart
            │           Clear context.currentRoundStart
            │
            └─► visit(GameProcessedEvent)
                    └─► Batch persist all pending entities
                    └─► Batch persist all pending accolades
                        JPA resolves IDs via cascade
                        Clear context
```

## Key Files

| File | Responsibility |
|------|----------------|
| [`CS2LogParser.java`](src/main/java/com/rankforge/pipeline/CS2LogParser.java) | Parses log lines, handles rewind logic, queues accolades |
| [`EventProcessorImpl.java`](src/main/java/com/rankforge/pipeline/EventProcessorImpl.java) | Processes events, creates GameEntity, links accolades |
| [`JpaEventStore.java`](src/main/java/com/rankforge/pipeline/persistence/JpaEventStore.java) | Converts events to entities, batch persistence |
| [`AccoladeStore.java`](src/main/java/com/rankforge/pipeline/persistence/AccoladeStore.java) | Creates accolade entities, queues to context |
| [`EventProcessingContext.java`](src/main/java/com/rankforge/pipeline/persistence/EventProcessingContext.java) | Tracks `currentGame`, `currentRoundStart`, pending entities and accolades |

## Accolade Handling

Accolades are parsed in [`parseAndQueueAccolades()`](src/main/java/com/rankforge/pipeline/CS2LogParser.java) (line 329-388) **before** `GameOverEvent` is returned. At this point, `GameEntity` doesn't exist yet.

**Solution:** 
1. Accolades are added to `EventProcessingContext.pendingAccolades` 
2. When `EventProcessorImpl.visit(GameOverEvent)` creates the `GameEntity`, it calls `context.linkAccoladesToGame()`
3. All accolades get their `game` reference set
4. They're batch persisted along with game events during `GAME_PROCESSED`

## Entity Relationships

```java
// GameEventEntity.java
@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
@JoinColumn(name = "gameId")
private GameEntity game;

@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
@JoinColumn(name = "roundStartEventId")
private RoundStartEventEntity roundStart;

// AccoladeEntity.java
@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
@JoinColumn(name = "gameId", nullable = false)
private GameEntity game;
```

JPA's `CascadeType.PERSIST` ensures that when we save `GameEventEntity` or `AccoladeEntity`, the referenced `GameEntity` and `RoundStartEventEntity` are persisted first (if not already), and foreign keys are resolved automatically.
