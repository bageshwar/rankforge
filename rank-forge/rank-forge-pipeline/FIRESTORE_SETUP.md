# GCP Firestore Integration for RankForge

This document explains how to set up and use the Google Cloud Firestore implementation of the PersistenceLayer interface.

## Overview

The `FirestoreBasedPersistenceLayer` provides a NoSQL alternative to the SQLite implementation, storing data in Google Cloud Firestore. This implementation adapts SQL-like operations to Firestore's document-based model:

- **Tables** → **Collections**
- **Rows** → **Documents**  
- **Primary Keys** → **Document IDs**
- **SQL WHERE clauses** → **Firestore Query filters**

## Setup Prerequisites

### 1. GCP Project Setup
1. Create a Google Cloud Project or use an existing one
2. Enable the Firestore API in your project
3. Set up authentication using one of these methods:
   - Service Account Key (recommended for production)
   - Application Default Credentials (for development)
   - Environment variables

### 2. Maven Dependencies
The required dependency is already added to `pom.xml`:

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-firestore</artifactId>
    <version>3.15.8</version>
</dependency>
```

### 3. Authentication Setup

#### Option A: Service Account Key (Recommended for Production)
1. Create a service account in the GCP Console
2. Download the JSON key file
3. Set the environment variable:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/service-account-key.json"
   ```

#### Option B: Application Default Credentials (Development)
```bash
gcloud auth application-default login
```

## Usage

### Basic Initialization

```java
// Initialize with project ID
String projectId = "your-gcp-project-id";
FirestoreBasedPersistenceLayer firestore = new FirestoreBasedPersistenceLayer(projectId);

// Or use custom Firestore instance (useful for testing)
Firestore customFirestore = FirestoreOptions.getDefaultInstance().getService();
FirestoreBasedPersistenceLayer firestore = new FirestoreBasedPersistenceLayer(customFirestore);
```

### Creating Tables (Collections)

```java
ColumnDefinition[] columns = {
    new ColumnDefinition.Builder("id", ColumnType.TEXT).primaryKey().build(),
    new ColumnDefinition.Builder("playerName", ColumnType.TEXT).notNull().build(),
    new ColumnDefinition.Builder("score", ColumnType.INTEGER).build(),
    new ColumnDefinition.Builder("rank", ColumnType.REAL).build()
};

String[] uniqueConstraints = {"id"};

// Creates collection schema metadata
firestore.createTable("players", columns, uniqueConstraints, true);
```

### Inserting Data

```java
Map<String, Object> playerData = new HashMap<>();
playerData.put("id", "player123");
playerData.put("playerName", "John Doe");
playerData.put("score", 1500);
playerData.put("rank", 8.5);

int rowsAffected = firestore.insert("players", playerData);
```

### Querying Data

```java
// Query all players
ResultSet allPlayers = firestore.query("players", null, null);

// Query with WHERE clause (simplified syntax)
ResultSet highScorers = firestore.query("players", null, "score > ?", 1000);

// Process results
while (highScorers.next()) {
    String name = highScorers.getString("playerName");
    int score = highScorers.getInt("score");
    System.out.println(name + ": " + score);
}
```

### Updating Data

```java
Map<String, Object> updates = new HashMap<>();
updates.put("score", 1600);
updates.put("rank", 7.2);

int updated = firestore.update("players", updates, "id = ?", "player123");
```

### Upserting Data

```java
Map<String, Object> playerData = new HashMap<>();
playerData.put("id", "player456");
playerData.put("playerName", "Jane Smith");
playerData.put("score", 1800);

String[] uniqueColumns = {"id"};
String[] updateColumns = {"playerName", "score"}; // null means update all non-unique columns

int affected = firestore.upsert("players", playerData, uniqueColumns, updateColumns);
```

### Deleting Data

```java
int deleted = firestore.delete("players", "score < ?", 500);
```

## Key Differences from SQL Implementation

### 1. Document IDs
- Firestore automatically generates document IDs if not provided
- The implementation looks for common ID field names: `id`, `_id`, `documentId`, `uid`
- If none found, a UUID is generated

### 2. WHERE Clause Limitations
- Currently supports basic operators: `=`, `>`, `>=`, `<`, `<=`, `!=`, `<>`
- Complex WHERE clauses with AND/OR are not yet supported
- Multiple parameters require separate query calls

### 3. Schema Flexibility
- Firestore is schemaless, so `createTable()` mainly stores metadata
- Column definitions are stored in a special `_schemas` collection
- No enforcement of data types at the database level

### 4. Transactions
- Firestore operations are atomic per document
- Cross-document transactions require Firestore's transaction API (not yet implemented)

## Error Handling

The implementation wraps Firestore exceptions in `SQLException` to maintain interface compatibility:

```java
try {
    firestore.insert("players", playerData);
} catch (SQLException e) {
    // Handle both SQL and Firestore errors uniformly
    logger.error("Database operation failed: " + e.getMessage());
}
```

## Performance Considerations

1. **Indexing**: Firestore automatically indexes most fields, but complex queries may need composite indexes
2. **Batch Operations**: Consider implementing batch writes for bulk operations
3. **Read Consistency**: Firestore provides strong consistency for single-document reads
4. **Cost**: Firestore charges per operation, so optimize query patterns

## Limitations

1. **ResultSet Functionality**: Many advanced ResultSet methods throw `UnsupportedOperationException`
2. **SQL Features**: No support for JOINs, GROUP BY, or complex aggregations
3. **WHERE Clause Parser**: Simple parser that supports basic comparison operators only
4. **Transactions**: No cross-document transaction support yet

## Testing

For unit testing, you can use the Firestore emulator:

```bash
# Install and start the emulator
gcloud components install cloud-firestore-emulator
gcloud beta emulators firestore start --host-port=localhost:8080

# Set environment variable
export FIRESTORE_EMULATOR_HOST=localhost:8080
```

## Migration from SQLite

To migrate from SQLite to Firestore:

1. Update your dependency injection to use `FirestoreBasedPersistenceLayer`
2. Ensure your WHERE clauses use the supported operator syntax
3. Test thoroughly as Firestore's query semantics differ from SQL
4. Consider data migration scripts for existing SQLite data

## Example Configuration

```java
@Configuration
public class PersistenceConfig {
    
    @Value("${gcp.project.id}")
    private String projectId;
    
    @Bean
    @ConditionalOnProperty(name = "persistence.type", havingValue = "firestore")
    public PersistenceLayer firestorePersistenceLayer() throws SQLException {
        return new FirestoreBasedPersistenceLayer(projectId);
    }
    
    @Bean
    @ConditionalOnProperty(name = "persistence.type", havingValue = "sqlite", matchIfMissing = true)
    public PersistenceLayer sqlitePersistenceLayer() throws SQLException {
        return new SQLiteBasedPersistenceLayer("./data");
    }
}
```

This allows switching between SQLite and Firestore via configuration properties.