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

import com.rankforge.core.events.GameProcessedEvent;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaPlayerStatsStore
 * Tests the unique constraint violation issue when storing multiple games for the same player
 */
@ExtendWith(MockitoExtension.class)
class JpaPlayerStatsStoreTest {

    @Mock
    private PlayerStatsRepository repository;

    private JpaPlayerStatsStore store;

    @BeforeEach
    void setUp() {
        store = new JpaPlayerStatsStore(repository);
    }

    @Test
    void testStoreBatch_SamePlayerInMultipleGames_ShouldNotThrowUniqueConstraintViolation() {
        // Setup: Create player stats for the same player in two different games
        String playerId = "[U:1:1090227400]";
        Instant game1Timestamp = Instant.parse("2026-01-07T18:00:53.938698671Z");
        Instant game2Timestamp = Instant.parse("2026-01-07T19:00:53.938698671Z");

        PlayerStats stats1 = createPlayerStats(playerId, "Player1", 10, 5, game1Timestamp);
        PlayerStats stats2 = createPlayerStats(playerId, "Player1", 15, 8, game2Timestamp);

        // First game: no existing record
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<PlayerStatsEntity> entities = invocation.getArgument(0);
            return entities; // Simulate successful save
        });

        // Store stats and process first game
        store.store(stats1, false);
        GameProcessedEvent event1 = new GameProcessedEvent(game1Timestamp, new HashMap<>());
        store.onGameEnded(event1);
        
        // Verify first game was saved with gameTimestamp
        ArgumentCaptor<List<PlayerStatsEntity>> captor1 = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(captor1.capture());
        List<PlayerStatsEntity> savedEntities1 = captor1.getValue();
        assertEquals(1, savedEntities1.size());
        assertEquals(playerId, savedEntities1.get(0).getPlayerId());
        assertEquals(game1Timestamp, savedEntities1.get(0).getGameTimestamp(), 
                "First game should have gameTimestamp set");

        // Reset mocks for second game
        reset(repository);
        
        // Second game: should be able to insert even though player exists
        // This should NOT throw DataIntegrityViolationException
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<PlayerStatsEntity> entities = invocation.getArgument(0);
            // Verify gameTimestamp is set
            for (PlayerStatsEntity entity : entities) {
                assertNotNull(entity.getGameTimestamp(), "gameTimestamp must be set");
            }
            return entities; // Simulate successful save
        });

        // Store stats and process second game - this should work without throwing
        store.store(stats2, false);
        
        // This should NOT throw an exception even though the same playerId exists
        assertDoesNotThrow(() -> {
            GameProcessedEvent event2 = new GameProcessedEvent(game2Timestamp, new HashMap<>());
            store.onGameEnded(event2);
        });

        // Verify second game was also saved with different gameTimestamp
        ArgumentCaptor<List<PlayerStatsEntity>> captor2 = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(captor2.capture());
        List<PlayerStatsEntity> savedEntities2 = captor2.getValue();
        assertEquals(1, savedEntities2.size());
        assertEquals(playerId, savedEntities2.get(0).getPlayerId());
        assertEquals(game2Timestamp, savedEntities2.get(0).getGameTimestamp(),
                "Second game should have different gameTimestamp");
    }

    @Test
    void testStoreBatch_DuplicatePlayerInSameBatch_ShouldDeduplicate() {
        // Setup: Same player appears twice in the same batch
        String playerId = "[U:1:1090227400]";
        Instant gameTimestamp = Instant.parse("2026-01-07T18:00:53.938698671Z");

        PlayerStats stats1 = createPlayerStats(playerId, "Player1", 10, 5, gameTimestamp);
        PlayerStats stats2 = createPlayerStats(playerId, "Player1", 12, 6, gameTimestamp.plusSeconds(10));

        // Store both in the map (simulating what happens in real scenario)
        store.store(stats1, false);
        store.store(stats2, false);

        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Process game end - should deduplicate and only save one record
        GameProcessedEvent event = new GameProcessedEvent(gameTimestamp, new HashMap<>());
        store.onGameEnded(event);

        // Verify only one entity was saved (deduplicated)
        ArgumentCaptor<List<PlayerStatsEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(captor.capture());
        List<PlayerStatsEntity> savedEntities = captor.getValue();
        assertEquals(1, savedEntities.size(), "Should deduplicate same player in same batch");
        assertEquals(playerId, savedEntities.get(0).getPlayerId());
    }

    @Test
    void testStoreBatch_WhenRepositoryThrowsDataIntegrityViolation_ShouldPropagate() {
        // Setup: Simulate the actual error scenario
        String playerId = "[U:1:1090227400]";
        Instant gameTimestamp = Instant.parse("2026-01-07T18:00:53.938698671Z");

        PlayerStats stats = createPlayerStats(playerId, "Player1", 10, 5, gameTimestamp);
        store.store(stats, false);

        // Mock repository to throw DataIntegrityViolationException (simulating unique constraint violation)
        when(repository.saveAll(anyList())).thenThrow(
            new DataIntegrityViolationException("Violation of UNIQUE KEY constraint")
        );

        // This should throw the exception (current behavior)
        GameProcessedEvent event = new GameProcessedEvent(gameTimestamp, new HashMap<>());
        assertThrows(DataIntegrityViolationException.class, () -> {
            store.onGameEnded(event);
        });
    }

    @Test
    void testStoreBatch_ReproducesRealIssue_SamePlayerIdInDifferentGames() {
        // This test reproduces the actual issue: same playerId, different gameTimestamps
        // The database has a unique constraint on playerId, but we want to store multiple records
        String playerId = "[U:1:1090227400]";
        Instant game1Timestamp = Instant.parse("2026-01-07T18:00:53.938698671Z");
        Instant game2Timestamp = Instant.parse("2026-01-07T19:00:53.938698671Z");

        PlayerStats stats1 = createPlayerStats(playerId, "Player1", 10, 5, game1Timestamp);
        PlayerStats stats2 = createPlayerStats(playerId, "Player1", 15, 8, game2Timestamp);

        // Simulate first game being saved successfully
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<PlayerStatsEntity> entities = invocation.getArgument(0);
            // Verify gameTimestamp is set
            assertNotNull(entities.get(0).getGameTimestamp(), "gameTimestamp must be set");
            return entities;
        });

        // First game
        store.store(stats1, false);
        GameProcessedEvent event1 = new GameProcessedEvent(game1Timestamp, new HashMap<>());
        store.onGameEnded(event1);

        // Reset and simulate second game - this should work even with same playerId
        reset(repository);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<PlayerStatsEntity> entities = invocation.getArgument(0);
            // Verify gameTimestamp is different
            assertEquals(game2Timestamp, entities.get(0).getGameTimestamp());
            // This should NOT throw unique constraint violation because gameTimestamp is different
            return entities;
        });

        // Second game with same playerId but different gameTimestamp
        store.store(stats2, false);
        GameProcessedEvent event2 = new GameProcessedEvent(game2Timestamp, new HashMap<>());
        
        // This should NOT throw an exception
        assertDoesNotThrow(() -> {
            store.onGameEnded(event2);
        }, "Should be able to store same playerId with different gameTimestamp");
    }

    @Test
    void testStoreBatch_ReproducesActualDatabaseError_GameTimestampNotInInsert() {
        // This test reproduces the EXACT error from production:
        // The INSERT statement doesn't include gameTimestamp, which means the database column doesn't exist
        // "insert into PlayerStats (assists,clutchesWon,createdAt,damageDealt,deaths,headshotKills,kills,lastSeenNickname,lastUpdated,playerId,rank,roundsPlayed) values (?,?,?,?,?,?,?,?,?,?,?,?)"
        // Notice: gameTimestamp is MISSING from the INSERT!
        String playerId = "[U:1:1090227400]";
        Instant gameTimestamp = Instant.parse("2026-01-07T18:00:53.938698671Z");

        PlayerStats stats = createPlayerStats(playerId, "Player1", 10, 5, gameTimestamp);
        store.store(stats, false);

        // Simulate the EXACT error: DataIntegrityViolationException with unique constraint violation
        // The error message shows gameTimestamp is NOT in the INSERT statement
        org.springframework.dao.DataIntegrityViolationException exception = 
            new org.springframework.dao.DataIntegrityViolationException(
                "could not execute statement [Violation of UNIQUE KEY constraint 'UK_o06dpimursngkn3uisdoo9mig'. " +
                "Cannot insert duplicate key in object 'dbo.PlayerStats'. " +
                "The duplicate key value is ([U:1:1090227400]).]; SQL [null]; constraint [insert into PlayerStats " +
                "(assists,clutchesWon,createdAt,damageDealt,deaths,headshotKills,kills,lastSeenNickname,lastUpdated,playerId,rank,roundsPlayed) " +
                "values (?,?,?,?,?,?,?,?,?,?,?,?)]"
            );

        when(repository.saveAll(anyList())).thenThrow(exception);

        // This currently throws the exception - we need to fix it
        GameProcessedEvent event = new GameProcessedEvent(gameTimestamp, new HashMap<>());
        
        // The test should verify that gameTimestamp is set in the entity, even though it's not in the INSERT
        // This proves the database column doesn't exist yet
        ArgumentCaptor<List<PlayerStatsEntity>> captor = ArgumentCaptor.forClass(List.class);
        try {
            store.onGameEnded(event);
            fail("Should have thrown DataIntegrityViolationException");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Verify that gameTimestamp was set in the entity (even if DB column doesn't exist)
            verify(repository, times(1)).saveAll(captor.capture());
            List<PlayerStatsEntity> entities = captor.getValue();
            assertEquals(1, entities.size());
            assertNotNull(entities.get(0).getGameTimestamp(), 
                "gameTimestamp MUST be set in entity - if it's missing from INSERT, the DB column doesn't exist");
            assertEquals(gameTimestamp, entities.get(0).getGameTimestamp());
            
            // The root cause: database schema is out of sync
            // 1. gameTimestamp column doesn't exist in database
            // 2. Unique constraint on playerId still exists
            // Solution: Database schema needs to be updated
        }
    }

    private PlayerStats createPlayerStats(String playerId, String nickname, int kills, int deaths, Instant lastUpdated) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(playerId);
        stats.setLastSeenNickname(nickname);
        stats.setKills(kills);
        stats.setDeaths(deaths);
        stats.setAssists(0);
        stats.setHeadshotKills(0);
        stats.setRoundsPlayed(1);
        stats.setClutchesWon(0);
        stats.setDamageDealt(100.0);
        stats.setRank(1000);
        stats.setLastUpdated(lastUpdated);
        return stats;
    }
}
