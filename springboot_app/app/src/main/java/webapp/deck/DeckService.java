package webapp.deck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coordinates ownership-locked deck operations and validation before writes. */
@Service
public class DeckService {
    private final DeckRepository decks;
    private final CardCatalogRepository catalog;
    private final DeckValidator validator;

    public DeckService(DeckRepository decks, CardCatalogRepository catalog, DeckValidator validator) {
        this.decks = decks;
        this.catalog = catalog;
        this.validator = validator;
    }

    @Transactional
    public DeckRecord createDeck(int authenticatedUserId) {
        lockUser(authenticatedUserId);
        return decks.createDeck(authenticatedUserId)
                .orElseThrow(() -> notFound("User not found"));
    }

    @Transactional
    public DeckView createDeck(int authenticatedUserId, List<DeckEntry> initialEntries) {
        lockUser(authenticatedUserId);
        DeckRecord deck = decks.createDeck(authenticatedUserId)
                .orElseThrow(() -> notFound("User not found"));
        List<DeckEntry> proposed = List.copyOf(initialEntries);
        DeckValidationResult validation = validate(authenticatedUserId, proposed, false);
        requireValid(validation);
        List<DeckEntry> savedEntries = persistEntries(authenticatedUserId, deck.id(), proposed);
        return new DeckView(deck, savedEntries, validate(authenticatedUserId, savedEntries, false));
    }

    @Transactional(readOnly = true)
    public List<DeckRecord> listDecks(int authenticatedUserId) {
        return decks.findOwnedDecks(authenticatedUserId);
    }

