/*
 *
 *  *Copyright [2026] [Bageshwar Pratap Narain]
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

package com.rankforge.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory class for creating properly configured ObjectMapper instances.
 * Ensures consistent configuration across the application, including support
 * for Java 8 time types (Instant, LocalDateTime, etc.).
 * 
 * Author bageshwar.pn
 * Date 2026
 */
public class ObjectMapperFactory {
    
    /**
     * Creates a new ObjectMapper instance with standard configuration.
     * This includes:
     * - JavaTimeModule for Java 8 time type support (Instant, LocalDateTime, etc.)
     * 
     * @return A new configured ObjectMapper instance
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
