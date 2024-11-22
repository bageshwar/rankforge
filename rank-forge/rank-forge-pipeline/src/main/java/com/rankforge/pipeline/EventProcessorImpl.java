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
import com.rankforge.core.interfaces.RankingService;
import com.rankforge.core.models.Player;
import com.rankforge.core.models.PlayerStats;
import com.rankforge.core.stores.PlayerStatsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class processes the incoming GameEvent
 * Author bageshwar.pn
 * Date 26/10/24
 */
public class EventProcessorImpl implements EventProcessor, GameEventVisitor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessorImpl.class);
    private final PlayerStatsStore statsRepo;
    private final RankingService rankingService;

    public EventProcessorImpl(PlayerStatsStore statsRepo, RankingService rankingService) {
        this.statsRepo = statsRepo;
        this.rankingService = rankingService;
    }

    private static PlayerStats getDefaultPlayerStats(Player player) {
        PlayerStats playerStats = new PlayerStats();
        playerStats.setPlayerId(player.getSteamId());
        return playerStats;
    }

    @Override
    public void processEvent(GameEvent event) {
        //logger.info("Processing event {} at {}", event.getGameEventType(), event.getTimestamp().toString());
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
    public void visit(GameProcessedEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        logger.info("Processed game at {}", event.getTimestamp());
    }

    @Override
    public void visit(AttackEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        player1Stats.damageDealt += event.getDamage();
        statsRepo.store(player1Stats, false);
    }

    @Override
    public void visit(AssistEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        player1Stats.assists++;
        statsRepo.store(player1Stats, false);
    }

    @Override
    public void visit(BombEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {

    }

    @Override
    public void visit(KillEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        player1Stats.kills++;
        if (event.isHeadshot()) {
            player1Stats.headshotKills++;
        }

        player2Stats.deaths++;
        statsRepo.store(player1Stats, false);
        statsRepo.store(player2Stats, false);
    }

    @Override
    public void visit(RoundStartEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {

    }

    @Override
    public void visit(RoundEndEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
        event.getPlayers().remove("0"); //remove bots
        List<PlayerStats> list = event.getPlayers().stream()
                .map(playerSteamId -> statsRepo.getPlayerStats("[U:1:" + playerSteamId + "]"))
                .flatMap(playerStats1 -> playerStats1.stream()
                        .peek(p -> p.roundsPlayed++))
                .toList();

        rankingService.updateRankings(list);

        for (PlayerStats playerStats : list) {
            statsRepo.store(playerStats, true);
        }
    }

    @Override
    public void visit(GameOverEvent event, PlayerStats player1Stats, PlayerStats player2Stats) {
    }
}
