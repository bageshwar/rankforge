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

package com.rankforge.pipeline;

import com.rankforge.core.events.AssistEvent;
import com.rankforge.core.events.AttackEvent;
import com.rankforge.core.events.BombEvent;
import com.rankforge.core.events.GameActionEvent;
import com.rankforge.core.events.GameEvent;
import com.rankforge.core.events.GameEventVisitor;
import com.rankforge.core.events.GameOverEvent;
import com.rankforge.core.events.GameProcessedEvent;
import com.rankforge.core.events.KillEvent;
import com.rankforge.core.events.RoundEndEvent;
import com.rankforge.core.events.RoundStartEvent;
import com.rankforge.core.interfaces.EventProcessor;
import com.rankforge.core.interfaces.GameEventListener;
import com.rankforge.core.interfaces.RankingService;
import com.rankforge.core.models.Player;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import com.rankforge.pipeline.persistence.EventProcessingContext;
import com.rankforge.pipeline.persistence.entity.GameEntity;
import com.rankforge.pipeline.persistence.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class processes the incoming GameEvent.
 * Uses direct entity references via EventProcessingContext for game/round linking.
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class EventProcessorImpl implements EventProcessor, GameEventVisitor, GameEventListener {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorImpl.class);
    private final PlayerStatsStore statsRepo;
    private final RankingService rankingService;
    private final List<GameEventListener> eventListeners;
    private final EventProcessingContext context;
    private final GameRepository gameRepository;

    public EventProcessorImpl(PlayerStatsStore statsRepo, RankingService rankingService,
                              EventProcessingContext context, GameRepository gameRepository) {
        this.statsRepo = statsRepo;
        this.rankingService = rankingService;
        this.context = context;
        this.gameRepository = gameRepository;
        this.eventListeners = new ArrayList<>();
    }

    private static PlayerStats getDefaultPlayerStats(Player player) {
        PlayerStats playerStats = new PlayerStats();
        playerStats.setPlayerId(player.getSteamId());
        playerStats.setLastSeenNickname(player.getName());
        return playerStats;
    }

    @Override
    public void processEvent(GameEvent event) {
        logger.debug("Processing event {} at {}", event.getGameEventType(), event.getTimestamp().toString());
        // delegate to eventType processor
        PlayerStats player1Stats = null;
        PlayerStats player2Stats = null;

        if (event instanceof GameActionEvent gameActionEvent) {
            player1Stats = statsRepo.getPlayerStats(gameActionEvent.getPlayer1().getSteamId())
                    .orElseGet(() -> getDefaultPlayerStats(gameActionEvent.getPlayer1()));
            player2Stats = statsRepo.getPlayerStats(gameActionEvent.getPlayer2().getSteamId()).
                    orElseGet(() -> getDefaultPlayerStats(gameActionEvent.getPlayer2()));
        }

        switch (event.getGameEventType()) {
            case KILL -> visit((KillEvent) event, player1Stats, player2Stats);
            case ASSIST -> visit((AssistEvent) event, player1Stats, player2Stats);
            case ATTACK -> visit((AttackEvent) event, player1Stats, player2Stats);
            case BOMB_EVENT -> visit((BombEvent) event, player1Stats, player2Stats);
            case ROUND_START -> visit((RoundStartEvent) event, player1Stats, player2Stats);
            case ROUND_END -> visit((RoundEndEvent) event, player1Stats, player2Stats);
            case GAME_OVER -> visit((GameOverEvent) event, player1Stats, player2Stats);
            case GAME_PROCESSED -> visit((GameProcessedEvent) event, player1Stats, player2Stats);

            default -> throw new IllegalStateException("Unexpected value: " + event.getGameEventType());
        }
    }

    @Override
    public void addGameEventListener(GameEventListener listener) {
        eventListeners.add(listener);
    }

    @Override
    public void visit(GameProcessedEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        logger.info("Processed game at {}", event.getTimestamp());
        
        // Notify listeners - JpaEventStore will batch persist all pending entities and accolades
        this.onGameEnded(event);
    }

    @Override
    public void visit(AttackEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        double oldDamage = player1Stats.getDamageDealt();
        player1Stats.setDamageDealt(player1Stats.getDamageDealt() + event.getDamage());
        logger.debug("Attack event: {} dealt {} damage to {} (total damage: {} -> {})", 
                event.getPlayer1().getName(), event.getDamage(), event.getPlayer2().getName(), 
                oldDamage, player1Stats.getDamageDealt());
        statsRepo.store(player1Stats, false);
    }

    @Override
    public void visit(AssistEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        int oldAssists = player1Stats.getAssists();
        player1Stats.setAssists(player1Stats.getAssists() + 1);
        logger.debug("Assist event: {} assisted in kill (assists: {} -> {})", 
                event.getPlayer1().getName(), oldAssists, player1Stats.getAssists());
        statsRepo.store(player1Stats, false);
    }

    @Override
    public void visit(BombEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        // Bomb events are stored but don't affect stats
    }

    @Override
    public void visit(KillEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        int oldKills = player1Stats.getKills();
        int oldHeadshotKills = player1Stats.getHeadshotKills();
        int oldDeaths = player2Stats.getDeaths();
        
        player1Stats.setKills(player1Stats.getKills() + 1);
        if (event.isHeadshot()) {
            player1Stats.setHeadshotKills(player1Stats.getHeadshotKills() + 1);
        }

        player2Stats.setDeaths(player2Stats.getDeaths() + 1);
        
        logger.debug("Kill event: {} killed {} with {} {} (killer: kills {} -> {}, hs {} -> {}, victim: deaths {} -> {})", 
                event.getPlayer1().getName(), event.getPlayer2().getName(), event.getWeapon(),
                event.isHeadshot() ? "(HEADSHOT)" : "", oldKills, player1Stats.getKills(),
                oldHeadshotKills, player1Stats.getHeadshotKills(), oldDeaths, player2Stats.getDeaths());
        
        if (!event.getPlayer1().isBot()) {
            statsRepo.store(player1Stats, false);
        }

        if(!event.getPlayer2().isBot()) {
            statsRepo.store(player2Stats, false);
        }
    }

    @Override
    public void visit(RoundStartEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        this.onRoundStarted(event);
    }

    @Override
    public void visit(RoundEndEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        event.getPlayers().remove("0"); //remove bots
        
        // Get player list from event, but if empty (last round of match), get from context
        java.util.Collection<String> playerSteamIds = event.getPlayers();
        if (playerSteamIds.isEmpty()) {
            // For the last round, the parser doesn't have the JSON player stats
            // Get players from the events processed in this round via context
            playerSteamIds = context.getPlayersInCurrentRound();
            logger.info("Round end: event had no players, extracted {} from context", playerSteamIds.size());
        }
        
        logger.debug("Round end: processing {} players for ranking updates", playerSteamIds.size());
        
        List<PlayerStats> list = playerSteamIds.stream()
                .map(playerSteamId -> statsRepo.getPlayerStats("[U:1:" + playerSteamId + "]"))
                .flatMap(playerStats1 -> playerStats1.stream()
                        .peek(p -> {
                            int oldRounds = p.getRoundsPlayed();
                            p.setRoundsPlayed(p.getRoundsPlayed() + 1);
                            logger.debug("Player {} rounds played: {} -> {}", p.getLastSeenNickname(), oldRounds, p.getRoundsPlayed());
                        }))
                .toList();

        logger.debug("Updating rankings for {} players", list.size());
        rankingService.updateRankings(list);

        for (PlayerStats playerStats : list) {
            logger.debug("Archiving stats for player: {} (rank: {})", playerStats.getLastSeenNickname(), playerStats.getRank());
            statsRepo.store(playerStats, true);
        }

        this.onRoundEnded(event);
    }

    @Override
    public void visit(GameOverEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        logger.info("Processing GAME_OVER event at {} on map {}", event.getTimestamp(), event.getMap());
        
        // Check for duplicate game (same timestamp and map)
        Optional<GameEntity> existingGame = gameRepository.findDuplicate(event.getTimestamp(), event.getMap());
        if (existingGame.isPresent()) {
            GameEntity duplicate = existingGame.get();
            logger.warn("Duplicate game detected - skipping ingestion. Existing game ID: {}, timestamp: {}, map: {}", 
                    duplicate.getId(), event.getTimestamp(), event.getMap());
            // Skip processing - don't create game entity, don't process events, don't update stats
            // Clear any pending accolades that were queued before we detected the duplicate
            context.clear();
            return;
        }
        
        // Create transient GameEntity from GameOverEvent
        GameEntity gameEntity = new GameEntity();
        gameEntity.setGameOverTimestamp(event.getTimestamp());
        gameEntity.setMap(event.getMap());
        gameEntity.setMode(event.getMode());
        gameEntity.setTeam1Score(event.getTeam1Score());
        gameEntity.setTeam2Score(event.getTeam2Score());
        gameEntity.setDuration(event.getDuration());
        gameEntity.setEndTime(event.getTimestamp());
        
        // Calculate startTime from duration (approximate)
        if (event.getDuration() != null) {
            gameEntity.setStartTime(event.getTimestamp().minusSeconds(event.getDuration()));
        } else {
            // Fallback: estimate 30 minutes
            gameEntity.setStartTime(event.getTimestamp().minusSeconds(1800));
        }
        
        // Set the current game in context (this is transient, not yet persisted)
        // JPA will persist it when we call saveAll on pending entities due to cascade
        context.setCurrentGame(gameEntity);
        
        // Link any pending accolades to this game
        context.linkAccoladesToGame();
        
        logger.info("Created transient GameEntity for game ending at {} on map {}", 
                event.getTimestamp(), event.getMap());
        
        this.onGameStarted(event);
    }

    @Override
    public void onGameStarted(GameOverEvent event) {
        for (GameEventListener listener : this.eventListeners) {
            listener.onGameStarted(event);
        }
    }

    @Override
    public void onGameEnded(GameProcessedEvent event) {
        for (GameEventListener listener : this.eventListeners) {
            listener.onGameEnded(event);
        }
    }

    @Override
    public void onRoundStarted(RoundStartEvent event) {
        for (GameEventListener listener : this.eventListeners) {
            listener.onRoundStarted(event);
        }
    }

    @Override
    public void onRoundEnded(RoundEndEvent event) {
        for (GameEventListener listener : this.eventListeners) {
            listener.onRoundEnded(event);
        }
    }
}
