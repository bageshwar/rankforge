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

package com.rankforge.core.internal;

import com.rankforge.core.events.GameEvent;

/**
 * Model class returned by the log parser.
 * Author bageshwar.pn
 * Date 22/11/24
 */
public
class ParseLineResponse {
    GameEvent gameEvent;
    int nextIndex;

    public ParseLineResponse(GameEvent gameEvent, int nextIndex) {
        this.gameEvent = gameEvent;
        this.nextIndex = nextIndex;
    }

    public GameEvent getGameEvent() {
        return gameEvent;
    }

    public void setGameEvent(GameEvent gameEvent) {
        this.gameEvent = gameEvent;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }
}
