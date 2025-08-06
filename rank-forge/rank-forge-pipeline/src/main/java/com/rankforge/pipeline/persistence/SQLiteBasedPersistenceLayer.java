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

package com.rankforge.pipeline.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * This class is a SQLite based implementation of persistence layer
 * Author bageshwar.pn
 * Date 10/11/24
 */

public class SQLiteBasedPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteBasedPersistenceLayer.class);
    private final String dbPath;
    private final ReentrantLock connectionLock = new ReentrantLock();
    private volatile Connection connection;


    /**
     * Constructor initializes database connection
     *
     * @param dbPath Path to SQLite database file
     * @throws SQLException if database connection fails
     */
    public SQLiteBasedPersistenceLayer(String dbPath) throws SQLException {
        this.dbPath = dbPath + "/rankforge.db";
        initializeConnection();
    }

    /**
     * Thread-safe method to get or create a database connection
     *
     * @return active database connection
     * @throws SQLException if connection fails
     */
    private Connection getConnection() throws SQLException {
        Connection result = connection;
        if (result == null || result.isClosed()) {
            connectionLock.lock();
            try {
                result = connection;
                if (result == null || result.isClosed()) {
                    initializeConnection();
                    result = connection;
                }
            } finally {
                connectionLock.unlock();
            }
        }
        return result;
    }

    /**
     * Initializes database connection
     *
     * @throws SQLException if connection fails
     */
    private void initializeConnection() throws SQLException {
        try {
            // Explicitly verify driver availability
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                String errorMsg = """
                        SQLite JDBC driver not found. Please ensure you have added the \
                        sqlite-jdbc dependency to your project:
                        Maven: <dependency>
                            <groupId>org.xerial</groupId>
                            <artifactId>sqlite-jdbc</artifactId>
                            <version>3.45.1.0</version>
                        </dependency>
                        Gradle: implementation 'org.xerial:sqlite-jdbc:3.45.1.0'""";
                LOGGER.error(errorMsg);
                throw new SQLException(errorMsg, e);
            }

            // Verify database path
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    String errorMsg = "Failed to create database directory: " + parentDir.getAbsolutePath();
                    LOGGER.error(errorMsg);
                    throw new SQLException(errorMsg);
                }
            }

            String jdbcUrl = "jdbc:sqlite:" + dbPath;

            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl(jdbcUrl);

            //connection = DriverManager.getConnection(jdbcUrl);
            //connection.setAutoCommit(true);

            connection = dataSource.getConnection();
            connection.setAutoCommit(true);

            //DB db = ((org.sqlite.core.NativeDB.) connection).getDB();
            //db.setTrace(new SQLiteTraceCallback());

            // Test connection
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SELECT 1");
            }

            LOGGER.info("Successfully connected to database: {}", dbPath);

        } catch (SQLException e) {
            String errorMsg = "Failed to initialize SQLite database connection. " +
                    "Error: " + e.getMessage() + "\n" +
                    "Database path: " + dbPath;
            LOGGER.error(errorMsg);
            throw new SQLException(errorMsg, e);
        }
    }

    /**
     * Creates a new table in the database
     *
     * @param tableName   Name of the table
     * @param columns     Array of column definitions
     * @param ifNotExists If true, adds IF NOT EXISTS clause
     * @throws SQLException if table creation fails
     */
    @Override
    public synchronized void createTable(String tableName,
                                         ColumnDefinition[] columns,
                                         String[] uniqueConstraints,
                                         boolean ifNotExists) throws SQLException {
        validateTableName(tableName);
        if (columns == null || columns.length == 0) {
            throw new SQLException("No columns defined for table creation");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ");
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(tableName).append(" (");

        // Add column definitions
        StringJoiner columnDefinitions = new StringJoiner(", ");
        for (ColumnDefinition column : columns) {
            columnDefinitions.add(column.toSqlDefinition());
        }
        sql.append(columnDefinitions.toString());

        // Add unique constraints if specified
        if (uniqueConstraints != null && uniqueConstraints.length > 0) {
            sql.append(", UNIQUE(")
                    .append(String.join(", ", uniqueConstraints))
                    .append(")");
        }

        sql.append(")");

        LOGGER.debug("Executing table creation SQL: {}", sql);

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql.toString());
            LOGGER.debug("Successfully created table: {}", tableName);
        }
    }

    /**
     * Thread-safe insert operation
     */
    @Override
    public synchronized int insert(String tableName, Map<String, Object> data) throws SQLException {
        validateInput(tableName, data);

        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream()
                .map(key -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName, columns, placeholders);

        //LOGGER.debug("Executing insert query: {}", sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int paramIndex = 1;
            for (Object value : data.values()) {
                stmt.setObject(paramIndex++, value);
            }
            int rowsAffected = stmt.executeUpdate();
            //LOGGER.debug("Inserted {} row(s) into table {}", rowsAffected, tableName);
            return rowsAffected;
        }
    }

    /**
     * Thread-safe update operation
     */
    @Override
    public synchronized int update(String tableName, Map<String, Object> data,
                                   String whereClause, Object... whereParams) throws SQLException {
        validateInput(tableName, data);

        StringJoiner setClause = new StringJoiner(", ");
        data.keySet().forEach(key -> setClause.add(key + " = ?"));

        String sql = String.format("UPDATE %s SET %s WHERE %s",
                tableName, setClause.toString(), whereClause);

        //LOGGER.info("Executing update query: {}", sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int paramIndex = 1;

            for (Object value : data.values()) {
                stmt.setObject(paramIndex++, value);
            }

            for (Object param : whereParams) {
                stmt.setObject(paramIndex++, param);
            }

            int rowsAffected = stmt.executeUpdate();
            //LOGGER.info("Updated {} row(s) in table {}", rowsAffected, tableName);
            return rowsAffected;
        }
    }

    /**
     * Thread-safe query operation
     */
    @Override
    public synchronized ResultSet query(String tableName, String[] columns,
                                        String whereClause, Object... whereParams) throws SQLException {
        validateTableName(tableName);

        String columnList = columns != null && columns.length > 0 ?
                String.join(", ", columns) : "*";

        String sql = String.format("SELECT %s FROM %s", columnList, tableName);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }

        //LOGGER.debug("Executing select query: {}", sql);

        PreparedStatement stmt = getConnection().prepareStatement(sql);
        if (whereParams != null) {
            for (int i = 0; i < whereParams.length; i++) {
                stmt.setObject(i + 1, whereParams[i]);
            }
        }

        return stmt.executeQuery();

    }

    /**
     * Thread-safe delete operation
     */
    @Override
    public synchronized int delete(String tableName, String whereClause,
                                   Object... whereParams) throws SQLException {
        validateTableName(tableName);

        String sql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < whereParams.length; i++) {
                stmt.setObject(i + 1, whereParams[i]);
            }

            int rowsAffected = stmt.executeUpdate();
            LOGGER.info(String.format("Deleted %d row(s) from table %s", rowsAffected, tableName));
            return rowsAffected;
        }
    }

    @Override
    public synchronized int upsert(String tableName,
                                   Map<String, Object> data,
                                   String[] uniqueColumns,
                                   String[] updateColumns) throws SQLException {
        validateInput(tableName, data);
        if (uniqueColumns == null || uniqueColumns.length == 0) {
            throw new SQLException("Unique columns must be specified for upsert operation");
        }

        // Validate all specified columns exist in data map
        for (String column : uniqueColumns) {
            if (!data.containsKey(column)) {
                throw new SQLException("Unique column '" + column + "' not found in data map");
            }
        }

        // If updateColumns is null, use all non-unique columns
        Set<String> columnsToUpdate;
        if (updateColumns == null) {
            columnsToUpdate = new HashSet<>(data.keySet());
            columnsToUpdate.removeAll(Arrays.asList(uniqueColumns));
        } else {
            columnsToUpdate = new HashSet<>(Arrays.asList(updateColumns));
            // Validate update columns exist in data map
            for (String column : updateColumns) {
                if (!data.containsKey(column)) {
                    throw new SQLException("Update column '" + column + "' not found in data map");
                }
            }
        }

        // Build the SQL query
        StringBuilder sql = new StringBuilder()
                .append("INSERT INTO ").append(tableName)
                .append(" (").append(String.join(", ", data.keySet())).append(")")
                .append(" VALUES (").append(String.join(", ", Collections.nCopies(data.size(), "?")))
                .append(")")
                .append(" ON CONFLICT(").append(String.join(", ", uniqueColumns)).append(")")
                .append(" DO UPDATE SET ");

        // Add update assignments
        StringJoiner updateClause = new StringJoiner(", ");
        for (String column : columnsToUpdate) {
            updateClause.add(column + " = excluded." + column);
        }
        sql.append(updateClause.toString());

        //LOGGER.debug("Executing upsert query: {}", sql);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            // Set values for INSERT
            int paramIndex = 1;
            for (Object value : data.values()) {
                stmt.setObject(paramIndex++, value);
            }

            int rowsAffected = stmt.executeUpdate();
            //LOGGER.debug("Upserted {} row(s) in table {}", rowsAffected, tableName);
            return rowsAffected;
        }
    }

    /**
     * Validates table name
     *
     * @param tableName Name of the table
     * @throws SQLException if validation fails
     */
    private void validateTableName(String tableName) throws SQLException {
        if (tableName == null || tableName.trim().isEmpty()) {
            LOGGER.error("Table name cannot be null or empty");
            throw new SQLException("Table name cannot be null or empty");
        }
        // Basic SQL injection prevention
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            LOGGER.error("Invalid table name format: {}", tableName);
            throw new SQLException("Invalid table name format");
        }
    }

    /**
     * Validates table name and data map
     *
     * @param tableName Name of the table
     * @param data      Map of column names to values
     * @throws SQLException if validation fails
     */
    private void validateInput(String tableName, Map<String, Object> data) throws SQLException {
        validateTableName(tableName);
        if (data == null || data.isEmpty()) {
            LOGGER.error("Data map cannot be null or empty");
            throw new SQLException("Data map cannot be null or empty");
        }
    }

    /**
     * Batch insert multiple records
     */
    @Override
    public synchronized int batchInsert(String tableName, List<Map<String, Object>> dataList) throws SQLException {
        validateTableName(tableName);
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        
        // All records should have the same columns
        Map<String, Object> firstRecord = dataList.get(0);
        String columns = String.join(", ", firstRecord.keySet());
        String placeholders = firstRecord.keySet().stream()
                .map(key -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName, columns, placeholders);

        Connection conn = getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false); // Start transaction
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map<String, Object> data : dataList) {
                    int paramIndex = 1;
                    for (String key : firstRecord.keySet()) {
                        stmt.setObject(paramIndex++, data.get(key));
                    }
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                conn.commit(); // Commit transaction
                
                int totalInserted = Arrays.stream(results).sum();
                LOGGER.debug("Batch inserted {} records into table {}", totalInserted, tableName);
                return totalInserted;
            }
        } catch (SQLException e) {
            conn.rollback(); // Rollback on error
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit); // Restore original autocommit
        }
    }

    /**
     * Batch update multiple records
     */
    @Override
    public synchronized int batchUpdate(String tableName, List<BatchUpdateOperation> updates) throws SQLException {
        validateTableName(tableName);
        if (updates == null || updates.isEmpty()) {
            return 0;
        }

        Connection conn = getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        int totalUpdated = 0;
        
        try {
            conn.setAutoCommit(false); // Start transaction
            
            for (BatchUpdateOperation update : updates) {
                StringJoiner setClause = new StringJoiner(", ");
                update.getData().keySet().forEach(key -> setClause.add(key + " = ?"));

                String sql = String.format("UPDATE %s SET %s WHERE %s",
                        tableName, setClause.toString(), update.getWhereClause());

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    int paramIndex = 1;

                    for (Object value : update.getData().values()) {
                        stmt.setObject(paramIndex++, value);
                    }

                    for (Object param : update.getWhereParams()) {
                        stmt.setObject(paramIndex++, param);
                    }

                    totalUpdated += stmt.executeUpdate();
                }
            }
            
            conn.commit(); // Commit transaction
            LOGGER.debug("Batch updated {} records in table {}", totalUpdated, tableName);
            return totalUpdated;
            
        } catch (SQLException e) {
            conn.rollback(); // Rollback on error
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit); // Restore original autocommit
        }
    }

    /**
     * Batch upsert multiple records
     */
    @Override
    public synchronized int batchUpsert(String tableName,
                                        List<Map<String, Object>> dataList,
                                        String[] uniqueColumns,
                                        String[] updateColumns) throws SQLException {
        validateTableName(tableName);
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        if (uniqueColumns == null || uniqueColumns.length == 0) {
            throw new SQLException("Unique columns must be specified for batch upsert operation");
        }

        // All records should have the same structure
        Map<String, Object> firstRecord = dataList.get(0);
        
        // If updateColumns is null, use all non-unique columns
        Set<String> columnsToUpdate;
        if (updateColumns == null) {
            columnsToUpdate = new HashSet<>(firstRecord.keySet());
            columnsToUpdate.removeAll(Arrays.asList(uniqueColumns));
        } else {
            columnsToUpdate = new HashSet<>(Arrays.asList(updateColumns));
        }

        // Build the SQL query
        StringBuilder sql = new StringBuilder()
                .append("INSERT INTO ").append(tableName)
                .append(" (").append(String.join(", ", firstRecord.keySet())).append(")")
                .append(" VALUES (").append(String.join(", ", Collections.nCopies(firstRecord.size(), "?")))
                .append(")")
                .append(" ON CONFLICT(").append(String.join(", ", uniqueColumns)).append(")")
                .append(" DO UPDATE SET ");

        // Add update assignments
        StringJoiner updateClause = new StringJoiner(", ");
        for (String column : columnsToUpdate) {
            updateClause.add(column + " = excluded." + column);
        }
        sql.append(updateClause.toString());

        Connection conn = getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false); // Start transaction
            
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (Map<String, Object> data : dataList) {
                    int paramIndex = 1;
                    for (String key : firstRecord.keySet()) {
                        stmt.setObject(paramIndex++, data.get(key));
                    }
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                conn.commit(); // Commit transaction
                
                int totalAffected = Arrays.stream(results).sum();
                LOGGER.debug("Batch upserted {} records in table {}", totalAffected, tableName);
                return totalAffected;
            }
        } catch (SQLException e) {
            conn.rollback(); // Rollback on error
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit); // Restore original autocommit
        }
    }

    @Override
    public void close() {
        connectionLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            LOGGER.error("Error closing database connection", e);
        } finally {
            connectionLock.unlock();
        }
    }
}