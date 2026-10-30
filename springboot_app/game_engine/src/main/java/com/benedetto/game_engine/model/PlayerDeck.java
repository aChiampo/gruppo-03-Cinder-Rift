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

    public void shuffleEquipments() {
        if (equipmentDeck != null) Collections.shuffle(equipmentDeck);
    }

    public void shuffleRoundCreatures(int roundNumber) {
        if (creatureRounds != null && creatureRounds.containsKey(roundNumber)) {
            Collections.shuffle(creatureRounds.get(roundNumber));
        }
    }
}