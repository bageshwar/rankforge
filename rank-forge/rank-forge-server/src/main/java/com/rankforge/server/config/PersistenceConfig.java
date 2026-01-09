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

package com.rankforge.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rankforge.core.stores.PlayerStatsStore;
import com.rankforge.pipeline.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

/**
 * Configuration for persistence layer beans.
 * Supports JDBC persistence based on configuration.
 * 
 * Author bageshwar.pn
 * Date [Current Date]
 */
@Configuration
public class PersistenceConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceConfig.class);

    @Value("${rankforge.persistence.jdbc.url}")
    private String jdbcUrl;

    @Value("${rankforge.persistence.jdbc.username}")
    private String jdbcUsername;

    @Value("${rankforge.persistence.jdbc.password}")
    private String jdbcPassword;

    /**
     * JDBC persistence layer bean
     */
    @Bean
    @ConditionalOnProperty(name = "rankforge.persistence.type", havingValue = "jdbc")
    public PersistenceLayer jdbcPersistenceLayer() throws SQLException {

        LOGGER.info("Initializing JDBC persistence layer with jdbcURL: {}, username: {}",
                jdbcUrl, jdbcUsername);
        return new JdbcBasedPersistenceLayer(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /**
     * Jackson ObjectMapper bean for JSON serialization
     * Configured to support Java 8 time types (Instant, LocalDateTime, etc.)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Player stats store that uses the configured persistence layer
     */
    //@Bean
    public PlayerStatsStore playerStatsStore(PersistenceLayer persistenceLayer, ObjectMapper objectMapper) {
        LOGGER.info("Initializing PlayerStatsStore with persistence layer: {}", 
                persistenceLayer.getClass().getSimpleName());
        return new DBBasedPlayerStatsStore(persistenceLayer, objectMapper);
    }
}