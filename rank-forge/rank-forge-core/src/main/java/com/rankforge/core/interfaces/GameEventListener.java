package com.rankforge.core.interfaces;

import com.rankforge.core.events.*;

public interface GameEventListener {

    /**
     * Fired when the Game is started, ironically the event name is GameOverEvent,
     * because we start the parsing only when we encounter the game finish boundary
     * @param event
     */
    void onGameStarted(GameOverEvent event);

    /**
     * Fired when the Game processing is complete
     * @param event
     */
    void onGameEnded(GameProcessedEvent event);
    void onRoundStarted(RoundStartEvent event);
    void onRoundEnded(RoundEndEvent event);
}
