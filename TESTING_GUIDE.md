# Testing Guide for Game Deduplication and Admin Delete API

This guide explains how to test the changes in the `.cursor/game-dedupe-delete` worktree.

## Prerequisites

1. **Java 17** installed
2. **Maven** installed
3. **Database** configured (Azure SQL or local database)
4. **API Key** configured in `application-local.properties`

## Step 1: Navigate to the Worktree

```bash
cd /Users/bpratapnarain/src/rankforge/.cursor/game-dedupe-delete/rank-forge
```

## Step 2: Build the Project

Build all modules (core, pipeline, server):

```bash
# From the rank-forge directory
mvn clean install -DskipTests
```

Or build just the server module:

```bash
cd rank-forge-server
mvn clean package -DskipTests
```

## Step 3: Configure Local Properties

Make sure you have `application-local.properties` configured with:
- Database connection details
- API key: `rankforge.api.key=your-test-api-key`

You can generate a test API key:
```bash
openssl rand -hex 32
```

## Step 4: Run the Server

From the `rank-forge-server` directory:

```bash
# Option 1: Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Option 2: Using the JAR
java -jar target/rank-forge-server-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

The server should start on `http://localhost:8080`

## Step 5: Test the Endpoints

### Test 1: Health Check (No Auth Required)

```bash
curl http://localhost:8080/api/admin/health
```

Expected: `Admin API is healthy`

### Test 2: Test Deduplication

**First, ingest a game** (this will create a game in the database):

```bash
# Replace with your actual API key
API_KEY="your-api-key-here"

# Ingest a game via pipeline endpoint
curl -X POST http://localhost:8080/api/pipeline/process \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"logFile": "path/to/logfile.json"}'
```

**Then try to ingest the same game again** - it should be skipped due to deduplication.

Check the logs for:
```
Duplicate game detected - skipping ingestion. Existing game ID: X, timestamp: Y, map: Z
```

### Test 3: List Games (to get a game ID)

```bash
curl http://localhost:8080/api/games | jq '.[0].id'
```

This will return a game ID you can use for deletion testing.

### Test 4: Delete a Game (Admin API)

```bash
# Replace GAME_ID with an actual game ID from your database
GAME_ID=123
API_KEY="your-api-key-here"

curl -X DELETE "http://localhost:8080/api/admin/games/$GAME_ID" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Game deleted successfully",
  "gameId": 123,
  "deletedAt": "2026-01-11T10:30:00Z"
}
```

### Test 5: Verify Deletion

```bash
# Try to get the deleted game - should return 404
curl http://localhost:8080/api/games/$GAME_ID
```

Expected: `404 Not Found`

### Test 6: Test Unauthorized Access

```bash
# Try without API key
curl -X DELETE "http://localhost:8080/api/admin/games/123"

# Try with wrong API key
curl -X DELETE "http://localhost:8080/api/admin/games/123" \
  -H "X-API-Key: wrong-key"
```

Both should return: `401 Unauthorized`

## Step 6: Check Logs

Monitor the application logs for:

1. **Deduplication logs:**
   ```
   Duplicate game detected - skipping ingestion. Existing game ID: X, timestamp: Y, map: Z
   ```

2. **Deletion audit logs:**
   ```
   ADMIN_AUDIT: Game deletion requested - gameId: X, time: Y, requester: API
   ADMIN_DELETE: Starting deletion of game ID: X
   ADMIN_DELETE: Deleted N game events (including GAME_OVER) for game ID: X
   ADMIN_DELETE: Deleted M accolades for game ID: X
   ADMIN_DELETE: Deleted K player stats entries for game ID: X
   ADMIN_DELETE: Successfully deleted game ID: X
   ```

## Step 7: Verify Database State

After deletion, verify in your database that:

1. Game entity is deleted from `Game` table
2. All events are deleted from `GameEvent` table (where `gameId = X`)
3. All accolades are deleted from `Accolade` table (where `gameId = X`)
4. Player stats with matching `gameTimestamp` are deleted from `PlayerStats` table

## Troubleshooting

### Issue: "API key not configured"
- Make sure `rankforge.api.key` is set in `application-local.properties`
- Or set it as environment variable: `export PIPELINE_API_KEY=your-key`

### Issue: "Game not found"
- Make sure you're using a valid game ID
- Check that games exist: `curl http://localhost:8080/api/games`

### Issue: Build errors
- Make sure you're in the worktree directory
- Try: `mvn clean install -DskipTests` from the root `rank-forge` directory

### Issue: Database connection errors
- Verify your `application-local.properties` has correct database credentials
- Check that the database is accessible

## Quick Test Script

Save this as `test-admin-api.sh`:

```bash
#!/bin/bash

API_KEY="${PIPELINE_API_KEY:-your-api-key-here}"
BASE_URL="http://localhost:8080"

echo "Testing Admin API..."

# Health check
echo "1. Health check..."
curl -s "$BASE_URL/api/admin/health"
echo -e "\n"

# Get first game ID
echo "2. Getting game list..."
GAME_ID=$(curl -s "$BASE_URL/api/games" | jq -r '.[0].id // empty')

if [ -z "$GAME_ID" ]; then
    echo "No games found. Please ingest a game first."
    exit 1
fi

echo "Found game ID: $GAME_ID"

# Delete game
echo "3. Deleting game $GAME_ID..."
RESPONSE=$(curl -s -X DELETE "$BASE_URL/api/admin/games/$GAME_ID" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json")

echo "$RESPONSE" | jq '.'

# Verify deletion
echo "4. Verifying deletion..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/games/$GAME_ID")
if [ "$STATUS" == "404" ]; then
    echo "✓ Game successfully deleted (404 as expected)"
else
    echo "✗ Game still exists (status: $STATUS)"
fi
```

Make it executable and run:
```bash
chmod +x test-admin-api.sh
./test-admin-api.sh
```
