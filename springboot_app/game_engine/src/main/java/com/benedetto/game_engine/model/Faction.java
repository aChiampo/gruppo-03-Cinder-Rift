package com.benedetto.game_engine.model;

public enum Faction {
    CYBORG,
    MOSTRI_RADIOATTIVI,
    SAPIENS,
    CULTISTI,
    ALIENI;

    public boolean isStrongAgainst(Faction target) {
        if (target == null) return false;
        return switch (this) {
            case CYBORG -> target == MOSTRI_RADIOATTIVI || target == SAPIENS;
            case MOSTRI_RADIOATTIVI -> target == SAPIENS || target == CULTISTI;
            case SAPIENS -> target == CULTISTI || target == ALIENI;
            case CULTISTI -> target == CYBORG || target == ALIENI;
            case ALIENI -> target == CYBORG || target == MOSTRI_RADIOATTIVI;
        };
    }
}
