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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rankforge.core.models.Player;

import java.time.Instant;
import java.util.Map;

/**
 * This class represents an assist event in the game
 * Author bageshwar.pn
 * Date 26/10/24
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class AssistEvent extends GameActionEvent {

    private AssistType assistType;

    public AssistEvent(Instant timestamp, Map<String, String> additionalData, Player player1, Player player2, String weapon, AssistType assistType) {
        super(timestamp, GameEventType.ASSIST, additionalData, player1, player2, weapon);
        this.assistType = assistType;
    }

    public AssistType getAssistType() {
        return assistType;
    }

    public void setAssistType(AssistType assistType) {
        this.assistType = assistType;
    }

    public enum AssistType {
        Regular, Flash
    }
}
