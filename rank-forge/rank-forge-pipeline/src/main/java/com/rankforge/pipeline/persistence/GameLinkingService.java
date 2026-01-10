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

import com.rankforge.core.events.GameEventType;
import com.rankforge.core.events.GameOverEvent;
import com.rankforge.pipeline.persistence.entity.GameEventEntity;
import com.rankforge.pipeline.persistence.entity.RoundEndEventEntity;
import com.rankforge.pipeline.persistence.repository.AccoladeRepository;
import com.rankforge.pipeline.persistence.repository.GameEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for linking games to events, rounds, and accolades
 * Author bageshwar.pn
 * Date 2026
 */
@Service
public class GameLinkingService {
    private static final Logger logger = LoggerFactory.getLogger(GameLinkingService.class);
    
    private final GameEventRepository gameEventRepository;
    private final AccoladeRepository accoladeRepository;
    
    public GameLinkingService(GameEventRepository gameEventRepository, AccoladeRepository accoladeRepository) {
        this.gameEventRepository = gameEventRepository;
        this.accoladeRepository = accoladeRepository;
    }
    
    /**
     * Main method to link a game to all related events, rounds, and accolades
     */
    @Transactional(rollbackFor = Exception.class)
    public void linkGameToEvents(GameOverEvent gameOverEvent, Long gameId, Instant gameStartTime, Instant gameEndTime) {
        logger.info("Linking game {} to events (start: {}, end: {})", gameId, gameStartTime, gameEndTime);
        
        // 1. Link rounds to game
        int totalRounds = gameOverEvent.getTeam1Score() + gameOverEvent.getTeam2Score();
        List<Long> roundIds = linkRoundsToGame(gameId, gameOverEvent.getTimestamp(), totalRounds);
        
        // 2. Link all events to game
        linkEventsToGame(gameId, gameStartTime, gameEndTime);
        
        // 3. Link events to rounds
        if (!roundIds.isEmpty()) {
            linkEventsToRounds(roundIds, gameStartTime, gameEndTime);
        }
        
        // 4. Link accolades to game
        linkAccoladesToGame(gameId, gameOverEvent.getTimestamp());
        
        logger.info("Completed linking game {} to events", gameId);
    }
    
