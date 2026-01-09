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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * This class is a vanilla JDBC based implementation of persistence layer
 * that supports multiple database types (MySQL, PostgreSQL, H2, etc.)
 * Author bageshwar.pn
 * Date 10/11/24
 */
public class JdbcBasedPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcBasedPersistenceLayer.class);
    
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Properties connectionProperties;
    private final DatabaseType databaseType;
    private final ReentrantLock connectionLock = new ReentrantLock();
    private volatile Connection connection;

    /**
     * Supported database types
     */
    public enum DatabaseType {
        MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
        POSTGRESQL("PostgreSQL", "org.postgresql.Driver"),
        H2("H2", "org.h2.Driver"),
        HSQLDB("HSQLDB", "org.hsqldb.jdbc.JDBCDriver"),
        DERBY("Apache Derby", "org.apache.derby.jdbc.EmbeddedDriver"),
        ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver"),
        SQL_SERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

        private final String displayName;
        private final String driverClassName;

        DatabaseType(String displayName, String driverClassName) {
            this.displayName = displayName;
            this.driverClassName = driverClassName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        /**
         * Detects database type from JDBC URL
         */
        public static DatabaseType fromJdbcUrl(String jdbcUrl) {
            if (jdbcUrl == null) {
                throw new IllegalArgumentException("JDBC URL cannot be null");
            }
            
            String url = jdbcUrl.toLowerCase();
            if (url.startsWith("jdbc:mysql:")) return MYSQL;
            if (url.startsWith("jdbc:postgresql:")) return POSTGRESQL;
            if (url.startsWith("jdbc:h2:")) return H2;
            if (url.startsWith("jdbc:hsqldb:")) return HSQLDB;
            if (url.startsWith("jdbc:derby:")) return DERBY;
            if (url.startsWith("jdbc:oracle:")) return ORACLE;
            if (url.startsWith("jdbc:sqlserver:")) return SQL_SERVER;
            
            throw new IllegalArgumentException("Unsupported database type for URL: " + jdbcUrl);
        }
    }

    /**
     * Constructor with JDBC URL and credentials
     *
     * @param jdbcUrl  JDBC connection URL
     * @param username Database username
     * @param password Database password
     * @throws SQLException if database connection fails
     */
    public JdbcBasedPersistenceLayer(String jdbcUrl, String username, String password) 
            throws SQLException {
        this(jdbcUrl, username, password, new Properties());
    }

    /**
     * Constructor with JDBC URL, credentials, and connection properties
     *
     * @param jdbcUrl             JDBC connection URL
     * @param username            Database username
     * @param password            Database password
     * @param connectionProperties Additional connection properties
     * @throws SQLException if database connection fails
     */
    public JdbcBasedPersistenceLayer(String jdbcUrl, String username, String password, 
                                     Properties connectionProperties) throws SQLException {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.connectionProperties = new Properties(connectionProperties);
        this.databaseType = DatabaseType.fromJdbcUrl(jdbcUrl);
        initializeConnection();
    }

    /**
     * Constructor with DataSource (recommended for production use)
     *
     * @param dataSource DataSource instance
     * @param jdbcUrl    JDBC URL (for database type detection)
     * @throws SQLException if database connection fails
     */
    public JdbcBasedPersistenceLayer(DataSource dataSource, String jdbcUrl) throws SQLException {
        this.jdbcUrl = jdbcUrl;
        this.username = null;
        this.password = null;
        this.connectionProperties = new Properties();
        this.databaseType = DatabaseType.fromJdbcUrl(jdbcUrl);
        this.connection = dataSource.getConnection();
        this.connection.setAutoCommit(true);
        
        // Test connection
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(getTestQuery());
        }
        
        LOGGER.info("Successfully connected to {} database via DataSource", databaseType.getDisplayName());
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
            // Load the appropriate JDBC driver
            try {
                Class.forName(databaseType.getDriverClassName());
            } catch (ClassNotFoundException e) {
                String errorMsg = String.format(
                    "%s JDBC driver not found. Please ensure you have added the appropriate dependency to your project.",
                    databaseType.getDisplayName()
                );
                LOGGER.error(errorMsg);
                throw new SQLException(errorMsg, e);
            }

            // Create connection
            if (username != null && password != null) {
                connectionProperties.setProperty("user", username);
                connectionProperties.setProperty("password", password);
                connection = DriverManager.getConnection(jdbcUrl, connectionProperties);
            } else {
                connection = DriverManager.getConnection(jdbcUrl, connectionProperties);
            }
            
            connection.setAutoCommit(true);

            // Test connection
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(getTestQuery());
            }

            LOGGER.info("Successfully connected to {} database: {}", 
                       databaseType.getDisplayName(), jdbcUrl);

        } catch (SQLException e) {
            String errorMsg = String.format(
                "Failed to initialize %s database connection. Error: %s\nJDBC URL: %s",
                databaseType.getDisplayName(), e.getMessage(), jdbcUrl
            );
            LOGGER.error(errorMsg);
            throw new SQLException(errorMsg, e);
        }
    }

    /**
     * Gets appropriate test query for the database type
     */
    private String getTestQuery() {
        return switch (databaseType) {
            case ORACLE -> "SELECT 1 FROM DUAL";
            case SQL_SERVER -> "SELECT 1";
            default -> "SELECT 1";
        };
    }

    /**
     * Creates a new table in the database
     *
     * @param tableName        Name of the table
     * @param columns          Array of column definitions
     * @param uniqueConstraints Array of unique constraint column names
     * @param ifNotExists      If true, adds IF NOT EXISTS clause
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
        if (ifNotExists && supportsIfNotExists()) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(tableName).append(" (");

        // Add column definitions
        StringJoiner columnDefinitions = new StringJoiner(", ");
        for (ColumnDefinition column : columns) {
            columnDefinitions.add(adaptColumnDefinition(column));
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
            if (ifNotExists && !supportsIfNotExists()) {
                // Check if table exists first for databases that don't support IF NOT EXISTS
                if (tableExists(tableName)) {
                    LOGGER.debug("Table {} already exists, skipping creation", tableName);
                    return;
                }
            }
            stmt.execute(sql.toString());
            LOGGER.debug("Successfully created table: {}", tableName);
        }
    }

    /**
     * Adapts column definition for specific database types
     */
    private String adaptColumnDefinition(ColumnDefinition column) {
        StringBuilder sql = new StringBuilder(column.getName())
                .append(" ").append(adaptColumnType(column.getType()));

        if (column.isPrimaryKey()) {
            sql.append(" PRIMARY KEY");
            if (column.isAutoIncrement()) {
                sql.append(getAutoIncrementSyntax());
            }
        }
        
        if (column.isNotNull()) {
            sql.append(" NOT NULL");
        }

        if (column.isUnique()) {
            sql.append(" UNIQUE");
        }

        if (column.getDefaultValue() != null) {
            sql.append(" DEFAULT ").append(column.getDefaultValue());
        }
        
        return sql.toString();
    }

    /**
     * Adapts column types for specific databases
     */
    private String adaptColumnType(ColumnType columnType) {
        return switch (databaseType) {
            case MYSQL -> switch (columnType) {
                case INTEGER -> "INT";
                case TEXT -> "TEXT";
                case TEXT_SHORT -> "VARCHAR(255)";
                case TEXT_LONG -> "LONGTEXT";
                case REAL -> "DOUBLE";
                case BLOB -> "LONGBLOB";
                case BOOLEAN -> "BOOLEAN";
            };
            case POSTGRESQL -> switch (columnType) {
                case INTEGER -> "INTEGER";
                case TEXT -> "TEXT";
                case TEXT_SHORT -> "VARCHAR(255)";
                case TEXT_LONG -> "TEXT";
                case REAL -> "DOUBLE PRECISION";
                case BLOB -> "BYTEA";
                case BOOLEAN -> "BOOLEAN";
            };
            case ORACLE -> switch (columnType) {
                case INTEGER -> "NUMBER(10)";
                case TEXT -> "VARCHAR2(4000)";
                case TEXT_SHORT -> "VARCHAR2(255)";
                case TEXT_LONG -> "CLOB";
                case REAL -> "BINARY_DOUBLE";
                case BLOB -> "BLOB";
                case BOOLEAN -> "NUMBER(1)";
            };
            case SQL_SERVER -> switch (columnType) {
                case INTEGER -> "INT";
                case TEXT -> "NVARCHAR(450)"; // Indexable text
                case TEXT_SHORT -> "NVARCHAR(255)"; // Short indexable text
                case TEXT_LONG -> "NVARCHAR(MAX)"; // Large text content
                case REAL -> "FLOAT";
                case BLOB -> "VARBINARY(MAX)";
                case BOOLEAN -> "BIT";
            };
            default -> columnType.getSqlType();
        };
    }

    /**
     * Gets auto-increment syntax for specific databases
     */
    private String getAutoIncrementSyntax() {
        return switch (databaseType) {
            case MYSQL -> " AUTO_INCREMENT";
            case POSTGRESQL -> ""; // PostgreSQL uses SERIAL types
            case SQL_SERVER -> " IDENTITY(1,1)";
            case ORACLE -> ""; // Oracle uses sequences
            default -> " AUTOINCREMENT";
        };
    }

    /**
     * Checks if database supports IF NOT EXISTS clause
     */
    private boolean supportsIfNotExists() {
        return switch (databaseType) {
            case MYSQL, POSTGRESQL, H2, HSQLDB -> true;
            case ORACLE, SQL_SERVER, DERBY -> false;
        };
    }

    /**
     * Checks if table exists
     */
    private boolean tableExists(String tableName) throws SQLException {
        try (ResultSet rs = getConnection().getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
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

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int paramIndex = 1;
            for (Object value : data.values()) {
                setParameterWithType(stmt, paramIndex++, value);
            }
            int rowsAffected = stmt.executeUpdate();
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

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int paramIndex = 1;

            for (Object value : data.values()) {
                setParameterWithType(stmt, paramIndex++, value);
            }

            for (Object param : whereParams) {
                setParameterWithType(stmt, paramIndex++, param);
            }

            int rowsAffected = stmt.executeUpdate();
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

        PreparedStatement stmt = getConnection().prepareStatement(sql);
        if (whereParams != null) {
            for (int i = 0; i < whereParams.length; i++) {
                setParameterWithType(stmt, i + 1, whereParams[i]);
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
                setParameterWithType(stmt, i + 1, whereParams[i]);
            }

            int rowsAffected = stmt.executeUpdate();
            LOGGER.info("Deleted {} row(s) from table {}", rowsAffected, tableName);
            return rowsAffected;
        }
    }

    /**
     * Upsert operation - database-specific implementation
     */
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

        // Use native upsert for databases that support it
        if (databaseType == DatabaseType.MYSQL || databaseType == DatabaseType.POSTGRESQL || 
            databaseType == DatabaseType.SQL_SERVER || databaseType == DatabaseType.H2) {
            
            String sql = buildUpsertQuery(tableName, data, uniqueColumns, columnsToUpdate);
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                setUpsertParameters(stmt, data, uniqueColumns, columnsToUpdate);
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected;
            }
        } else {
            // Fallback: Try update first, then insert if no rows affected
            return performManualUpsert(tableName, data, uniqueColumns, updateColumns);
        }
    }

    /**
     * Manual upsert implementation for databases without native upsert support
     */
    private int performManualUpsert(String tableName, Map<String, Object> data, 
                                   String[] uniqueColumns, String[] updateColumns) throws SQLException {
        // Build WHERE clause for unique columns
        StringJoiner whereClause = new StringJoiner(" AND ");
        List<Object> whereParams = new ArrayList<>();
        for (String uniqueCol : uniqueColumns) {
            whereClause.add(uniqueCol + " = ?");
            whereParams.add(data.get(uniqueCol));
        }

        // Try update first
        Set<String> columnsToUpdate;
        if (updateColumns == null) {
            columnsToUpdate = new HashSet<>(data.keySet());
            columnsToUpdate.removeAll(Arrays.asList(uniqueColumns));
        } else {
            columnsToUpdate = new HashSet<>(Arrays.asList(updateColumns));
        }

        if (!columnsToUpdate.isEmpty()) {
            Map<String, Object> updateData = new HashMap<>();
            for (String column : columnsToUpdate) {
                updateData.put(column, data.get(column));
            }

            int rowsUpdated = update(tableName, updateData, whereClause.toString(), whereParams.toArray());
            if (rowsUpdated > 0) {
                return rowsUpdated;
            }
        }

        // If no rows were updated, try insert
        try {
            return insert(tableName, data);
        } catch (SQLException e) {
            // If insert fails due to unique constraint, that's expected in concurrent scenarios
            // Try update one more time in case another thread inserted the record
            if (!columnsToUpdate.isEmpty()) {
                Map<String, Object> updateData = new HashMap<>();
                for (String column : columnsToUpdate) {
                    updateData.put(column, data.get(column));
                }
                return update(tableName, updateData, whereClause.toString(), whereParams.toArray());
            }
            throw e;
        }
    }

    /**
     * Builds database-specific upsert query
     */
    private String buildUpsertQuery(String tableName, Map<String, Object> data, 
                                   String[] uniqueColumns, Set<String> columnsToUpdate) {
        return switch (databaseType) {
            case MYSQL -> buildMySQLUpsertQuery(tableName, data, uniqueColumns, columnsToUpdate);
            case POSTGRESQL -> buildPostgreSQLUpsertQuery(tableName, data, uniqueColumns, columnsToUpdate);
            case SQL_SERVER -> buildSQLServerMergeQuery(tableName, data, uniqueColumns, columnsToUpdate);
            default -> buildGenericUpsertQuery(tableName, data, uniqueColumns, columnsToUpdate);
        };
    }

    private String buildMySQLUpsertQuery(String tableName, Map<String, Object> data, 
                                        String[] uniqueColumns, Set<String> columnsToUpdate) {
        StringBuilder sql = new StringBuilder()
                .append("INSERT INTO ").append(tableName)
                .append(" (").append(String.join(", ", data.keySet())).append(")")
                .append(" VALUES (").append(String.join(", ", Collections.nCopies(data.size(), "?")))
                .append(")")
                .append(" ON DUPLICATE KEY UPDATE ");

        StringJoiner updateClause = new StringJoiner(", ");
        for (String column : columnsToUpdate) {
            updateClause.add(column + " = VALUES(" + column + ")");
        }
        sql.append(updateClause.toString());
        return sql.toString();
    }

    private String buildPostgreSQLUpsertQuery(String tableName, Map<String, Object> data, 
                                             String[] uniqueColumns, Set<String> columnsToUpdate) {
        StringBuilder sql = new StringBuilder()
                .append("INSERT INTO ").append(tableName)
                .append(" (").append(String.join(", ", data.keySet())).append(")")
                .append(" VALUES (").append(String.join(", ", Collections.nCopies(data.size(), "?")))
                .append(")")
                .append(" ON CONFLICT(").append(String.join(", ", uniqueColumns)).append(")")
                .append(" DO UPDATE SET ");

        StringJoiner updateClause = new StringJoiner(", ");
        for (String column : columnsToUpdate) {
            updateClause.add(column + " = EXCLUDED." + column);
        }
        sql.append(updateClause.toString());
        return sql.toString();
    }

    private String buildSQLServerMergeQuery(String tableName, Map<String, Object> data, 
                                           String[] uniqueColumns, Set<String> columnsToUpdate) {
        // Build a proper SQL Server MERGE statement
        StringBuilder sql = new StringBuilder();
        sql.append("MERGE ").append(tableName).append(" AS target ");
        sql.append("USING (VALUES (");
        sql.append(String.join(", ", Collections.nCopies(data.size(), "?")));
        sql.append(")) AS source (").append(String.join(", ", data.keySet())).append(") ");
        
        // ON clause for matching
        sql.append("ON (");
        StringJoiner onClause = new StringJoiner(" AND ");
        for (String uniqueCol : uniqueColumns) {
            onClause.add("target." + uniqueCol + " = source." + uniqueCol);
        }
        sql.append(onClause.toString()).append(") ");
        
        // WHEN MATCHED (update)
        if (!columnsToUpdate.isEmpty()) {
            sql.append("WHEN MATCHED THEN UPDATE SET ");
            StringJoiner updateClause = new StringJoiner(", ");
            for (String column : columnsToUpdate) {
                updateClause.add(column + " = source." + column);
            }
            sql.append(updateClause.toString()).append(" ");
        }
        
        // WHEN NOT MATCHED (insert)
        sql.append("WHEN NOT MATCHED THEN INSERT (");
        sql.append(String.join(", ", data.keySet()));
        sql.append(") VALUES (");
        StringJoiner insertValues = new StringJoiner(", ");
        for (String column : data.keySet()) {
            insertValues.add("source." + column);
        }
        sql.append(insertValues.toString()).append(");");
        
        return sql.toString();
    }

    private String buildGenericUpsertQuery(String tableName, Map<String, Object> data, 
                                          String[] uniqueColumns, Set<String> columnsToUpdate) {
        // For databases that support INSERT OR REPLACE (H2)
        if (databaseType == DatabaseType.H2 || databaseType == DatabaseType.HSQLDB) {
            StringBuilder sql = new StringBuilder()
                    .append("MERGE INTO ").append(tableName)
                    .append(" USING (VALUES (")
                    .append(String.join(", ", Collections.nCopies(data.size(), "?")))
                    .append(")) AS source (").append(String.join(", ", data.keySet())).append(") ON (");
            
            StringJoiner onClause = new StringJoiner(" AND ");
            for (String uniqueCol : uniqueColumns) {
                onClause.add(tableName + "." + uniqueCol + " = source." + uniqueCol);
            }
            sql.append(onClause.toString()).append(")");
            
            if (!columnsToUpdate.isEmpty()) {
                sql.append(" WHEN MATCHED THEN UPDATE SET ");
                StringJoiner updateClause = new StringJoiner(", ");
                for (String column : columnsToUpdate) {
                    updateClause.add(column + " = source." + column);
                }
                sql.append(updateClause.toString());
            }
            
            sql.append(" WHEN NOT MATCHED THEN INSERT (")
                    .append(String.join(", ", data.keySet()))
                    .append(") VALUES (");
            StringJoiner insertValues = new StringJoiner(", ");
            for (String column : data.keySet()) {
                insertValues.add("source." + column);
            }
            sql.append(insertValues.toString()).append(")");
            
            return sql.toString();
        } else {
            // Fallback: Just insert (will fail on duplicates, but that's expected for unsupported DBs)
            StringBuilder sql = new StringBuilder()
                    .append("INSERT INTO ").append(tableName)
                    .append(" (").append(String.join(", ", data.keySet())).append(")")
                    .append(" VALUES (").append(String.join(", ", Collections.nCopies(data.size(), "?")))
                    .append(")");
            return sql.toString();
        }
    }

    /**
     * Sets parameters for upsert query
     */
    private void setUpsertParameters(PreparedStatement stmt, Map<String, Object> data, 
                                    String[] uniqueColumns, Set<String> columnsToUpdate) throws SQLException {
        int paramIndex = 1;
        
        switch (databaseType) {
            case MYSQL, POSTGRESQL -> {
                // Standard INSERT ... ON CONFLICT/DUPLICATE KEY
                for (Object value : data.values()) {
                    setParameterWithType(stmt, paramIndex++, value);
                }
            }
            default -> {
                // Generic approach
                for (Object value : data.values()) {
                    setParameterWithType(stmt, paramIndex++, value);
                }
            }
        }
    }

    /**
     * Sets parameter with proper type handling for different databases
     */
    private void setParameterWithType(PreparedStatement stmt, int paramIndex, Object value) throws SQLException {
        if (value == null) {
            // Handle null values explicitly with proper SQL type
            switch (databaseType) {
                case SQL_SERVER -> stmt.setNull(paramIndex, java.sql.Types.NVARCHAR);
                case ORACLE -> stmt.setNull(paramIndex, java.sql.Types.VARCHAR);
                default -> stmt.setNull(paramIndex, java.sql.Types.VARCHAR);
            }
        } else if (value instanceof String) {
            stmt.setString(paramIndex, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(paramIndex, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(paramIndex, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(paramIndex, (Double) value);
        } else if (value instanceof Float) {
            stmt.setFloat(paramIndex, (Float) value);
        } else if (value instanceof Boolean) {
            switch (databaseType) {
                case SQL_SERVER, ORACLE -> stmt.setInt(paramIndex, (Boolean) value ? 1 : 0);
                default -> stmt.setBoolean(paramIndex, (Boolean) value);
            }
        } else if (value instanceof java.sql.Timestamp) {
            stmt.setTimestamp(paramIndex, (java.sql.Timestamp) value);
        } else if (value instanceof java.sql.Date) {
            stmt.setDate(paramIndex, (java.sql.Date) value);
        } else if (value instanceof java.time.Instant) {
            stmt.setTimestamp(paramIndex, java.sql.Timestamp.from((java.time.Instant) value));
        } else if (value instanceof java.time.LocalDateTime) {
            stmt.setTimestamp(paramIndex, java.sql.Timestamp.valueOf((java.time.LocalDateTime) value));
        } else if (value instanceof Enum<?>) {
            stmt.setString(paramIndex, value.toString());
        } else if (value instanceof byte[]) {
            stmt.setBytes(paramIndex, (byte[]) value);
        } else {
            // For complex objects, convert to string
            stmt.setString(paramIndex, value.toString());
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
                        setParameterWithType(stmt, paramIndex++, data.get(key));
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
                        setParameterWithType(stmt, paramIndex++, value);
                    }

                    for (Object param : update.getWhereParams()) {
                        setParameterWithType(stmt, paramIndex++, param);
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

        // Use native batch upsert for databases that support it
        if (databaseType == DatabaseType.MYSQL || databaseType == DatabaseType.POSTGRESQL || 
            databaseType == DatabaseType.SQL_SERVER || databaseType == DatabaseType.H2) {
            
            String sql = buildUpsertQuery(tableName, firstRecord, uniqueColumns, columnsToUpdate);

            Connection conn = getConnection();
            boolean originalAutoCommit = conn.getAutoCommit();
            
            try {
                conn.setAutoCommit(false); // Start transaction
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (Map<String, Object> data : dataList) {
                        setUpsertParameters(stmt, data, uniqueColumns, columnsToUpdate);
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
        } else {
            // Fallback: Manual upsert for each record (slower but works)
            int totalAffected = 0;
            for (Map<String, Object> data : dataList) {
                totalAffected += performManualUpsert(tableName, data, uniqueColumns, updateColumns);
            }
            LOGGER.debug("Batch upserted {} records in table {} (manual mode)", totalAffected, tableName);
            return totalAffected;
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
     * Get the current database type
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * Get the JDBC URL
     */
    public String getJdbcUrl() {
        return jdbcUrl;
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