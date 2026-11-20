package com.benedetto.game_engine.battle.exception;

public class InactiveStageEventException extends RuntimeException {

    private final String eventCode;

    public InactiveStageEventException(String message) {
        super(message);
        this.eventCode = null;
    }

    public InactiveStageEventException(String eventCode, String message) {
        super(message);
        this.eventCode = eventCode;
    }

    public String getEventCode() {
        return eventCode;
    }
}
