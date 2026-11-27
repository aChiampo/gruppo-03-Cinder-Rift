package webapp.pack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import webapp.pack.OpenPacksResponse.MercyView;
import webapp.pack.OpenPacksResponse.OpenedPackView;

/** Commits the entire batch before its result is returned to the controller. */
@Service
public class PackOpeningService {
    private final PackOpeningRepository inventory;
    private final PackRepository packs;
    private final PackConfigurationValidationService validation;
    private final PackDrawingService drawing;

    public PackOpeningService(PackOpeningRepository inventory, PackRepository packs,
            PackConfigurationValidationService validation, PackDrawingService drawing) {
        this.inventory = inventory;
        this.packs = packs;
        this.validation = validation;
        this.drawing = drawing;
    }

    @Transactional(readOnly = true)
    public InventoryView inventory(int userId) { return inventory.inventory(userId); }

    @Transactional(readOnly = true)
    public List<OwnedPackView> ownedPacks(int userId) {
        inventory.inventory(userId);
        return packs.findOwned(userId);
    }

    @Transactional
    public OpenPacksResponse open(int userId, OpenPacksRequest request) {
        // The NOWAIT ownership attempt must precede all potentially waiting locks.
        var owned = inventory.lockOwned(userId, request.packId()).orElseGet(() -> {
            inventory.inventory(userId);
            if (!inventory.packExists(request.packId())) throw notFound();
            throw insufficient(request, 0);
        });
        if (!inventory.lockPack(request.packId())) throw notFound();
        inventory.lockActiveUser(userId);
        if (owned.quantity() < request.quantity()) throw insufficient(request, owned.quantity());
        var pack = packs.findById(request.packId()).orElseThrow(PackOpeningService::notFound);
        var config = validation.prepare(pack.definition());
        Integer counter = config.mercyEnabled()
                ? inventory.mercyCounter(userId, request.packId()).orElse(config.threshold()) : null;
        var results = new ArrayList<OpenedPackView>();
        var credits = new TreeMap<Variant, Integer>(Comparator.comparingInt(Variant::cardId).thenComparingInt(Variant::artId));
        for (int sequence = 1; sequence <= request.quantity(); sequence++) {
            var result = drawing.openOnePack(config, counter);
            results.add(new OpenedPackView(sequence, result.cards()));
            counter = result.nextMercy();
            result.cards().forEach(card -> credits.merge(new Variant(card.cardId(), card.cardArtId()), 1, Math::addExact));
        }
        // Stable order also protects concurrent openings of different pack types sharing cards.
        credits.forEach((variant, quantity) -> inventory.grant(userId, variant.cardId(), variant.artId(), quantity));
        int remaining = owned.quantity() - request.quantity();
        inventory.consume(owned.inventoryId(), request.packId(), remaining);
        if (config.mercyEnabled()) inventory.saveMercy(userId, request.packId(), counter);
        return new OpenPacksResponse(request.packId(), request.quantity(), remaining, List.copyOf(results),
                config.mercyEnabled() ? new MercyView(config.threshold(), counter, config.mercyRarity()) : null);
    }

    private static PackOpeningFailure insufficient(OpenPacksRequest request, int available) {
        return new PackOpeningFailure(409, "INSUFFICIENT_PACK_QUANTITY", "Not enough owned packs.",
                Map.of("packId", request.packId(), "requestedQuantity", request.quantity(), "availableQuantity", available));
    }

    private static PackOpeningFailure notFound() { return new PackOpeningFailure(404, "PACK_NOT_FOUND", "Pack not found."); }

    private record Variant(int cardId, int artId) { }
}
