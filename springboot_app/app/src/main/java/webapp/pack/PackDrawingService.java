package webapp.pack;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import webapp.card.CardArtwork;
import webapp.pack.OpenPacksResponse.PulledCardView;
import static webapp.pack.PackConfigurationValidationService.rank;
import static webapp.pack.PackConfigurationValidationService.weight;

/** Draws one pack without persistence or transaction boundaries. */
@Service
public class PackDrawingService {
    private final PackDrawSource random;

    public PackDrawingService(PackDrawSource random) { this.random = random; }

    DrawResult openOnePack(PackOpeningConfiguration config, Integer counter) {
        var cards = new ArrayList<PulledCardView>();
        for (int i = 0; i < config.slots().size(); i++) {
            var rates = config.slots().get(i).pullRates();
            int roll = random.nextInt(1_000_000);
            int sum = 0;
            Integer rarity = null;
            for (var rate : rates) {
                sum += weight(rate.probability());
                if (roll < sum) { rarity = rate.rarityId(); break; }
            }
            if (rarity == null) throw PackOpeningFailure.invalidConfiguration("Invalid slot probability distribution.");
            cards.add(drawCard(config, rarity, i + 1));
        }
        if (!config.mercyEnabled()) return new DrawResult(List.copyOf(cards), null);
        int current = counter == null ? config.threshold() : counter;
        if (current < 0 || current > config.threshold()) {
            throw PackOpeningFailure.invalidConfiguration("The mercy counter is outside its configured threshold.");
        }
        boolean qualifies = cards.stream().anyMatch(c -> rank(c.rarity().code()) >= rank(config.mercyRarity().code()));
        int remaining = Math.max(current - 1, 0);
        if (!qualifies && remaining == 0) {
            int last = cards.size();
            cards.set(last - 1, drawCard(config, config.mercyRarity().id(), last));
            qualifies = true;
        }
        return new DrawResult(List.copyOf(cards), qualifies ? config.threshold() : remaining);
    }

    private PulledCardView drawCard(PackOpeningConfiguration config, int rarity, int slot) {
        var pool = config.cardsByRarity().get(rarity);
        if (pool == null || pool.isEmpty()) throw PackOpeningFailure.invalidConfiguration("No eligible cards for the drawn rarity.");
        var card = pool.get(random.nextInt(pool.size()));
        var rates = config.artwork();
        int roll = random.nextInt(1_000_000);
        int sum = 0;
        String code = null;
        var weights = List.of(rates.standard(), rates.extendedArt(), rates.fullArt(), rates.alternateArt());
        var codes = List.of("STANDARD", "EXTENDED_ART", "FULL_ART", "ALTERNATE_ART");
        for (int i = 0; i < codes.size(); i++) {
            sum += weight(weights.get(i));
            if (roll < sum) { code = codes.get(i); break; }
        }
        if (code == null) throw PackOpeningFailure.invalidConfiguration("Invalid artwork probability distribution.");
        final String selected = code;
        CardArtwork art = card.artworks().stream().filter(a -> a.artCode().equals(selected)).findFirst()
                .orElseGet(() -> card.artworks().stream().filter(a -> a.artCode().equals("STANDARD")).findFirst()
                        .orElseThrow(() -> PackOpeningFailure.invalidConfiguration("STANDARD artwork is unavailable.")));
        return new PulledCardView(slot, card.id(), art.cardArtId(), art.artCode(), card.name(), card.rarity(), art.imageUrl());
    }

    record DrawResult(List<PulledCardView> cards, Integer nextMercy) { }
}
