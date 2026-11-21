package webapp.card;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Provides administrative card catalog writes without changing player-owned cards or decks. */
@Service
public class CardManagementService {
    private final CardManagementRepository management;
    private final CardCatalogRepository catalog;

    public CardManagementService(CardManagementRepository management, CardCatalogRepository catalog) {
        this.management = management;
        this.catalog = catalog;
    }

    @Transactional
    public CardCatalogItem create(CardWriteRequest request) {
        validate(request, null);
        int cardId = management.create(request);
        return findCreatedCard(cardId);
    }

    @Transactional
    public CardCatalogItem replace(int cardId, CardWriteRequest request) {
        management.lockActiveCard(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        ensureNotInUse(cardId);
        validate(request, cardId);
        management.update(cardId, request);
        return findCreatedCard(cardId);
    }

    @Transactional
    public void delete(int cardId) {
        management.lockActiveCard(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        ensureNotInUse(cardId);
        management.softDelete(cardId);
    }

    private void validate(CardWriteRequest request, Integer cardId) {
        requireReference(request.typeId(), management::typeExists, "typeId");
        requireReference(request.rarityId(), management::rarityExists, "rarityId");
        requireReference(request.factionId(), management::factionExists, "factionId");
        requireReference(request.expansionId(), management::expansionExists, "expansionId");
        Set<String> artCodes = new HashSet<>();
        Set<String> imageUrls = new HashSet<>();
        for (CardArtworkWriteRequest artwork : request.artworks()) {
            if (!artCodes.add(artwork.artCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Artwork codes must be unique");
            }
            String imageUrl = artwork.imageUrl();
            if (imageUrl != null && (!imageUrls.add(imageUrl)
                    || !management.artworkUrlAvailable(imageUrl, cardId))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Artwork image URLs must be unique");
            }
        }
    }

    private static void requireReference(Integer id, ReferenceCheck check, String field) {
        if (id != null && !check.exists(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or deleted " + field);
        }
    }

    private void ensureNotInUse(int cardId) {
        if (management.isInUse(cardId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Cards in a collection or deck cannot be changed or deleted");
        }
    }

    private CardCatalogItem findCreatedCard(int cardId) {
        return catalog.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("Card disappeared after catalog write"));
    }

    @FunctionalInterface
    private interface ReferenceCheck {
        boolean exists(int id);
    }
}
