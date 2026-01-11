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

package com.rankforge.server.service;

import com.rankforge.pipeline.persistence.entity.AccoladeEntity;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.PlayerStatsEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import com.rankforge.pipeline.persistence.repository.PlayerStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Admin service for game management operations including deletion.
 * Handles cascade deletion of related entities.
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class AdminGameService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminGameService.class);
    
    private final GameRepository gameRepository;
    private final GameEventRepository gameEventRepository;
    private final AccoladeRepository accoladeRepository;
    private final PlayerStatsRepository playerStatsRepository;
    
    @Autowired
    public AdminGameService(GameRepository gameRepository,
                           GameEventRepository gameEventRepository,
                           AccoladeRepository accoladeRepository,
                           PlayerStatsRepository playerStatsRepository) {
        this.gameRepository = gameRepository;
        this.gameEventRepository = gameEventRepository;
        this.accoladeRepository = accoladeRepository;
        this.playerStatsRepository = playerStatsRepository;
    }
    
    /**
     * Delete a game and all related entities (cascade deletion).
     * 
     * Deletes in order:
     * 1. All game events (rounds, kills, assists, etc.) associated with the game
     * 2. All accolades associated with the game
     * 3. All player stats entries that match the game's timestamp
     * 4. The game entity itself
     * 
     * Note: This is a hard delete. Aggregate stats (leaderboards, user totals, clan totals)
     * should be recomputed in the background after deletion.
     * 
     * @param gameId The ID of the game to delete
     * @return true if the game was found and deleted, false if not found
     */
    @Transactional
    public boolean deleteGame(Long gameId) {
        logger.info("ADMIN_DELETE: Starting deletion of game ID: {}", gameId);
        
        Optional<GameEntity> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            logger.warn("ADMIN_DELETE: Game not found with ID: {}", gameId);
            return false;
        }
        
        GameEntity game = gameOpt.get();
        Instant gameTimestamp = game.getGameOverTimestamp();
        String map = game.getMap();
        
        logger.info("ADMIN_DELETE: Game found - ID: {}, timestamp: {}, map: {}", 
                gameId, gameTimestamp, map);
        
        // 1. Delete all game events associated with this game (including GAME_OVER)
        List<GameEventEntity> gameEvents = gameEventRepository.findAllByGameId(gameId);
        int eventCount = gameEvents.size();
        if (eventCount > 0) {
            gameEventRepository.deleteAll(gameEvents);
            logger.info("ADMIN_DELETE: Deleted {} game events (including GAME_OVER) for game ID: {}", 
                    eventCount, gameId);
        }
        
        // 2. Delete all accolades associated with this game
        List<AccoladeEntity> accolades = accoladeRepository.findByGameId(gameId);
        int accoladeCount = accolades.size();
        if (accoladeCount > 0) {
            accoladeRepository.deleteAll(accolades);
            logger.info("ADMIN_DELETE: Deleted {} accolades for game ID: {}", accoladeCount, gameId);
        }
        
        // 3. Delete player stats entries that match this game's timestamp
        // PlayerStats are stored with gameTimestamp matching the game's gameOverTimestamp
        List<PlayerStatsEntity> playerStats = playerStatsRepository.findByGameTimestamp(gameTimestamp);
        int statsCount = playerStats.size();
        if (statsCount > 0) {
            playerStatsRepository.deleteAll(playerStats);
            logger.info("ADMIN_DELETE: Deleted {} player stats entries for game ID: {}", statsCount, gameId);
        }
        
        // 4. Delete the game entity itself
        gameRepository.delete(game);
        logger.info("ADMIN_DELETE: Deleted game entity ID: {}", gameId);
        
        logger.info("ADMIN_DELETE: Successfully deleted game ID: {} (events: {}, accolades: {}, stats: {})", 
                gameId, eventCount, accoladeCount, statsCount);
        
        return true;
    }
    
    /**
     * Check if a game exists by ID.
     * 
     * @param gameId The game ID to check
     * @return true if the game exists, false otherwise
     */
    public boolean gameExists(Long gameId) {
        return gameRepository.existsById(gameId);
    }
}
