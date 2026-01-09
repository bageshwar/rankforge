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

/**
 * This class represents a column in db
 * Author bageshwar.pn
 * Date 10/11/24
 */
public class ColumnDefinition {
    private final String name;
    private final ColumnType type;
    private final boolean primaryKey;
    private final boolean autoIncrement;
    private final boolean notNull;
    private final boolean unique;
    private final String defaultValue;

    private ColumnDefinition(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.primaryKey = builder.primaryKey;
        this.autoIncrement = builder.autoIncrement;
        this.notNull = builder.notNull;
        this.unique = builder.unique;
        this.defaultValue = builder.defaultValue;
    }

    String toSqlDefinition() {
        StringBuilder sql = new StringBuilder(name)
                .append(" ").append(type.getSqlType());

        if (autoIncrement && primaryKey) {
            // For H2: Use AUTO_INCREMENT before PRIMARY KEY
            // Syntax: INTEGER AUTO_INCREMENT PRIMARY KEY
            sql.append(" AUTO_INCREMENT PRIMARY KEY");
        } else if (primaryKey) {
            sql.append(" PRIMARY KEY");
        }
        if (notNull) {
            sql.append(" NOT NULL");
        }

        if (unique) {
            sql.append(" UNIQUE");
        }

        if (defaultValue != null) {
            sql.append(" DEFAULT ").append(defaultValue);
        }
        return sql.toString();
    }

    // Getter methods
    public String getName() {
        return name;
    }

    public ColumnType getType() {
        return type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public boolean isUnique() {
        return unique;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public static class Builder {
        private final String name;
        private final ColumnType type;
        private boolean primaryKey = false;
        private boolean autoIncrement = false;
        private boolean notNull = false;
        private boolean unique = false;
        private String defaultValue = null;

        public Builder(String name, ColumnType type) {
            this.name = name;
            this.type = type;
        }

        public Builder primaryKey() {
            this.primaryKey = true;
            return this;
        }

        public Builder autoIncrement() {
            this.autoIncrement = true;
            return this;
        }

        public Builder notNull() {
            this.notNull = true;
            return this;
        }

        public Builder unique() {
            this.unique = true;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public ColumnDefinition build() {
            return new ColumnDefinition(this);
        }
    }
}
