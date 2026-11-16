package com.benedetto.game_engine.model;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Player {
    private String name;
    private int hp;
    private int roundsWon;
    private PlayerDeck deck;

    private List<Creature> currentRoundCreatures;
    private Creature activeCreature;
    private Equipment activeEquipment;

    public Player(String name, PlayerDeck deck) {
        this.name = name;
        this.deck = deck;
        this.roundsWon = 0;
    }

    public void prepareForRound(int roundNumber, Equipment drawnEquipment) {
        this.hp = 30; // Resettati a 30 a ogni inizio round
        this.activeEquipment = drawnEquipment;

        List<Creature> originalList = deck.getCreatureRounds().get(roundNumber);
        this.currentRoundCreatures = new ArrayList<>();
        if (originalList != null) {
            for (Creature c : originalList) {
                this.currentRoundCreatures.add(c.cloneForBattle());
            }
        }

        if (!currentRoundCreatures.isEmpty()) {
            this.activeCreature = currentRoundCreatures.remove(0);
            this.activeCreature.setEquipment(this.activeEquipment);
        } else {
            this.activeCreature = null;
        }
    }

    public boolean hasActiveCreature() {
        return activeCreature != null && !activeCreature.isDead();
    }

    public void takeHpDamage(int damage) {
        this.hp -= damage;
    }

    public void handleCreatureDeath(List<String> log) {
        if (!currentRoundCreatures.isEmpty()) {
            this.activeCreature = currentRoundCreatures.remove(0);
            this.activeCreature.setEquipment(this.activeEquipment);
            log.add(String.format("-> [%s] entra in campo per %s ed eredita l'equipaggiamento [%s]!",
                    activeCreature.getName(), name, activeEquipment.getName()));
        } else {
            this.activeCreature = null;
            log.add(String.format("-> %s non ha più creature nel mazzetto per questo round!", name));
        }
    }

    public void addRoundWin() {
        this.roundsWon++;
    }
}