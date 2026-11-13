package webapp.deck;

/** A validation error or a non-blocking draft warning. */
public record DeckViolation(String code, String scope, String message, Integer cardId,
        Integer cardArtId, String destination, Severity severity) {
    public enum Severity {
        ERROR,
        WARNING
    }

    static DeckViolation deck(String code, String message) {
        return new DeckViolation(code, "deck", message, null, null, null, Severity.ERROR);
    }

    static DeckViolation warning(String code, String scope, String message, String destination) {
        return new DeckViolation(code, scope, message, null, null, destination, Severity.WARNING);
    }

    static DeckViolation destination(String code, String message, String destination) {
        return new DeckViolation(code, "round", message, null, null, destination, Severity.ERROR);
    }

    static DeckViolation card(String code, String message, DeckEntry entry) {
        return new DeckViolation(code, "card", message, entry.cardId(), entry.cardArtId(),
                entry.destination(), Severity.ERROR);
    }
}