    @Transactional(readOnly = true)
    public List<DeckView> listDeckViews(int authenticatedUserId) {
        return decks.findOwnedDecks(authenticatedUserId).stream()
                .map(deck -> view(authenticatedUserId, deck))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckView getDeckView(int authenticatedUserId, int deckId) {
        DeckRecord deck = decks.findOwnedDeck(authenticatedUserId, deckId)
                .orElseThrow(() -> notFound("Deck not found"));
        return view(authenticatedUserId, deck);
    }

    @Transactional
    public DeckView updateDeck(int authenticatedUserId, int deckId, List<DeckEntry> entries,
            boolean challengeable) {
        lockOwnedDeck(authenticatedUserId, deckId);
        List<DeckEntry> proposed = List.copyOf(entries);
        DeckValidationResult validation = validate(authenticatedUserId, proposed, challengeable);
        requireValid(validation);
        if (!decks.setChallengeable(authenticatedUserId, deckId, challengeable)) {
            throw notFound("Deck not found");
        }
        decks.deleteEntries(authenticatedUserId, deckId);
        List<DeckEntry> savedEntries = persistEntries(authenticatedUserId, deckId, proposed);
        DeckRecord updated = decks.findOwnedDeck(authenticatedUserId, deckId)
                .orElseThrow(() -> notFound("Deck not found"));
        return new DeckView(updated, savedEntries, validate(authenticatedUserId, savedEntries, challengeable));
    }

    @Transactional(readOnly = true)
    public DeckRecord getDeck(int authenticatedUserId, int deckId) {
        return decks.findOwnedDeck(authenticatedUserId, deckId)
                .orElseThrow(() -> notFound("Deck not found"));
    }

    @Transactional(readOnly = true)
    public List<DeckEntry> getEntries(int authenticatedUserId, int deckId) {
        getDeck(authenticatedUserId, deckId);
        return decks.findEntries(authenticatedUserId, deckId);
    }

    @Transactional
    public void deleteDeck(int authenticatedUserId, int deckId) {
        lockOwnedDeck(authenticatedUserId, deckId);
        if (!decks.deleteDeck(authenticatedUserId, deckId)) {
            throw notFound("Deck not found");
        }
    }

    @Transactional
    public DeckValidationResult addCard(int authenticatedUserId, int deckId, DeckEntry newEntry) {
        DeckRecord deck = lockOwnedDeck(authenticatedUserId, deckId);
        List<DeckEntry> proposed = new ArrayList<>(decks.findEntries(authenticatedUserId, deckId));
        if (newEntry == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deck entry is required");
        }
        if (newEntry.id() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entry ID is assigned by the database");
        }
        proposed.add(newEntry);
        DeckValidationResult result = validate(authenticatedUserId, proposed, deck.challengeable());
        if (!result.valid()) {
            return result;
        }
        if (decks.addEntry(authenticatedUserId, deckId, newEntry).isEmpty()) {
            throw notFound("Deck not found");
        }
        return result;
    }

    @Transactional
    public DeckValidationResult moveCard(int authenticatedUserId, int deckId, int entryId, String destination) {
        DeckRecord deck = lockOwnedDeck(authenticatedUserId, deckId);
        List<DeckEntry> proposed = new ArrayList<>(decks.findEntries(authenticatedUserId, deckId));
        boolean found = false;
        for (int index = 0; index < proposed.size(); index++) {
            DeckEntry entry = proposed.get(index);
            if (entry.id() != null && entry.id() == entryId) {
                proposed.set(index, entry.withDestination(destination));
                found = true;
                break;
            }
        }
        if (!found) {
            throw notFound("Deck entry not found");
        }
        DeckValidationResult result = validate(authenticatedUserId, proposed, deck.challengeable());
        if (!result.valid()) {
            return result;
        }
        if (!decks.updateEntryDestination(authenticatedUserId, deckId, entryId, destination)) {
            throw notFound("Deck entry not found");
        }
        return result;
    }

    @Transactional
    public void removeCard(int authenticatedUserId, int deckId, int entryId) {
        DeckRecord deck = lockOwnedDeck(authenticatedUserId, deckId);
        if (deck.challengeable() && !decks.setChallengeable(authenticatedUserId, deckId, false)) {
            throw notFound("Deck not found");
        }
        if (!decks.deleteEntry(authenticatedUserId, deckId, entryId)) {
            throw notFound("Deck entry not found");
        }
    }

    @Transactional
    public DeckValidationResult setChallengeable(int authenticatedUserId, int deckId, boolean challengeable) {
        lockOwnedDeck(authenticatedUserId, deckId);
        List<DeckEntry> entries = decks.findEntries(authenticatedUserId, deckId);
        if (!challengeable) {
            if (!decks.setChallengeable(authenticatedUserId, deckId, false)) {
                throw notFound("Deck not found");
            }
            return validate(authenticatedUserId, entries, false);
        }
        DeckValidationResult result = validate(authenticatedUserId, entries, challengeable);
        if (!result.valid()) {
            return result;
        }
        if (!decks.setChallengeable(authenticatedUserId, deckId, challengeable)) {
            throw notFound("Deck not found");
        }
        return result;
    }

    @Transactional
    public DeckValidationResult validateDeck(int authenticatedUserId, int deckId) {
        lockOwnedDeck(authenticatedUserId, deckId);
        return validate(authenticatedUserId, decks.findEntries(authenticatedUserId, deckId), true);
    }

    private DeckValidationResult validate(int userId, List<DeckEntry> entries, boolean requireComplete) {
        Set<Integer> cardIds = new HashSet<>();
        Set<CardArtworkKey> artworks = new HashSet<>();
        for (DeckEntry entry : entries) {
            cardIds.add(entry.cardId());
            artworks.add(new CardArtworkKey(entry.cardId(), entry.cardArtId()));
        }
        DeckValidationContext context = new DeckValidationContext(userId, entries,
                catalog.findCards(cardIds), catalog.findArtworks(cardIds), catalog.findRoundConfigurations(),
                catalog.findOwnedCopies(userId, cardIds, artworks), requireComplete);
        return validator.validate(context);
    }

    private DeckView view(int userId, DeckRecord deck) {
        List<DeckEntry> entries = decks.findEntries(userId, deck.id());
        return new DeckView(deck, entries, validate(userId, entries, deck.challengeable()));
    }

    private List<DeckEntry> persistEntries(int userId, int deckId, List<DeckEntry> entries) {
        List<DeckEntry> saved = new ArrayList<>(entries.size());
        for (DeckEntry entry : entries) {
            if (entry == null || entry.id() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deck entries must not include IDs");
            }
            saved.add(decks.addEntry(userId, deckId, entry)
                    .orElseThrow(() -> notFound("Deck not found")));
        }
        return List.copyOf(saved);
    }

    private static void requireValid(DeckValidationResult validation) {
        if (!validation.valid()) {
            throw new DeckValidationException(validation);
        }
    }

    private DeckRecord lockOwnedDeck(int userId, int deckId) {
        lockUser(userId);
        return decks.lockOwnedDeck(userId, deckId)
                .orElseThrow(() -> notFound("Deck not found"));
    }

    private void lockUser(int userId) {
        if (!decks.lockActiveUser(userId)) {
            throw notFound("User not found");
        }
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
