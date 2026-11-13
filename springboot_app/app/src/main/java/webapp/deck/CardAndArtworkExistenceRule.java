package webapp.deck;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/** Requires each entry's logical card and selected artwork to exist and be active. */
@Component
public class CardAndArtworkExistenceRule implements DeckValidationRule {
    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        List<DeckViolation> violations = new ArrayList<>();
        for (DeckEntry entry : context.entries()) {
            CardDefinition card = context.cards().get(entry.cardId());
            if (card == null || card.deleted()) {
                violations.add(DeckViolation.card("CARD_NOT_FOUND",
                        "Card does not exist or is deleted", entry));
            } else if (!context.artworks().contains(new CardArtworkKey(entry.cardId(), entry.cardArtId()))) {
                violations.add(DeckViolation.card("CARD_ART_NOT_FOUND",
                        "Selected artwork does not belong to an active card", entry));
            }
        }
        return violations;
    }
}
