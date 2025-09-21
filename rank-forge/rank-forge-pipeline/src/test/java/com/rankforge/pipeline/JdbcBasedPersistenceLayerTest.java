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

package com.rankforge.pipeline;

import com.rankforge.pipeline.persistence.ColumnDefinition;
import com.rankforge.pipeline.persistence.ColumnType;
import com.rankforge.pipeline.persistence.JdbcBasedPersistenceLayer;
import com.rankforge.pipeline.persistence.PersistenceLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JdbcBasedPersistenceLayer
 * This test uses H2 in-memory database for testing
 */
class JdbcBasedPersistenceLayerTest {

    private PersistenceLayer persistenceLayer;

    @BeforeEach
    void setUp() throws SQLException {
        // Using H2 in-memory database for testing
        String jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        String username = "sa";
        String password = "";
        
        persistenceLayer = new JdbcBasedPersistenceLayer(jdbcUrl, username, password);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (persistenceLayer != null) {
            persistenceLayer.close();
        }
    }

    @Test
    void testCreateTable() throws SQLException {
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("name", ColumnType.TEXT_SHORT)
                .notNull()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        // Test table creation
        assertDoesNotThrow(() -> 
            persistenceLayer.createTable("test_players", columns, null, true)
        );

        // Test creating the same table again with IF NOT EXISTS
        assertDoesNotThrow(() -> 
            persistenceLayer.createTable("test_players", columns, null, true)
        );
    }

    @Test
    void testInsertAndQuery() throws SQLException {
        // Create table first
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("name", ColumnType.TEXT_SHORT)
                .notNull()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        persistenceLayer.createTable("test_players", columns, null, true);

        // Test insert
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", "John Doe");
        playerData.put("score", 95.5);

        int rowsInserted = persistenceLayer.insert("test_players", playerData);
        assertEquals(1, rowsInserted);

        // Test query
        try (ResultSet rs = persistenceLayer.query("test_players", null, "name = ?", "John Doe")) {
            assertTrue(rs.next());
            assertEquals("John Doe", rs.getString("name"));
            assertEquals(95.5, rs.getDouble("score"), 0.001);
            assertFalse(rs.next());
        }
    }

    @Test
    void testUpdate() throws SQLException {
        // Create table and insert data
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("name", ColumnType.TEXT_SHORT)
                .notNull()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        persistenceLayer.createTable("test_players", columns, null, true);

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", "Jane Doe");
        playerData.put("score", 85.0);
        persistenceLayer.insert("test_players", playerData);

        // Test update
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("score", 90.0);

        int rowsUpdated = persistenceLayer.update("test_players", updateData, "name = ?", "Jane Doe");
        assertEquals(1, rowsUpdated);

        // Verify update
        try (ResultSet rs = persistenceLayer.query("test_players", null, "name = ?", "Jane Doe")) {
            assertTrue(rs.next());
            assertEquals(90.0, rs.getDouble("score"), 0.001);
        }
    }

    @Test
    void testUpsert() throws SQLException {
        // Create table with unique constraint
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("username", ColumnType.TEXT_SHORT)
                .notNull()
                .unique()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        String[] uniqueConstraints = {"username"};
        persistenceLayer.createTable("test_users", columns, uniqueConstraints, true);

        // Test initial upsert (insert)
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", "player1");
        userData.put("score", 100.0);

        String[] uniqueColumns = {"username"};
        int rowsAffected = persistenceLayer.upsert("test_users", userData, uniqueColumns, null);
        assertTrue(rowsAffected >= 1);

        // Test second upsert (update)
        userData.put("score", 150.0);
        rowsAffected = persistenceLayer.upsert("test_users", userData, uniqueColumns, null);
        assertTrue(rowsAffected >= 1);

        // Verify final score
        try (ResultSet rs = persistenceLayer.query("test_users", null, "username = ?", "player1")) {
            assertTrue(rs.next());
            assertEquals(150.0, rs.getDouble("score"), 0.001);
        }
    }

