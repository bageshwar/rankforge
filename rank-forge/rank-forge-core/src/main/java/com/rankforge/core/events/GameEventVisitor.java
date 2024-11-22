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

package com.rankforge.core.events;

import com.rankforge.core.models.PlayerStats;

/**
 * This interface is designed for visiting different event types
 * Author bageshwar.pn
 * Date 09/11/24
 */
public interface GameEventVisitor {
    void visit(AttackEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(AssistEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(BombEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(KillEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(RoundStartEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(RoundEndEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(GameOverEvent event, PlayerStats player1Stats, PlayerStats player2Stats);

    void visit(GameProcessedEvent event, PlayerStats player1Stats, PlayerStats player2Stats);
}
