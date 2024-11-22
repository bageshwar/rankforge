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
 * This enum is used for representing a column type
 * Author bageshwar.pn
 * Date 10/11/24
 */
public enum ColumnType {
    INTEGER("INTEGER"),
    TEXT("TEXT"),
    REAL("REAL"),
    BLOB("BLOB"),
    BOOLEAN("INTEGER"); // SQLite doesn't have a native boolean type

    private final String sqlType;

    ColumnType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getSqlType() {
        return sqlType;
    }
}