    @Test
    void testBatchInsert() throws SQLException {
        // Create table
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("name", ColumnType.TEXT_SHORT)
                .notNull()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        persistenceLayer.createTable("test_players", columns, null, true);

        // Prepare batch data
        List<Map<String, Object>> batchData = List.of(
            Map.of("name", "Player1", "score", 100.0),
            Map.of("name", "Player2", "score", 200.0),
            Map.of("name", "Player3", "score", 300.0)
        );

        // Test batch insert
        int rowsInserted = persistenceLayer.batchInsert("test_players", batchData);
        assertEquals(3, rowsInserted);

        // Verify all records were inserted
        try (ResultSet rs = persistenceLayer.query("test_players", null, null)) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            assertEquals(3, count);
        }
    }

    @Test
    void testDelete() throws SQLException {
        // Create table and insert data
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("name", ColumnType.TEXT_SHORT)
                .notNull()
                .build()
        };

        persistenceLayer.createTable("test_players", columns, null, true);

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", "ToDelete");
        persistenceLayer.insert("test_players", playerData);

        // Test delete
        int rowsDeleted = persistenceLayer.delete("test_players", "name = ?", "ToDelete");
        assertEquals(1, rowsDeleted);

        // Verify deletion
        try (ResultSet rs = persistenceLayer.query("test_players", null, "name = ?", "ToDelete")) {
            assertFalse(rs.next());
        }
    }

    @Test
    void testDatabaseTypeDetection() {
        if (persistenceLayer instanceof JdbcBasedPersistenceLayer jdbcLayer) {
            assertEquals(JdbcBasedPersistenceLayer.DatabaseType.H2, jdbcLayer.getDatabaseType());
        }
    }

    @Test
    void testInvalidTableName() {
        assertThrows(SQLException.class, () -> {
            persistenceLayer.insert("invalid-table-name!", Map.of("col", "value"));
        });
    }

    @Test
    void testNullParameterHandling() throws SQLException {
        // Create table with nullable columns
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("name", ColumnType.TEXT_SHORT)
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        persistenceLayer.createTable("test_nullable", columns, null, true);

        // Test insert with null values
        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", "John Doe");
        playerData.put("score", null); // Null value

        int rowsInserted = persistenceLayer.insert("test_nullable", playerData);
        assertEquals(1, rowsInserted);

        // Test query with null parameter
        try (ResultSet rs = persistenceLayer.query("test_nullable", null, "score IS NULL")) {
            assertTrue(rs.next());
            assertEquals("John Doe", rs.getString("name"));
            assertNull(rs.getObject("score"));
        }
    }

    @Test
    void testEnumParameterHandling() throws SQLException {
        // Create table
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("status", ColumnType.TEXT_SHORT)
                .build()
        };

        persistenceLayer.createTable("test_enum", columns, null, true);

        // Test insert with enum value (simulated with string)
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ACTIVE"); // Enum-like string

        int rowsInserted = persistenceLayer.insert("test_enum", data);
        assertEquals(1, rowsInserted);

        // Test query with enum parameter
        try (ResultSet rs = persistenceLayer.query("test_enum", null, "status = ?", "ACTIVE")) {
            assertTrue(rs.next());
            assertEquals("ACTIVE", rs.getString("status"));
        }
    }

    @Test
    void testUpsertWithPrimaryKeyConflict() throws SQLException {
        // Create table with unique constraint
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("playerId", ColumnType.TEXT_SHORT)
                .notNull()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build(),
            new ColumnDefinition.Builder("updated", ColumnType.TEXT_SHORT)
                .build()
        };

        String[] uniqueConstraints = {"playerId"};
        persistenceLayer.createTable("test_upsert", columns, uniqueConstraints, true);

        // First upsert (should insert)
        Map<String, Object> playerData1 = new HashMap<>();
        playerData1.put("playerId", "player123");
        playerData1.put("score", 100.0);
        playerData1.put("updated", "2024-01-01");

        String[] uniqueColumns = {"playerId"};
        String[] updateColumns = {"score", "updated"};
        
        int result1 = persistenceLayer.upsert("test_upsert", playerData1, uniqueColumns, updateColumns);
        assertTrue(result1 >= 1, "First upsert should insert record");

        // Second upsert with same playerId (should update)
        Map<String, Object> playerData2 = new HashMap<>();
        playerData2.put("playerId", "player123");
        playerData2.put("score", 200.0);
        playerData2.put("updated", "2024-01-02");

        int result2 = persistenceLayer.upsert("test_upsert", playerData2, uniqueColumns, updateColumns);
        assertTrue(result2 >= 1, "Second upsert should update record");

        // Verify final state
        try (ResultSet rs = persistenceLayer.query("test_upsert", null, "playerId = ?", "player123")) {
            assertTrue(rs.next());
            assertEquals("player123", rs.getString("playerId"));
            assertEquals(200.0, rs.getDouble("score"), 0.001);
            assertEquals("2024-01-02", rs.getString("updated"));
            assertFalse(rs.next(), "Should only have one record");
        }
    }

    @Test
    void testBatchUpsertWithConflicts() throws SQLException {
        // Create table
        ColumnDefinition[] columns = {
            new ColumnDefinition.Builder("id", ColumnType.INTEGER)
                .primaryKey()
                .autoIncrement()
                .build(),
            new ColumnDefinition.Builder("playerId", ColumnType.TEXT_SHORT)
                .notNull()
                .build(),
            new ColumnDefinition.Builder("score", ColumnType.REAL)
                .build()
        };

        String[] uniqueConstraints = {"playerId"};
        persistenceLayer.createTable("test_batch_upsert", columns, uniqueConstraints, true);

        // Insert initial data
        Map<String, Object> initialData = Map.of("playerId", "player1", "score", 50.0);
        persistenceLayer.insert("test_batch_upsert", initialData);

        // Batch upsert with mix of new and existing players
        List<Map<String, Object>> batchData = List.of(
            Map.of("playerId", "player1", "score", 100.0), // Update existing
            Map.of("playerId", "player2", "score", 200.0), // Insert new
            Map.of("playerId", "player3", "score", 300.0)  // Insert new
        );

        String[] uniqueColumns = {"playerId"};
        String[] updateColumns = {"score"};

        int totalAffected = persistenceLayer.batchUpsert("test_batch_upsert", batchData, uniqueColumns, updateColumns);
        assertTrue(totalAffected >= 3, "Should affect at least 3 rows");

        // Verify results
        try (ResultSet rs = persistenceLayer.query("test_batch_upsert", new String[]{"playerId", "score"}, null)) {
            Map<String, Double> results = new HashMap<>();
            while (rs.next()) {
                results.put(rs.getString("playerId"), rs.getDouble("score"));
            }
            
            assertEquals(3, results.size(), "Should have exactly 3 players");
            assertEquals(100.0, results.get("player1"), 0.001, "Player1 should be updated");
            assertEquals(200.0, results.get("player2"), 0.001, "Player2 should be inserted");
            assertEquals(300.0, results.get("player3"), 0.001, "Player3 should be inserted");
        }
    }
}