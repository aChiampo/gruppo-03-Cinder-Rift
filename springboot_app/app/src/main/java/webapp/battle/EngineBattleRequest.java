package webapp.battle;

import java.util.List;
import tools.jackson.databind.JsonNode;

/** Internal engine snapshots contain server-owned data only, with one entry per physical copy. */
public record EngineBattleRequest(Challenger challenger, Deck deck) {
    public record Challenger(String userId, String username, String displayName) { }
    public record Deck(List<Entry> entries) {
        public Deck { entries = List.copyOf(entries); }
    }
    public record Entry(int cardId, int artworkId, String destination, String name, String faction,
            Integer energyCost, Integer powerPoints, Integer baseAtk, Integer initialDef, List<Effect> effects) {
        public Entry { effects = List.copyOf(effects); }
    }
    public record Effect(int effectId, String code, JsonNode parameters) { }
}