    /**
     * Link rounds to game by finding N most recent rounds before gameOverTimestamp
     * Returns list of round IDs that were linked
     */
    private List<Long> linkRoundsToGame(Long gameId, Instant gameOverTimestamp, int totalRounds) {
        logger.debug("Linking {} rounds to game {}", totalRounds, gameId);
        
        // Find N most recent ROUND_END events before gameOverTimestamp
        // Use a time window (e.g., 2 hours before game over) to limit search
        Instant searchStart = gameOverTimestamp.minusSeconds(7200); // 2 hours before
        
        List<GameEventEntity> roundEvents = gameEventRepository.findByGameEventTypeAndTimestampBetween(
                GameEventType.ROUND_END, searchStart, gameOverTimestamp);
        
        // Sort by timestamp descending and take the N most recent
        List<RoundEndEventEntity> rounds = roundEvents.stream()
                .filter(e -> e instanceof RoundEndEventEntity)
                .map(e -> (RoundEndEventEntity) e)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())) // Descending
                .limit(totalRounds)
                .collect(Collectors.toList());
        
        if (rounds.size() < totalRounds) {
            logger.warn("Found only {} rounds, expected {}", rounds.size(), totalRounds);
        }
        
        List<Long> roundIds = rounds.stream()
                .map(RoundEndEventEntity::getId)
                .collect(Collectors.toList());
        
        if (!roundIds.isEmpty()) {
            int updated = gameEventRepository.updateRoundsGameId(gameId, roundIds);
            logger.info("Linked {} rounds to game {}", updated, gameId);
        }
        
        return roundIds;
    }
    
    /**
     * Link all events between gameStartTime and gameEndTime to the game
     */
    private void linkEventsToGame(Long gameId, Instant gameStartTime, Instant gameEndTime) {
        logger.debug("Linking events to game {} (time range: {} to {})", gameId, gameStartTime, gameEndTime);
        
        List<GameEventEntity> events = gameEventRepository.findEventsBetweenTimestamps(gameStartTime, gameEndTime);
        
        // Filter out GAME_OVER events (they define the game, not belong to it)
        List<Long> eventIds = events.stream()
                .filter(e -> e.getGameEventType() != GameEventType.GAME_OVER)
                .map(GameEventEntity::getId)
                .collect(Collectors.toList());
        
        if (!eventIds.isEmpty()) {
            int updated = gameEventRepository.updateEventsGameId(gameId, eventIds);
            logger.info("Linked {} events to game {}", updated, gameId);
        } else {
            logger.warn("No events found to link to game {}", gameId);
        }
    }
    
    /**
     * Link events to rounds based on timestamp ranges
     * For each round, find events between previous ROUND_END and this ROUND_END
     */
    private void linkEventsToRounds(List<Long> roundIds, Instant gameStartTime, Instant gameEndTime) {
        logger.debug("Linking events to {} rounds", roundIds.size());
        
        // Get all round end events with their timestamps
        List<RoundEndEventEntity> rounds = roundIds.stream()
                .map(id -> gameEventRepository.findById(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(e -> e instanceof RoundEndEventEntity)
                .map(e -> (RoundEndEventEntity) e)
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp())) // Ascending by time
                .collect(Collectors.toList());
        
        if (rounds.isEmpty()) {
            logger.warn("No rounds found to link events to");
            return;
        }
        
        // Get all events in the game time range
        List<GameEventEntity> allEvents = gameEventRepository.findEventsBetweenTimestamps(gameStartTime, gameEndTime);
        
        // Filter out ROUND_END and GAME_OVER events (they don't belong to a round)
        List<GameEventEntity> eventsToLink = allEvents.stream()
                .filter(e -> e.getGameEventType() != GameEventType.ROUND_END 
                          && e.getGameEventType() != GameEventType.GAME_OVER)
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
        
        // For each round, find events between previous round end and this round end
        // Use array to hold mutable reference for lambda
        Instant[] previousRoundEnd = {gameStartTime};
        int totalLinked = 0;
        
        for (RoundEndEventEntity round : rounds) {
            Instant roundEndTime = round.getTimestamp();
            
            // Find events between previousRoundEnd[0] and roundEndTime
            List<Long> eventIds = eventsToLink.stream()
                    .filter(e -> {
                        Instant eventTime = e.getTimestamp();
                        return (eventTime.isAfter(previousRoundEnd[0]) || eventTime.equals(previousRoundEnd[0]))
                            && (eventTime.isBefore(roundEndTime) || eventTime.equals(roundEndTime));
                    })
                    .map(GameEventEntity::getId)
                    .collect(Collectors.toList());
            
            if (!eventIds.isEmpty()) {
                int updated = gameEventRepository.updateEventsRoundId(round.getId(), eventIds);
                totalLinked += updated;
                logger.debug("Linked {} events to round {}", updated, round.getId());
            }
            
            previousRoundEnd[0] = roundEndTime;
        }
        
        logger.info("Linked {} total events to rounds", totalLinked);
    }
    
    /**
     * Link accolades to game by matching gameTimestamp
     */
    private void linkAccoladesToGame(Long gameId, Instant gameOverTimestamp) {
        logger.debug("Linking accolades to game {} (timestamp: {})", gameId, gameOverTimestamp);
        
        // Use a small time window to match accolades (e.g., within 1 minute of game over)
        Instant windowStart = gameOverTimestamp.minusSeconds(60);
        Instant windowEnd = gameOverTimestamp.plusSeconds(60);
        
        // Try exact match first
        int updated = accoladeRepository.updateAccoladesGameId(gameId, gameOverTimestamp);
        
        if (updated == 0) {
            // If no exact match, try within time window
            logger.debug("No exact match for accolades, trying time window");
            List<com.rankforge.pipeline.persistence.entity.AccoladeEntity> accolades = 
                    accoladeRepository.findByGameTimestampBetween(windowStart, windowEnd);
            
            if (!accolades.isEmpty()) {
                // Update each accolade individually (fallback if bulk update doesn't work)
                for (com.rankforge.pipeline.persistence.entity.AccoladeEntity accolade : accolades) {
                    accolade.setGameId(gameId);
                }
                accoladeRepository.saveAll(accolades);
                updated = accolades.size();
            }
        }
        
        logger.info("Linked {} accolades to game {}", updated, gameId);
    }
}
