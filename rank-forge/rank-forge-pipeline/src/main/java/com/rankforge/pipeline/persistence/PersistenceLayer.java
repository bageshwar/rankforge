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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This interface represents a generic persistence layer.
 * Author bageshwar.pn
 * Date 10/11/24
 */
public interface PersistenceLayer extends AutoCloseable {
    /**
     * Creates a new table in the database
     *
     * @param tableName   Name of the table
     * @param columns     Array of column definitions
     * @param ifNotExists If true, adds IF NOT EXISTS clause
     * @throws SQLException if table creation fails
     */
    void createTable(String tableName,
                     ColumnDefinition[] columns,
                     String[] uniqueConstraints,
                     boolean ifNotExists) throws SQLException;

    /**
     * Inserts data into specified table
     *
     * @param tableName Name of the table
     * @param data      Map of column names to values
     * @return number of rows affected
     * @throws SQLException if insert operation fails
     */
    int insert(String tableName, Map<String, Object> data) throws SQLException;

    /**
     * Updates data in specified table based on condition
     *
     * @param tableName   Name of the table
     * @param data        Map of column names to new values
     * @param whereClause WHERE clause for update (without 'WHERE' keyword)
     * @param whereParams Parameters for WHERE clause
     * @return number of rows affected
     * @throws SQLException if update operation fails
     */
    int update(String tableName, Map<String, Object> data,
               String whereClause, Object... whereParams) throws SQLException;

    /**
     * Performs an upsert (insert or update) operation
     *
     * @param tableName     Name of the table
     * @param data          Map of column names to values
     * @param uniqueColumns Array of column names that form the unique constraint
     * @param updateColumns Array of column names to update on conflict (null means update all)
     * @return number of rows affected
     * @throws SQLException if upsert operation fails
     */
    int upsert(String tableName,
               Map<String, Object> data,
               String[] uniqueColumns,
               String[] updateColumns) throws SQLException;

    /**
     * Queries data from specified table
     *
     * @param tableName   Name of the table
     * @param columns     Columns to select
     * @param whereClause WHERE clause (without 'WHERE' keyword)
     * @param whereParams Parameters for WHERE clause
     * @return ResultSet containing query results
     * @throws SQLException if query fails
     */
    ResultSet query(String tableName, String[] columns,
                    String whereClause, Object... whereParams) throws SQLException;

    /**
     * Deletes data from specified table
     *
     * @param tableName   Name of the table
     * @param whereClause WHERE clause (without 'WHERE' keyword)
     * @param whereParams Parameters for WHERE clause
     * @return number of rows affected
     * @throws SQLException if delete operation fails
     */
    int delete(String tableName, String whereClause,
               Object... whereParams) throws SQLException;

    /**
     * Batch insert multiple records into specified table
     *
     * @param tableName Name of the table
     * @param dataList  List of maps containing column names to values
     * @return number of rows affected
     * @throws SQLException if batch insert operation fails
     */
    int batchInsert(String tableName, List<Map<String, Object>> dataList) throws SQLException;

    /**
     * Batch update multiple records in specified table based on conditions
     *
     * @param tableName Name of the table
     * @param updates   List of BatchUpdateOperation containing data and conditions
     * @return number of rows affected
     * @throws SQLException if batch update operation fails
     */
    int batchUpdate(String tableName, List<BatchUpdateOperation> updates) throws SQLException;

    /**
     * Batch upsert (insert or update) multiple records
     *
     * @param tableName     Name of the table
     * @param dataList      List of maps containing column names to values
     * @param uniqueColumns Array of column names that form the unique constraint
     * @param updateColumns Array of column names to update on conflict (null means update all)
     * @return number of rows affected
     * @throws SQLException if batch upsert operation fails
     */
    int batchUpsert(String tableName,
                    List<Map<String, Object>> dataList,
                    String[] uniqueColumns,
                    String[] updateColumns) throws SQLException;

    /**
     * Helper class for batch update operations
     */
    class BatchUpdateOperation {
        private final Map<String, Object> data;
        private final String whereClause;
        private final Object[] whereParams;

        public BatchUpdateOperation(Map<String, Object> data, String whereClause, Object... whereParams) {
            this.data = data;
            this.whereClause = whereClause;
            this.whereParams = whereParams;
        }

        public Map<String, Object> getData() { return data; }
        public String getWhereClause() { return whereClause; }
        public Object[] getWhereParams() { return whereParams; }
    }
}
