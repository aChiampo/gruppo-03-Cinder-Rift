package webapp.deck;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
@Transactional
class DeckServiceDatabaseTests {
    @Autowired private JdbcClient jdbc;
    @Autowired private DeckService decks;

    private int ownerId;
    private int otherUserId;
    private int cardId;
    private int cardArtId;
    private int deckId;

    @BeforeEach
    void setUp() {
        ownerId = createUser();
        otherUserId = createUser();
        int creatureType = jdbc.sql("SELECT id FROM card_types WHERE code = 'CREATURE'")
                .query(Integer.class).single();
        cardId = jdbc.sql("""
                INSERT INTO cards (id_type, energy_cost, power, id_rarity)
                VALUES (:type, 1, 1, (SELECT id FROM rarities WHERE code = 'COMMON'))
                RETURNING id
                """)
                .param("type", creatureType)
                .query(Integer.class)
                .single();
        cardArtId = jdbc.sql("""
                INSERT INTO cards_arts (id_card, art_code)
                VALUES (:cardId, 'default')
                RETURNING id_art
                """)
                .param("cardId", cardId)
                .query(Integer.class)
                .single();
        jdbc.sql("""
                INSERT INTO collections (id_user, id_card, id_card_art, owned_replica, tradable_replica)
                VALUES (:userId, :cardId, :cardArtId, 1, 1)
                """)
                .param("userId", ownerId)
                .param("cardId", cardId)
                .param("cardArtId", cardArtId)
                .update();
        deckId = decks.createDeck(ownerId).id();
    }

    @Test
    void savesOwnedCopiesAsRowsAndRequiresFullValidityForChallengeability() {
        DeckValidationResult draft = decks.addCard(ownerId, deckId,
                new DeckEntry(cardId, cardArtId, "ROUND1"));

        assertThat(draft.valid()).isTrue();
        assertThat(draft.warnings()).isNotEmpty();
        assertThat(decks.getEntries(ownerId, deckId))
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.id()).isPositive();
                    assertThat(entry.cardId()).isEqualTo(cardId);
                    assertThat(entry.cardArtId()).isEqualTo(cardArtId);
                });
        int entryId = decks.getEntries(ownerId, deckId).getFirst().id();
        assertThat(decks.moveCard(ownerId, deckId, entryId, "ROUND2").valid()).isTrue();
        assertThat(decks.getEntries(ownerId, deckId).getFirst().destination()).isEqualTo("ROUND2");
        assertThat(decks.setChallengeable(ownerId, deckId, true).valid()).isFalse();
        assertThat(decks.getDeck(ownerId, deckId).challengeable()).isFalse();
        decks.removeCard(ownerId, deckId, entryId);
        assertThat(decks.getEntries(ownerId, deckId)).isEmpty();
    }

    @Test
    void validatesArtworkOwnershipDestinationAndConfiguredEnergyBeforeSaving() {
        DeckValidationResult missingArtwork = decks.addCard(ownerId, deckId,
                new DeckEntry(cardId, Integer.MAX_VALUE, "ROUND1"));
        assertThat(missingArtwork.errors()).anyMatch(v -> v.code().equals("CARD_ART_NOT_FOUND"));
        assertThat(decks.getEntries(ownerId, deckId)).isEmpty();

        DeckValidationResult incompatible = decks.addCard(ownerId, deckId,
                new DeckEntry(cardId, cardArtId, "EQUIPMENT"));
        assertThat(incompatible.errors()).anyMatch(v -> v.code().equals("DESTINATION_TYPE_MISMATCH"));

        jdbc.sql("""
                INSERT INTO collections (id_user, id_card, id_card_art, owned_replica, tradable_replica)
                VALUES (:userId, :cardId, :cardArtId, 2, 2)
                ON CONFLICT (id_user, id_card, id_card_art)
                DO UPDATE SET owned_replica = 2, tradable_replica = 2
                """)
                .param("userId", ownerId)
                .param("cardId", cardId)
                .param("cardArtId", cardArtId)
                .update();
        DeckValidationResult firstCopy = decks.addCard(ownerId, deckId,
                new DeckEntry(cardId, cardArtId, "ROUND1"));
        DeckValidationResult overEnergy = decks.addCard(ownerId, deckId,
                new DeckEntry(cardId, cardArtId, "ROUND1"));
        assertThat(firstCopy.valid()).isTrue();
        assertThat(overEnergy.errors()).anyMatch(v -> v.code().equals("DESTINATION_ENERGY_LIMIT_EXCEEDED"));
        assertThat(decks.getEntries(ownerId, deckId)).hasSize(1);
    }

    @Test
    void everyReadAndMutationIsScopedToTheAuthenticatedOwner() {
        assertThatThrownBy(() -> decks.getDeck(otherUserId, deckId))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> decks.addCard(otherUserId, deckId,
                new DeckEntry(cardId, cardArtId, "ROUND1")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> decks.deleteDeck(otherUserId, deckId))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(decks.getDeck(ownerId, deckId).id()).isEqualTo(deckId);
    }

    @Test
    void deletingADeckCascadesToItsCardCopies() {
        decks.addCard(ownerId, deckId, new DeckEntry(cardId, cardArtId, "ROUND1"));

        decks.deleteDeck(ownerId, deckId);

        assertThatThrownBy(() -> decks.getDeck(ownerId, deckId))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM decks_cards WHERE id_deck = :deckId")
                .param("deckId", deckId)
                .query(Long.class)
                .single()).isZero();
    }

    private int createUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return jdbc.sql("""
                INSERT INTO users (nickname, email, hash_password, id_role)
                VALUES (:nickname, :email, 'test-hash', (SELECT id FROM roles WHERE code = 'USER'))
                RETURNING id
                """)
                .param("nickname", "DeckPlayer-" + suffix)
                .param("email", suffix + "@example.invalid")
                .query(Integer.class)
                .single();
    }
}
