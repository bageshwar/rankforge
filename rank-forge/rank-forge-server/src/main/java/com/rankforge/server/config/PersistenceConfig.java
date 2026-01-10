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
import com.rankforge.core.stores.EventStore;
import com.rankforge.core.stores.PlayerStatsStore;
import com.rankforge.core.util.ObjectMapperFactory;
import com.rankforge.pipeline.persistence.AccoladeStore;
import com.rankforge.pipeline.persistence.EventProcessingContext;
import com.rankforge.pipeline.persistence.JpaEventStore;
import com.rankforge.pipeline.persistence.JpaPlayerStatsStore;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Configuration for persistence layer beans using Spring Data JPA.
 * 
 * Author bageshwar.pn  
 * Date 2026
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.rankforge.pipeline.persistence.repository")
public class PersistenceConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceConfig.class);

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    // DataSource is auto-configured by Spring Boot from spring.datasource.* properties
    // No explicit bean needed unless custom configuration is required

    /**
     * EntityManagerFactory bean for JPA
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.rankforge.pipeline.persistence.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        Properties properties = new Properties();
        // Set ddl-auto from configuration (update for local, validate for production)
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        LOGGER.info("Hibernate DDL auto mode: {}", ddlAuto);
        
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.jdbc.batch_size", "50");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        em.setJpaProperties(properties);
        
        return em;
    }

    /**
     * TransactionManager for JPA
     */
    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    /**
     * Jackson ObjectMapper bean for JSON serialization
     * Configured to support Java 8 time types (Instant, LocalDateTime, etc.)
     */
    @Bean
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.createObjectMapper();
    }
    
    /**
     * EventProcessingContext bean - shared context for game/round/accolade tracking
     * Note: EventProcessingContext is also a @Component, but we need to ensure a single instance
     */
    @Bean
    public EventProcessingContext eventProcessingContext() {
        LOGGER.info("Initializing EventProcessingContext");
        return new EventProcessingContext();
    }
    
    /**
     * JPA EventStore bean
     */
    @Bean
    public EventStore jpaEventStore(GameEventRepository gameEventRepository, 
                                     AccoladeRepository accoladeRepository,
                                     GameRepository gameRepository,
                                     ObjectMapper objectMapper,
                                     EventProcessingContext eventProcessingContext) {
        LOGGER.info("Initializing JPA EventStore with EventProcessingContext and GameRepository");
        return new JpaEventStore(gameEventRepository, accoladeRepository, gameRepository, 
                objectMapper, eventProcessingContext);
    }
    
    /**
     * JPA PlayerStatsStore bean
     */
    @Bean
    public PlayerStatsStore jpaPlayerStatsStore(PlayerStatsRepository playerStatsRepository) {
        LOGGER.info("Initializing JPA PlayerStatsStore");
        return new JpaPlayerStatsStore(playerStatsRepository);
    }
    
    /**
     * JPA AccoladeStore bean
     */
    @Bean
    public AccoladeStore jpaAccoladeStore(AccoladeRepository accoladeRepository,
                                          EventProcessingContext eventProcessingContext) {
        LOGGER.info("Initializing JPA AccoladeStore with EventProcessingContext");
        return new AccoladeStore(accoladeRepository, eventProcessingContext);
    }
}
