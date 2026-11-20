package webapp.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import webapp.auth.UserProfile;
import webapp.deck.*;

/** Validates and materializes the same committed database view, before any remote computation. */
@Service
public class BattleSnapshotService {
    private final PlatformTransactionManager transactions;
    private final DeckRepository decks;
    private final CardCatalogRepository catalog;
    private final DeckValidator validator;
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public BattleSnapshotService(PlatformTransactionManager transactions, DeckRepository decks,
            CardCatalogRepository catalog, DeckValidator validator, JdbcClient jdbc, JsonMapper json) {
        this.transactions = transactions;
        this.decks = decks;
        this.catalog = catalog;
        this.validator = validator;
        this.jdbc = jdbc;
        this.json = json;
    }

    public EngineBattleRequest prepare(UserProfile user, int deckId, BattleBudget budget) {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        tx.setTimeout((int) Math.max(1, budget.remaining().toSeconds()));
        return tx.execute(status -> {
            // Server-side timeouts also bound waits on the user/deck locks.
            jdbc.sql("SELECT set_config('statement_timeout', :milliseconds, true)")
                    .param("milliseconds", Long.toString(Math.max(1, budget.remaining().toMillis())))
                    .query(String.class).single();
            if (!decks.lockActiveUser(user.id()))
                throw new BattleFailure(HttpStatus.UNAUTHORIZED, "Unauthorized", "The account is no longer active.");
            decks.lockOwnedDeck(user.id(), deckId)
                    .orElseThrow(() -> new BattleFailure(HttpStatus.NOT_FOUND, "Not Found", "Deck not found."));
            List<DeckEntry> entries = decks.findEntries(user.id(), deckId);
            Set<Integer> ids = entries.stream().map(DeckEntry::cardId).collect(Collectors.toSet());
            Set<CardArtworkKey> arts = entries.stream()
                    .map(entry -> new CardArtworkKey(entry.cardId(), entry.cardArtId())).collect(Collectors.toSet());
            DeckValidationResult validation = validator.validate(new DeckValidationContext(user.id(), entries,
                    catalog.findCards(ids), catalog.findArtworks(ids), catalog.findRoundConfigurations(),
                    catalog.findOwnedCopies(user.id(), ids, arts), true));
            if (!validation.valid()) throw new BattleFailure(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Deck",
                    "The selected deck does not meet the battle requirements.",
                    Map.of("violations", validation.errors().stream().map(DeckViolation::message).toList()));
            Map<Integer, List<EngineBattleRequest.Effect>> effects = loadEffects(ids);
            Map<Integer, CardStats> stats = new HashMap<>();
            jdbc.sql("""
                    SELECT c.id, c.name, f.code AS faction, t.code AS type, c.energy_cost, c.power,
                           c.value_atk, c.value_def
                    FROM cards c
                    JOIN card_types t ON t.id = c.id_type
                    LEFT JOIN factions f ON f.id = c.id_faction
                    WHERE c.id IN (:ids)
                    """).param("ids", ids).query((row, index) -> new CardStats(row.getInt("id"),
                            row.getString("name"), row.getString("faction"), row.getString("type"),
                            (Integer) row.getObject("energy_cost"), (Integer) row.getObject("power"),
                            (Integer) row.getObject("value_atk"), (Integer) row.getObject("value_def")))
                    .list().forEach(card -> stats.put(card.id(), card));
            List<EngineBattleRequest.Entry> snapshots = new ArrayList<>();
            for (DeckEntry entry : entries) {
                CardStats card = stats.get(entry.cardId());
                if (card == null || card.name() == null || card.name().isBlank()) throw BattleFailure.integration();
                boolean creature = "CREATURE".equals(card.type());
                if (creature && (card.atk() == null || card.def() == null || card.atk() < 0 || card.def() < 1))
                    throw BattleFailure.integration();
                snapshots.add(new EngineBattleRequest.Entry(entry.cardId(), entry.cardArtId(), entry.destination(),
                        card.name(), creature ? faction(card.faction()) : null,
                        card.energy(), card.power(), creature ? card.atk() : null, creature ? card.def() : null,
                        effects.getOrDefault(entry.cardId(), List.of())));
            }
            budget.remaining();
            var challenger = new EngineBattleRequest.Challenger(Integer.toString(user.id()), user.nickname(), user.nickname());
            return new EngineBattleRequest(challenger, new EngineBattleRequest.Deck(snapshots));
        });
    }

    private Map<Integer, List<EngineBattleRequest.Effect>> loadEffects(Set<Integer> ids) {
        Map<Integer, List<EngineBattleRequest.Effect>> effects = new HashMap<>();
        jdbc.sql("""
                SELECT ce.card_id, e.id, e.code, ce.parameters::text AS parameters
                FROM card_effects ce JOIN effects e ON e.id = ce.effect_id
                WHERE ce.card_id IN (:ids)
                ORDER BY ce.card_id, ce.execution_order
                """).param("ids", ids).query((row, index) -> {
                    var effect = new EngineBattleRequest.Effect(row.getInt("id"), row.getString("code"),
                            json.readTree(row.getString("parameters")));
                    effects.computeIfAbsent(row.getInt("card_id"), ignored -> new ArrayList<>()).add(effect);
                    return effect;
                }).list();
        return effects;
    }

    private static String faction(String code) {
        return switch (code == null ? "" : code) {
            case "CYB", "CYBORG" -> "CYBORG";
            case "SAP", "SAPIENS" -> "SAPIENS";
            case "RAD", "RADIOACTIVE", "MOSTRI_RADIOATTIVI" -> "MOSTRI_RADIOATTIVI";
            case "CLT", "CULTISTS", "CULTISTI" -> "CULTISTI";
            case "ALN", "ALIENS", "ALIENI" -> "ALIENI";
            default -> throw BattleFailure.integration();
        };
    }

    private record CardStats(int id, String name, String faction, String type,
            Integer energy, Integer power, Integer atk, Integer def) { }
}
