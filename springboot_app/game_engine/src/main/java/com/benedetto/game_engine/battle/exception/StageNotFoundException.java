package com.benedetto.game_engine.battle.exception;

public class StageNotFoundException extends RuntimeException {

    private final String stageId;

    public StageNotFoundException(String message) {
        super(message);
        this.stageId = null;
    }

    public StageNotFoundException(String stageId, String message) {
        super(message);
        this.stageId = stageId;
    }

    public String getStageId() {
        return stageId;
    }
}
