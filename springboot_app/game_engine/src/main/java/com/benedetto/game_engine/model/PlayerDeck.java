package com.benedetto.game_engine.model;

import lombok.Data;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class PlayerDeck {
    private String playerId;
    private Map<Integer, List<Creature>> creatureRounds;
    private List<Equipment> equipmentDeck;
    private boolean deterministic;

    public void shuffleEquipments() {
        if (!deterministic && equipmentDeck != null) {
            Collections.shuffle(equipmentDeck);
        }
    }

    public void shuffleRoundCreatures(int roundNumber) {
        if (!deterministic && creatureRounds != null && creatureRounds.containsKey(roundNumber)) {
            Collections.shuffle(creatureRounds.get(roundNumber));
        }
    }
}
