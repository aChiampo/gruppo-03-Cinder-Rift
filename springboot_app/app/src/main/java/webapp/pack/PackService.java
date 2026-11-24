package webapp.pack;

import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import webapp.card.CardCatalogRepository;
import webapp.card.CardPage;

/** Manages definitions only; no package purchase, grant, or opening is performed. */
@Service
public class PackService {
    private final PackRepository packs;
    private final CardCatalogRepository cards;
    private final JsonMapper json;

    public PackService(PackRepository packs, CardCatalogRepository cards, JsonMapper json) {
        this.packs = packs;
        this.cards = cards;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public CardPage<PackView> list(int page, int size) {
        long total = packs.count();
        return new CardPage<>(packs.findPage(page, size), page, size, total,
                total == 0 ? 0 : Math.toIntExact((total + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public PackView get(int id) {
        return packs.findById(id).orElseThrow(() -> failure(HttpStatus.NOT_FOUND, "Pack not found"));
    }

    @Transactional
    public PackView create(PackWriteRequest request) {
        validate(request);
        return get(packs.create(request));
    }

    @Transactional
    public PackView replace(int id, PackWriteRequest request) {
        lockUnused(id);
        validate(request);
        packs.update(id, request);
        return get(id);
    }

    @Transactional
    public void delete(int id) {
        lockUnused(id);
        packs.delete(id);
    }

    private void lockUnused(int id) {
        if (!packs.lock(id)) throw failure(HttpStatus.NOT_FOUND, "Pack not found");
        // Preserve promised contents and historical mercy until a reconciliation policy exists.
        if (packs.isInUse(id)) throw failure(HttpStatus.CONFLICT,
                "Packs with ownership or mercy progress cannot be changed or deleted");
    }

    private void validate(PackWriteRequest pack) {
        if (!packs.artworkPullRatesExist(pack.artworkPullRatesId())) {
            throw failure(HttpStatus.BAD_REQUEST, "Unknown id_artwork_pull_rates");
        }
        validateCodes("factions", pack.allowedFactions());
        validateCodes("card_types", pack.allowedTypes());
        validateCodes("expansions", pack.allowedExpansions());
        validateCodes("rarities", pack.allowedRarities());
        requireCode("rarities", pack.description().grantedRarity());
        requireCode("rarities", pack.description().bestPullRarity());
        if (pack.description().grantedNumber() > pack.description().cardNumber()) {
            throw failure(HttpStatus.BAD_REQUEST, "granted_number cannot exceed card_number");
        }
        if (!pack.slotPulls().isObject() && !pack.slotPulls().isArray()) {
            throw failure(HttpStatus.BAD_REQUEST, "slot_pulls must be a JSON object or array");
        }
        var preview = pack.description().examplePull();
        if (preview.size() != pack.description().cardNumber()) {
            throw failure(HttpStatus.BAD_REQUEST, "example_pull must contain card1 through cardN for card_number");
        }
        for (int index = 1; index <= preview.size(); index++) {
            JsonNode card = preview.get("card" + index);
            if (card == null || !card.isObject() || !card.path("id").isIntegralNumber()
                    || !card.path("id").canConvertToInt() || card.path("id").asInt() <= 0) {
                throw failure(HttpStatus.BAD_REQUEST, "Each preview entry must be a complete F04 catalog card");
            }
            var current = cards.findById(card.path("id").asInt())
                    .orElseThrow(() -> failure(HttpStatus.BAD_REQUEST, "Unknown or deleted preview card"));
            if (!json.valueToTree(current).equals(card)) {
                throw failure(HttpStatus.BAD_REQUEST, "Preview cards must match the complete current F04 representation");
            }
        }
    }

    private void validateCodes(String table, List<String> codes) {
        if (new HashSet<>(codes).size() != codes.size()) {
            throw failure(HttpStatus.BAD_REQUEST, "Catalog filters cannot contain duplicate codes");
        }
        for (String code : codes) requireCode(table, code);
    }

    private void requireCode(String table, String code) {
        if (!packs.referenceExists(table, code)) {
            throw failure(HttpStatus.BAD_REQUEST, "Unknown or deleted " + table + " code");
        }
    }

    private static ResponseStatusException failure(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
