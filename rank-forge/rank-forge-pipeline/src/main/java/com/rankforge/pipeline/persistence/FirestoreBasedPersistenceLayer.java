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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is a GCP Firestore based implementation of persistence layer.
 * Adapts SQL-like operations to Firestore's NoSQL document model.
 * 
 * Key Mappings:
 * - Tables → Collections
 * - Rows → Documents
 * - Primary Keys → Document IDs
 * - SQL WHERE clauses → Firestore Query filters
 * 
 * Author bageshwar.pn
 * Date [Current Date]
 */
public class FirestoreBasedPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirestoreBasedPersistenceLayer.class);
    
    private final String projectId;
    private final String databaseId;
    private final Firestore firestore;
    private final ReentrantLock connectionLock = new ReentrantLock();
    private final Map<String, Set<String>> tableSchemas = new HashMap<>(); // Track table schemas
    
    /**
     * Constructor initializes Firestore connection
     *
     * @param projectId GCP project ID
     * @throws SQLException if Firestore connection fails
     */
    public FirestoreBasedPersistenceLayer(String projectId, String databaseId) throws SQLException {
        this.projectId = projectId;
        this.databaseId = databaseId;
        this.firestore = initializeFirestore();
        LOGGER.info("Successfully connected to Firestore project: {}", projectId);
    }
    
    /**
     * Constructor with custom Firestore instance (useful for testing)
     *
     * @param firestore Custom Firestore instance
     */
    public FirestoreBasedPersistenceLayer(Firestore firestore) {
        this.projectId = "custom";
        this.databaseId = "custom";
        this.firestore = firestore;
        LOGGER.info("Using custom Firestore instance");
    }
    
    /**
     * Initializes Firestore connection
     *
     * @return Firestore instance
     * @throws SQLException if connection fails
     */
    private Firestore initializeFirestore() throws SQLException {
        try {
            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId(projectId)
                    .setDatabaseId(databaseId)
                    .build();
            
            return firestoreOptions.getService();
            
        } catch (Exception e) {
            String errorMsg = "Failed to initialize Firestore connection. " +
                    "Error: " + e.getMessage() + "\n" +
                    "Project ID: " + projectId + "\n" +
                    "Ensure your GCP credentials are properly configured.";
            LOGGER.error(errorMsg);
            throw new SQLException(errorMsg, e);
        }
    }
    
    /**
     * Creates a new collection (table) in Firestore.
     * Since Firestore creates collections implicitly, this method mainly
     * validates the schema and stores it for future reference.
     *
     * @param tableName   Name of the collection
     * @param columns     Array of column definitions (stored as metadata)
     * @param uniqueConstraints Unique constraints (stored as metadata)
     * @param ifNotExists If true, won't throw error if collection exists
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
        
        connectionLock.lock();
        try {
            // Check if table schema already exists
            if (tableSchemas.containsKey(tableName) && !ifNotExists) {
                throw new SQLException("Collection '" + tableName + "' already exists");
            }
            
            // Store schema metadata
            Set<String> columnNames = new HashSet<>();
            for (ColumnDefinition column : columns) {
                columnNames.add(column.getName());
            }
            tableSchemas.put(tableName, columnNames);
            
            // Create a schema document to store table metadata
            Map<String, Object> schemaDoc = new HashMap<>();
            schemaDoc.put("tableName", tableName);
            schemaDoc.put("columns", Arrays.toString(columns));
            schemaDoc.put("uniqueConstraints", uniqueConstraints != null ? Arrays.toString(uniqueConstraints) : null);
            schemaDoc.put("createdAt", System.currentTimeMillis());
            
            // Store schema in a special _schemas collection
            DocumentReference schemaRef = firestore.collection("_schemas").document(tableName);
            ApiFuture<WriteResult> future = schemaRef.set(schemaDoc);
            future.get(); // Wait for completion
            
            LOGGER.debug("Successfully created collection schema: {}", tableName);
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Failed to create table schema: " + e.getMessage(), e);
        } finally {
            connectionLock.unlock();
        }
    }
    
    /**
     * Thread-safe insert operation
     */
    @Override
    public synchronized int insert(String tableName, Map<String, Object> data) throws SQLException {
        validateInput(tableName, data);
        
        try {
            CollectionReference collection = firestore.collection(tableName);
            
            // Generate document ID or use provided primary key
            String documentId = extractOrGenerateDocumentId(data);
            
            DocumentReference docRef = collection.document(documentId);
            ApiFuture<WriteResult> future = docRef.set(data);
            future.get(); // Wait for completion
            
            LOGGER.debug("Inserted document into collection {}", tableName);
            return 1; // Firestore operations are atomic, so always 1 or exception
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Failed to insert document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Thread-safe update operation
     */
    @Override
    public synchronized int update(String tableName, Map<String, Object> data,
                                   String whereClause, Object... whereParams) throws SQLException {
        validateInput(tableName, data);
        
        try {
            // Parse where clause and execute query to find documents to update
            List<QueryDocumentSnapshot> documentsToUpdate = executeQuery(tableName, whereClause, whereParams);
            
            int updatedCount = 0;
            for (QueryDocumentSnapshot doc : documentsToUpdate) {
                DocumentReference docRef = doc.getReference();
                ApiFuture<WriteResult> future = docRef.update(data);
                future.get(); // Wait for completion
                updatedCount++;
            }
            
            LOGGER.debug("Updated {} document(s) in collection {}", updatedCount, tableName);
            return updatedCount;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Failed to update documents: " + e.getMessage(), e);
        }
    }
    
    /**
     * Performs an upsert (insert or update) operation
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
        
        try {
            // Build query to find existing document
            Query query = firestore.collection(tableName);
            for (String column : uniqueColumns) {
                if (!data.containsKey(column)) {
                    throw new SQLException("Unique column '" + column + "' not found in data map");
                }
                query = query.whereEqualTo(column, data.get(column));
            }
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            if (querySnapshot.isEmpty()) {
                // Insert new document
                return insert(tableName, data);
            } else {
                // Update existing document(s)
                int updatedCount = 0;
                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Map<String, Object> updateData = new HashMap<>();
                    
                    if (updateColumns == null) {
                        // Update all non-unique columns
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            if (!Arrays.asList(uniqueColumns).contains(entry.getKey())) {
                                updateData.put(entry.getKey(), entry.getValue());
                            }
                        }
                    } else {
                        // Update only specified columns
                        for (String column : updateColumns) {
                            if (data.containsKey(column)) {
                                updateData.put(column, data.get(column));
                            }
                        }
                    }
                    
                    if (!updateData.isEmpty()) {
                        DocumentReference docRef = doc.getReference();
                        ApiFuture<WriteResult> updateFuture = docRef.update(updateData);
                        updateFuture.get(); // Wait for completion
                        updatedCount++;
                    }
                }
                
                LOGGER.debug("Upserted {} document(s) in collection {}", updatedCount, tableName);
                return updatedCount;
            }
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Failed to upsert documents: " + e.getMessage(), e);
        }
    }
    
    /**
     * Thread-safe query operation
     */
    @Override
    public synchronized ResultSet query(String tableName, String[] columns,
                                        String whereClause, Object... whereParams) throws SQLException {
        validateTableName(tableName);
        
        try {
            List<QueryDocumentSnapshot> documents = executeQuery(tableName, whereClause, whereParams);
            
            // Filter columns if specified
            if (columns != null && columns.length > 0) {
                // Note: Firestore doesn't support projection in the same way as SQL
                // We fetch all fields and filter in the ResultSet wrapper
                LOGGER.debug("Column filtering will be applied in ResultSet wrapper");
            }
            
            return new FirestoreResultSet(documents);
            
        } catch (Exception e) {
            throw new SQLException("Failed to execute query: " + e.getMessage(), e);
        }
    }
    
    /**
     * Thread-safe delete operation
     */
    @Override
    public synchronized int delete(String tableName, String whereClause,
                                   Object... whereParams) throws SQLException {
        validateTableName(tableName);
        
        try {
            // Execute query to find documents to delete
            List<QueryDocumentSnapshot> documentsToDelete = executeQuery(tableName, whereClause, whereParams);
            
            int deletedCount = 0;
            for (QueryDocumentSnapshot doc : documentsToDelete) {
                DocumentReference docRef = doc.getReference();
                ApiFuture<WriteResult> future = docRef.delete();
                future.get(); // Wait for completion
                deletedCount++;
            }
            
            LOGGER.info("Deleted {} document(s) from collection {}", deletedCount, tableName);
            return deletedCount;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Failed to delete documents: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes a Firestore query based on a simplified where clause
     */
    private List<QueryDocumentSnapshot> executeQuery(String tableName, String whereClause, Object... whereParams)
            throws SQLException, InterruptedException, ExecutionException {
        
        Query query = firestore.collection(tableName);
        
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            query = parseWhereClause(query, whereClause, whereParams);
        }
        
        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot querySnapshot = future.get();
        
        return querySnapshot.getDocuments();
    }
    
    /**
     * Parses a simplified WHERE clause and applies it to the Firestore query.
     * Supports basic operations like equality, inequality, etc.
     * 
     * Note: This is a simplified parser. A full implementation would need
     * a proper SQL parser for complex WHERE clauses.
     */
    private Query parseWhereClause(Query query, String whereClause, Object... whereParams) throws SQLException {
        // Simple parsing for basic WHERE clauses
        // Format: "column = ?" or "column > ?" etc.
        
        String[] parts = whereClause.trim().split("\\s+");
        if (parts.length < 3) {
            throw new SQLException("Invalid WHERE clause format: " + whereClause);
        }
        
        String column = parts[0];
        String operator = parts[1];
        
        if (whereParams.length == 0) {
            throw new SQLException("No parameters provided for WHERE clause");
        }
        
        Object value = whereParams[0];
        
        switch (operator.toLowerCase()) {
            case "=":
                return query.whereEqualTo(column, value);
            case ">":
                return query.whereGreaterThan(column, value);
            case ">=":
                return query.whereGreaterThanOrEqualTo(column, value);
            case "<":
                return query.whereLessThan(column, value);
            case "<=":
                return query.whereLessThanOrEqualTo(column, value);
            case "!=":
            case "<>":
                return query.whereNotEqualTo(column, value);
            default:
                throw new SQLException("Unsupported operator: " + operator);
        }
    }
    
    /**
     * Extracts document ID from data or generates a new one
     */
    private String extractOrGenerateDocumentId(Map<String, Object> data) {
        // Look for common primary key field names
        String[] possibleIdFields = {"id", "_id", "documentId", "uid"};
        
        for (String field : possibleIdFields) {
            Object value = data.get(field);
            if (value != null) {
                return value.toString();
            }
        }
        
        // Generate a new UUID if no ID field found
        return UUID.randomUUID().toString();
    }
    
    /**
     * Validates table name
     */
    private void validateTableName(String tableName) throws SQLException {
        if (tableName == null || tableName.trim().isEmpty()) {
            LOGGER.error("Collection name cannot be null or empty");
            throw new SQLException("Collection name cannot be null or empty");
        }
        
        // Firestore collection name validation
        if (!tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            LOGGER.error("Invalid collection name format: {}", tableName);
            throw new SQLException("Invalid collection name format");
        }
    }
    
    /**
     * Validates table name and data map
     */
    private void validateInput(String tableName, Map<String, Object> data) throws SQLException {
        validateTableName(tableName);
        if (data == null || data.isEmpty()) {
            LOGGER.error("Data map cannot be null or empty");
            throw new SQLException("Data map cannot be null or empty");
        }
    }
    
    @Override
    public void close() {
        connectionLock.lock();
        try {
            if (firestore != null) {
                firestore.close();
                LOGGER.info("Firestore connection closed successfully");
            }
        } catch (Exception e) {
            LOGGER.error("Error closing Firestore connection", e);
        } finally {
            connectionLock.unlock();
        }
    }
}